package zs.demo.jira

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.net.URLEncoder
import zs.demo.jira.CucumberEvidenceRenderer
import zs.demo.reporting.WordEvidenceGenerator

class JiraEvidenceService {

	private final String jiraUrl
	private final String jiraUser
	private final String jiraToken
	private final String projectKey
	private final String evidenceIssueTypeId
	private final CucumberEvidenceRenderer renderer = new CucumberEvidenceRenderer()

	JiraEvidenceService(String jiraUrl, String jiraUser, String jiraToken, String projectKey, String evidenceIssueTypeId) {
		this.jiraUrl = jiraUrl?.trim()
		this.jiraUser = jiraUser?.trim()
		this.jiraToken = jiraToken?.trim()
		this.projectKey = projectKey?.trim()
		this.evidenceIssueTypeId = evidenceIssueTypeId?.trim()
	}

	String createEvidenceIssue(String caseId, String description, String executionKey, String status, List<Map> steps, Map data, Map executionItem = [:]) {
		String testCaseName = buildTestCaseName(caseId, data)
		String evidenceDescription = safe(data?.description) ?: '-'

		Map body = [
			fields: [
				project    : [key: projectKey],
				issuetype  : [id: evidenceIssueTypeId],
				summary    : "Evidencia Automatizada - ${testCaseName}",
				description: renderer.build(
				buildTestCaseName(caseId, data),
				safe(data?.description) ?: '-',
				executionKey,
				status,
				steps,
				data,
				executionItem
				)
			]
		]

		println "[JIRA-DEMO] Creando issue de evidencia para ${testCaseName}"

		def response = post("${jiraUrl}/rest/api/3/issue", body)

		if (!response?.key) {
			throw new RuntimeException("No se pudo crear issue de evidencia. Respuesta=${response}")
		}

		println "[JIRA-DEMO] Issue evidencia creado: ${response.key}"
		
		String issueKey = response.key.toString()
		
		File wordEvidence = WordEvidenceGenerator.generate(
			testCaseName,
			executionKey,
			status,
			data,
			executionItem
		)
		
		if (wordEvidence) {
			uploadAttachment(
				issueKey,
				wordEvidence,
				'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
			)
		}
		
		File errorScreenshot = resolveErrorScreenshot(executionItem)
		
		if (errorScreenshot) {
			uploadAttachment(issueKey, errorScreenshot, 'image/png')
		}
		
		File cucumberHtml = resolveCucumberHtmlReport(executionItem)
		
		if (cucumberHtml) {
			uploadAttachment(issueKey, cucumberHtml, 'text/html')
		}
		
		return issueKey
	}

	private Map buildDescription(String caseId, String testCaseName, String description, String executionKey, String status, List<Map> steps, Map data, Map executionItem) {
		List<Map> content = []

		content << heading("Resumen de Ejecución Automatizada", 2)

		content << table([
			['Campo', 'Valor'],
			[
				'Caso de prueba',
				testCaseName
			],
			[
				'Descripción',
				description ?: '-'
			],
			[
				'Ejecución Zephyr',
				executionKey ?: '-'
			],
			['Estado final', status ?: '-'],
			[
				'Feature',
				safe(data?.featurePath)
			],
			['Tag', safe(data?.tags)],
			[
				'Duración',
				formatDurationSeconds(executionItem?.durationMs)
			]
		], true)

		content << heading("Detalle Step by Step", 3)

		if (!steps || steps.isEmpty()) {
			content << paragraph("No se registraron pasos detallados.")
		} else {
			content << buildStepsTable(steps)
		}

		if (safe(executionItem?.error)) {
			content << heading("Error técnico", 3)
			content << paragraph(safe(executionItem?.error))
		}

		return [type: 'doc', version: 1, content: content]
	}

	private String formatDurationSeconds(def durationMs) {
		try {
			String raw = safe(durationMs)
			if (!raw) return '-'

			BigDecimal ms = new BigDecimal(raw)
			BigDecimal seconds = ms.divide(new BigDecimal(1000), 2, BigDecimal.ROUND_HALF_UP)

			return "${seconds}s"
		} catch (Throwable ignored) {
			return '-'
		}
	}

	private Map buildStepsTable(List<Map> steps) {
		List<List<String>> rows = []
		rows << [
			'#',
			'Paso',
			'Resultado esperado',
			'Resultado actual',
			'Estado'
		]

		steps.eachWithIndex { Map step, int index ->
			rows << [
				(index + 1).toString(),
				safe(step.stepName),
				safe(step.expectedResult),
				safe(step.actualResult),
				safe(step.status)
			]
		}

		return table(rows, true)
	}

	private Map table(List<List<String>> rows, boolean firstRowHeader) {
		List<Map> tableRows = []

		rows.eachWithIndex { List<String> values, int index ->
			boolean header = firstRowHeader && index == 0

			tableRows << [
				type   : 'tableRow',
				content: values.collect { String value ->
					[
						type   : header ? 'tableHeader' : 'tableCell',
						content: [paragraph(value ?: '-')]
					]
				}
			]
		}

		return [
			type   : 'table',
			attrs  : [isNumberColumnEnabled: false, layout: 'default'],
			content: tableRows
		]
	}

	private Map heading(String text, int level) {
		return [type: 'heading', attrs: [level: level], content: [
				[type: 'text', text: text ?: '']
			]]
	}

	private Map paragraph(String text) {
		return [type: 'paragraph', content: [
				[type: 'text', text: text ?: '']
			]]
	}

