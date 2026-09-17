package roboyard.logic.network

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

actual object PlatformHttp {
    actual fun request(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?
    ): Pair<Int, String> {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            for ((k, v) in headers) conn.setRequestProperty(k, v)
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            if (body != null) {
                conn.doOutput = true
                conn.outputStream.use { os ->
                    val bytes = body.toByteArray(StandardCharsets.UTF_8)
                    os.write(bytes, 0, bytes.size)
                }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.use { s ->
                BufferedReader(InputStreamReader(s, StandardCharsets.UTF_8)).readText()
            } ?: ""
            return code to text
        } finally {
            conn.disconnect()
        }
    }
}
