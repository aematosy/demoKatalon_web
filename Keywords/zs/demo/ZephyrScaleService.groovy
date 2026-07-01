package zs.demo

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import internal.GlobalVariable
import java.net.URLEncoder

class ZephyrScaleService {

	private final String baseUrl
	private final String token
	private final String projectKey
	private final String testCycleKey

	ZephyrScaleService(String baseUrl, String token, String projectKey, String testCycleKey) {
		this.baseUrl = baseUrl?.trim()
		this.token = token?.trim()
		this.projectKey = projectKey?.trim()
		this.testCycleKey = testCycleKey?.trim()
	}

	void validateExistingCycle() {
		if (!testCycleKey) {
			throw new RuntimeException('ZS_TEST_CYCLE_KEY es obligatorio. Esta arquitectura no crea ciclos automáticamente.')
		}

		def response = get("${baseUrl}/testcycles/${URLEncoder.encode(testCycleKey, 'UTF-8')}")

		if (!response) {
			throw new RuntimeException("No existe o no se pudo consultar el ciclo Zephyr: ${testCycleKey}")
		}

		println "[ZS-DEMO] Ciclo validado correctamente: ${testCycleKey}"
	}

	String findOrCreateTestCase(String caseId, String description, boolean createIfMissing) {
		String normalizedCaseId = caseId?.trim()

		if (!normalizedCaseId) {
			throw new RuntimeException('caseId vacío. No se puede buscar/crear Test Case en Zephyr.')
		}

		String existing = findTestCaseByName(normalizedCaseId)

		if (existing) {
			println "[ZS-DEMO] Test Case existente reutilizado sin modificar: ${existing}"
			return existing
		}

		if (!createIfMissing) {
			throw new RuntimeException("No existe Test Case '${normalizedCaseId}' y ZS_TC_CREATE_IF_MISSING=false")
		}

		return createTestCase(normalizedCaseId, description)
	}

	private String findTestCaseByName(String caseId) {
		int startAt = 0
		int safety = 0

		while (safety < 100) {
			String url = "${baseUrl}/testcases?projectKey=${URLEncoder.encode(projectKey, 'UTF-8')}&maxResults=100&startAt=${startAt}"
			def response = get(url)

			def values = response?.values ?: []

			def found = values.find { item ->
				item?.name?.toString()?.trim()?.equalsIgnoreCase(caseId)
			}

			if (found?.key) {
				return found.key.toString()
			}

			if (response?.isLast == true || values.isEmpty()) {
				break
			}

			startAt += values.size()
			safety++
		}

		return null
	}

	private String createTestCase(String caseId, String description) {
		Map body = [
			projectKey: projectKey,
			name      : caseId,
			objective  : safeDescription(description, caseId)
		]

		applyTestCaseMetadata(body)

		String customFieldsJson = getGlobalValue('ZS_TESTCASE_CUSTOMFIELDS_JSON')

		if (customFieldsJson) {
			body.customFields = new JsonSlurper().parseText(customFieldsJson)
		}

		println "[ZS-DEMO] Creando Test Case Zephyr: ${caseId}"
		println "[ZS-DEMO] Body=${body}"

		def response = post("${baseUrl}/testcases", body)

		if (!response?.key) {
			throw new RuntimeException("No se pudo crear Test Case '${caseId}'. Respuesta=${response}")
		}

		println "[ZS-DEMO] Test Case creado: ${response.key}"
		return response.key.toString()
	}

	private void updateExistingTestCaseMetadata(String testCaseKey, String caseName, String description) {
		try {
			Map body = [
				name     : caseName,
				objective: safeDescription(description, caseName)
			]

			applyTestCaseMetadata(body)

			String customFieldsJson = getGlobalValue('ZS_TESTCASE_CUSTOMFIELDS_JSON')

			if (customFieldsJson) {
				body.customFields = new JsonSlurper().parseText(customFieldsJson)
			}

			println "[ZS-DEMO] Actualizando metadata Test Case existente: ${testCaseKey}"
			println "[ZS-DEMO] Body=${body}"

			put("${baseUrl}/testcases/${URLEncoder.encode(testCaseKey, 'UTF-8')}", body)

			println "[ZS-DEMO] Metadata Test Case actualizada: ${testCaseKey}"
		} catch (Throwable e) {
			println "[ZS-DEMO][WARN] No se pudo actualizar metadata del Test Case ${testCaseKey}: ${e.message}"
		}
	}

	private void applyTestCaseMetadata(Map body) {
		String ownerId = getGlobalValue('ZS_TC_OWNER_ACCOUNT_ID')
		String versionName = getGlobalValue('ZS_VERSION_NAME')

		if (ownerId) {
			body.ownerId = ownerId
		}

		if (versionName) {
			body.versionName = versionName
		}
	}

	Map createExecution(String testCaseKey, String status, String comment) {
		return createExecution(testCaseKey, status, comment, null)
	}

