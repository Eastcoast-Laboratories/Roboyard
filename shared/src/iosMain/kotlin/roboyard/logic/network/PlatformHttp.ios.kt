package roboyard.logic.network

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.HTTPBody
import platform.Foundation.HTTPMethod
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.create
import platform.Foundation.dataTaskWithRequest
import platform.Foundation.dataUsingEncoding
import platform.Foundation.setValue
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_semaphore_create
import platform.darwin.dispatch_semaphore_signal
import platform.darwin.dispatch_semaphore_wait
import platform.darwin.dispatch_time

/**
 * iOS blocking HTTP transport via NSURLSession + semaphore.
 * Must only be called from a background thread — same contract as the
 * desktop HttpURLConnection implementation (15s timeouts).
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual object PlatformHttp {
    actual fun request(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?
    ): Pair<Int, String> {
        val nsUrl = NSURL.URLWithString(url) ?: return -1 to ""
        val request = NSMutableURLRequest()
        request.setURL(nsUrl)
        request.HTTPMethod = method
        request.setTimeoutInterval(15.0)
        for ((k, v) in headers) request.setValue(v, forHTTPHeaderField = k)
        if (body != null) {
            request.HTTPBody = (body as NSString).dataUsingEncoding(NSUTF8StringEncoding)
        }

        val semaphore = dispatch_semaphore_create(0) ?: return -1 to ""
        var resultCode = -1
        var resultBody = ""
        NSURLSession.sharedSession.dataTaskWithRequest(request) { data, response, _ ->
            if (response is NSHTTPURLResponse) {
                resultCode = response.statusCode.toInt()
            }
            if (data != null) {
                resultBody = NSString.create(data, NSUTF8StringEncoding) as? String ?: ""
            }
            dispatch_semaphore_signal(semaphore)
        }.resume()
        // 20s overall bound so a stuck task cannot block the caller forever;
        // the request timeout already enforces the 15s transport timeout.
        dispatch_semaphore_wait(semaphore, dispatch_time(DISPATCH_TIME_NOW, 20_000_000_000))
        return resultCode to resultBody
    }
}