	private def post(String urlText, Map body) {
		HttpURLConnection conn = null

		try {
			conn = (HttpURLConnection) new URL(urlText).openConnection()
			conn.requestMethod = 'POST'
			conn.doOutput = true
			conn.connectTimeout = 15000
			conn.readTimeout = 30000
			conn.setRequestProperty('Authorization', "Basic ${basicAuth()}")
			conn.setRequestProperty('Content-Type', 'application/json')
			conn.setRequestProperty('Accept', 'application/json')

			conn.outputStream.withCloseable { OutputStream os ->
				os.write(JsonOutput.toJson(body).getBytes('UTF-8'))
				os.flush()
			}

			int code = conn.responseCode
			String text = readResponse(conn, code)

			if (code >= 200 && code < 300) {
				return text ? new JsonSlurper().parseText(text) : [:]
			}

			throw new RuntimeException("POST Jira HTTP ${code}: ${text}")
		} finally {
			conn?.disconnect()
		}
	}

	Long getIssueId(String issueKey) {
		def response = get("${jiraUrl}/rest/api/3/issue/${URLEncoder.encode(issueKey, 'UTF-8')}?fields=id")

		if (!response?.id) {
			throw new RuntimeException("No se pudo obtener id interno de Jira para ${issueKey}. Respuesta=${response}")
		}

		return response.id.toString().toLong()
	}

	private def get(String urlText) {
		HttpURLConnection conn = null

		try {
			conn = (HttpURLConnection) new URL(urlText).openConnection()
			conn.requestMethod = 'GET'
			conn.connectTimeout = 15000
			conn.readTimeout = 30000
			conn.setRequestProperty('Authorization', "Basic ${basicAuth()}")
			conn.setRequestProperty('Accept', 'application/json')

			int code = conn.responseCode
			String text = readResponse(conn, code)

			if (code >= 200 && code < 300) {
				return text ? new JsonSlurper().parseText(text) : [:]
			}

			throw new RuntimeException("GET Jira HTTP ${code}: ${text}")
		} finally {
			conn?.disconnect()
		}
	}

	private String basicAuth() {
		String raw = "${jiraUser}:${jiraToken}"
		return raw.bytes.encodeBase64().toString()
	}

	private String readResponse(HttpURLConnection conn, int code) {
		InputStream stream = code >= 200 && code < 300 ? conn.inputStream : conn.errorStream
		return stream ? stream.getText('UTF-8') : ''
	}

	private String safe(def value) {
		return value?.toString()?.trim() ?: ''
	}

	private String buildTestCaseName(String caseId, Map data) {
		String id = safe(caseId)
		String name = safe(data?.testName)

		if (id && name) {
			return "${id}_${name}"
		}

		return id ?: name
	}
	
	private File resolveErrorScreenshot(Map executionItem) {
		String path = safe(executionItem?.errorScreenshot)
	
		if (!path) {
			return null
		}
	
		File file = new File(path)
	
		if (!file.exists() || !file.isFile()) {
			println "[JIRA-DEMO][WARN] Screenshot no existe: ${path}"
			return null
		}
	
		return file
	}
	
	private void uploadAttachment(String issueKey, File file, String contentType) {
		String boundary = "----KatalonEvidence${System.currentTimeMillis()}"
		HttpURLConnection conn = null
	
		try {
			conn = (HttpURLConnection) new URL("${jiraUrl}/rest/api/3/issue/${issueKey}/attachments").openConnection()
			conn.requestMethod = 'POST'
			conn.doOutput = true
			conn.connectTimeout = 15000
			conn.readTimeout = 60000
	
			conn.setRequestProperty('Authorization', "Basic ${basicAuth()}")
			conn.setRequestProperty('X-Atlassian-Token', 'no-check')
			conn.setRequestProperty('Content-Type', "multipart/form-data; boundary=${boundary}")
	
			OutputStream output = conn.outputStream
	
			output.write("--${boundary}\r\n".getBytes('UTF-8'))
			output.write("Content-Disposition: form-data; name=\"file\"; filename=\"${file.name}\"\r\n".getBytes('UTF-8'))
			output.write("Content-Type: ${contentType ?: 'application/octet-stream'}\r\n\r\n".getBytes('UTF-8'))
			output.write(file.bytes)
			output.write("\r\n--${boundary}--\r\n".getBytes('UTF-8'))
	
			output.flush()
			output.close()
	
			int code = conn.responseCode
			String text = readResponse(conn, code)
	
			if (code >= 200 && code < 300) {
				println "[JIRA-DEMO] Archivo adjuntado al issue ${issueKey}: ${file.name}"
			} else {
				throw new RuntimeException("Upload Jira HTTP ${code}: ${text}")
			}
		} finally {
			conn?.disconnect()
		}
	}
	
	private File resolveCucumberHtmlReport(Map executionItem) {
		def report = executionItem?.cucumberReport
		String jsonPath = safe(report?.reportPath)
	
		if (!jsonPath) {
			return null
		}
	
		File jsonFile = new File(jsonPath)
		File reportDir = jsonFile.parentFile
	
		if (!reportDir?.exists()) {
			return null
		}
	
		File html = new File(reportDir, 'cucumber.html')
	
		if (!html.exists() || !html.isFile()) {
			println "[JIRA-DEMO][WARN] No existe cucumber.html en: ${reportDir.absolutePath}"
			return null
		}
	
		return html
	}
	
}