	Map createExecution(String testCaseKey, String status, String comment, def durationMs) {
		Map body = [
			projectKey  : projectKey,
			testCaseKey : testCaseKey,
			testCycleKey: testCycleKey,
			statusName  : mapStatus(status),
			comment     : comment ?: ''
		]

		String executorId = getGlobalValue('ZS_EXECUTOR_ACCOUNT_ID')
		String assigneeId = getGlobalValue('ZS_ASSIGNEE_ACCOUNT_ID')
		String environmentName = getGlobalValue('ZS_ENVIRONMENT_NAME')
		String versionName = getGlobalValue('ZS_VERSION_NAME')

		if (executorId) {
			body.executedById = executorId
		}

		if (assigneeId) {
			body.assignedToId = assigneeId
		}

		if (environmentName) {
			body.environmentName = environmentName
		}

		if (versionName) {
			body.versionName = versionName
		}

		Long realTimeMs = toLong(durationMs)

		if (realTimeMs && realTimeMs > 0L) {
			body.executionTime = realTimeMs
		}

		println "[ZS-DEMO] Creando ejecución Zephyr para TC=${testCaseKey}"
		println "[ZS-DEMO] Body=${body}"

		def response = post("${baseUrl}/testexecutions", body)

		if (!response?.id) {
			throw new RuntimeException("No se pudo crear ejecución Zephyr. Respuesta=${response}")
		}

		def execution = get("${baseUrl}/testexecutions/${response.id}")

		return [
			id : response.id,
			key: execution?.key ?: response?.key
		]
	}

	private String mapStatus(String status) {
		switch ((status ?: '').toUpperCase()) {
			case 'PASSED':
				return 'Pass'
			case 'SKIPPED':
				return 'Not Executed'
			default:
				return 'Fail'
		}
	}

	private def get(String urlText) {
		HttpURLConnection conn = null

		try {
			conn = openConnection(urlText, 'GET')
			int code = conn.responseCode
			String text = readResponse(conn, code)

			if (code >= 200 && code < 300) {
				return text ? new JsonSlurper().parseText(text) : [:]
			}

			throw new RuntimeException("GET Zephyr HTTP ${code}: ${text}")
		} finally {
			conn?.disconnect()
		}
	}

	private def post(String urlText, Map body) {
		HttpURLConnection conn = null

		try {
			conn = openConnection(urlText, 'POST')
			conn.doOutput = true
			conn.setRequestProperty('Content-Type', 'application/json')

			byte[] payload = JsonOutput.toJson(body).getBytes('UTF-8')
			conn.outputStream.write(payload)
			conn.outputStream.flush()
			conn.outputStream.close()

			int code = conn.responseCode
			String text = readResponse(conn, code)

			if (code >= 200 && code < 300) {
				return text ? new JsonSlurper().parseText(text) : [:]
			}

			throw new RuntimeException("POST Zephyr HTTP ${code}: ${text}")
		} finally {
			conn?.disconnect()
		}
	}

	private def put(String urlText, Map body) {
		HttpURLConnection conn = null

		try {
			conn = openConnection(urlText, 'PUT')
			conn.doOutput = true
			conn.setRequestProperty('Content-Type', 'application/json')

			byte[] payload = JsonOutput.toJson(body).getBytes('UTF-8')
			conn.outputStream.write(payload)
			conn.outputStream.flush()
			conn.outputStream.close()

			int code = conn.responseCode
			String text = readResponse(conn, code)

			if (code >= 200 && code < 300) {
				return text ? new JsonSlurper().parseText(text) : [:]
			}

			throw new RuntimeException("PUT Zephyr HTTP ${code}: ${text}")
		} finally {
			conn?.disconnect()
		}
	}

	private HttpURLConnection openConnection(String urlText, String method) {
		HttpURLConnection conn = (HttpURLConnection) new URL(urlText).openConnection()
		conn.requestMethod = method
		conn.connectTimeout = 15000
		conn.readTimeout = 30000
		conn.setRequestProperty('Authorization', "Bearer ${token}")
		conn.setRequestProperty('Accept', 'application/json')
		conn.setRequestProperty('User-Agent', 'Katalon-Zephyr-Demo/1.0')
		return conn
	}

	private String readResponse(HttpURLConnection conn, int code) {
		InputStream stream = code >= 200 && code < 300 ? conn.inputStream : conn.errorStream
		return stream ? stream.getText('UTF-8') : ''
	}

	private String getGlobalValue(String name) {
		try {
			def field = GlobalVariable.class.getField(name)
			def value = field.get(null)
			return value?.toString()?.trim()
		} catch (Throwable ignored) {
			return ''
		}
	}

	private Long toLong(def value) {
		try {
			if (value == null) {
				return null
			}

			String raw = value.toString().trim()

			if (!raw) {
				return null
			}

			return raw.toLong()
		} catch (Throwable ignored) {
			return null
		}
	}

	void linkIssueToExecution(String executionKey, Long issueId) {
		if (!executionKey) {
			throw new RuntimeException("executionKey vacío. No se puede vincular issue.")
		}

		if (!issueId) {
			throw new RuntimeException("issueId vacío. No se puede vincular issue.")
		}

		Map body = [
			issueId: issueId
		]

		println "[ZS-DEMO] Vinculando issue Jira ${issueId} a execution ${executionKey}"

		def response = post("${baseUrl}/testexecutions/${URLEncoder.encode(executionKey, 'UTF-8')}/links/issues", body)

		println "[ZS-DEMO] Issue vinculado a ejecución Zephyr. Response=${response}"
	}

	private String safeDescription(String description, String fallback) {
		String value = description?.toString()?.trim()
		return value ?: fallback
	}
}