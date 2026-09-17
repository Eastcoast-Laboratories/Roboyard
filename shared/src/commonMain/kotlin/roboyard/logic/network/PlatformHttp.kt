package roboyard.logic.network

/**
 * Minimal synchronous HTTP transport for the shared API client.
 * Implementations perform a blocking request and return (statusCode, body).
 * Must always be called from a background thread / coroutine.
 */
expect object PlatformHttp {
    /**
     * Perform an HTTP request.
     * @param method "GET" or "POST"
     * @param url full URL
     * @param headers request headers
     * @param body request body for POST, ignored for GET
     * @return Pair of (HTTP status code, response body)
     */
    fun request(
        method: String,
        url: String,
        headers: Map<String, String>,
        body: String?
    ): Pair<Int, String>
}
