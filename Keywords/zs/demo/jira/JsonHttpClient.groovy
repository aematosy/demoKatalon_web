package zs.demo.jira

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

class JsonHttpClient {

	private final int connectTimeoutMs = 15000
	private final int readTimeoutMs = 30000

	def get(String url, Map<String, String> headers) {
		HttpURLConnection conn = open(url, 'GET', headers)
		return parse(conn)
	}

	def post(String url, Map<String, String> headers, Map body) {
		HttpURLConnection conn = open(url, 'POST', headers)
		conn.doOutput = true
		conn.outputStream.withWriter('UTF-8') {
			it << JsonOutput.toJson(body ?: [:])
		}
		return parse(conn)
	}

	private HttpURLConnection open(String url, String method, Map<String, String> headers) {
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection()
		conn.requestMethod = method
		conn.connectTimeout = connectTimeoutMs
		conn.readTimeout = readTimeoutMs
		conn.instanceFollowRedirects = false
		headers?.each { k, v ->
			conn.setRequestProperty(k, v)
		}
		return conn
	}

	private def parse(HttpURLConnection conn) {
		int code = conn.responseCode
		String text = code >= 200 && code < 300 ?
			conn.inputStream?.getText('UTF-8') :
			conn.errorStream?.getText('UTF-8')

		if (code < 200 || code >= 300) {
			throw new RuntimeException("HTTP ${code}: ${text}")
		}

		if (!text?.trim()) {
			return [:]
		}

		return new JsonSlurper().parseText(text)
	}
}