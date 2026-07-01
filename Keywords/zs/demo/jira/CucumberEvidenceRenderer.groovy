package zs.demo.jira

class CucumberEvidenceRenderer {

	Map build(String caseName, String description, String executionKey, String status, List<Map> steps, Map data, Map executionItem) {
		List<Map> content = []

		content << heading("Reporte Cucumber - Ejecución Automatizada", 2)

		content << panel(status, [
			"Estado: ${status ?: '-'}",
			"Caso de prueba: ${caseName ?: '-'}",
			"Descripción: ${description ?: '-'}",
			"Ejecución Zephyr: ${executionKey ?: '-'}",
			"Feature: ${safe(data?.featurePath)}",
			"Tag: ${safe(data?.tags)}",
			"Duración: ${formatDurationSeconds(executionItem?.durationMs)}"
		])

		content << heading("Escenario ejecutado", 3)

		if (!steps || steps.isEmpty()) {
			content << paragraph("No se registraron pasos detallados.")
		} else {
			content << stepsTable(steps)
		}

		def report = executionItem?.cucumberReport

		if (report?.hasData()) {
			content << heading("Reporte Cucumber en formato Gherkin", 3)
			content.addAll(cucumberGherkinBlock(report))
		}

		if (safe(executionItem?.errorScreenshot)) {
			content << heading("Evidencia visual del error", 3)
			content << paragraph("Screenshot adjunto al issue.")
		}

		return [
			type   : "doc",
			version: 1,
			content: content
		]
	}

	private List<Map> cucumberGherkinBlock(def report) {
	List<Map> content = []

	content << heading("Feature: ${safe(report.feature) ?: '-'}", 3)
	content << paragraph("Scenario: ${safe(report.scenario) ?: '-'}")

	report.steps.each { Map step ->
		String status = safe(step.status).toUpperCase()
		String line = "${iconStatus(status)} ${safe(step.keyword)} ${safe(step.name)} ${formatDurationNanos(step.duration)}".trim()

		content << paragraph(line)

		if (status == "FAILED" && safe(step.error)) {
			content << panel("FAILED", normalizeErrorLines(step.error))
		}
	}

	return content
}

	private List<String> normalizeErrorLines(def errorValue) {
		String raw = safe(errorValue)

		if (!raw) {
			return ["Error no disponible."]
		}

		List<String> lines = raw.readLines()
			.collect { it?.replaceAll("\\u001B\\[[;\\d]*m", "")?.trim() }
			.findAll { it }

		if (lines.isEmpty()) {
			return ["Error no disponible."]
		}

		return lines.take(20)
	}

	private Map stepsTable(List<Map> steps) {
		List<Map> rows = []

		rows << tableRow(["#", "Estado", "Paso", "Resultado esperado", "Resultado actual"], true)

		steps.eachWithIndex { Map step, int index ->
			rows << tableRow([
				(index + 1).toString(),
				iconStatus(step.status),
				safe(step.stepName),
				safe(step.expectedResult),
				safe(step.actualResult)
			], false)
		}

		return [
			type: "table",
			attrs: [
				isNumberColumnEnabled: false,
				layout: "wide"
			],
			content: rows
		]
	}

	private Map panel(String status, List<String> lines) {
		String panelType = "info"

		if (safe(status).equalsIgnoreCase("PASSED")) {
			panelType = "success"
		} else if (safe(status).equalsIgnoreCase("FAILED")) {
			panelType = "error"
		}

		return [
			type: "panel",
			attrs: [panelType: panelType],
			content: lines.collect { paragraph(it) }
		]
	}

	private Map tableRow(List<String> values, boolean header) {
		return [
			type: "tableRow",
			content: values.collect { String value ->
				[
					type: header ? "tableHeader" : "tableCell",
					content: [paragraph(value ?: "-")]
				]
			}
		]
	}

	private Map heading(String text, int level) {
		return [
			type: "heading",
			attrs: [level: level],
			content: [[type: "text", text: text ?: ""]]
		]
	}

	private Map paragraph(String text) {
		String clean = safe(text)
			.replaceAll("\\u001B\\[[;\\d]*m", "")
			.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "")

		return [
			type: "paragraph",
			content: [[type: "text", text: clean ?: "-"]]
		]
	}

	private String iconStatus(def status) {
	String value = safe(status).toUpperCase()

	if (value == "PASSED" || value == "PASS") {
		return "✅"
	}

	if (value == "FAILED" || value == "FAIL") {
		return "❌"
	}

	if (value == "SKIPPED" || value == "SKIP") {
		return "⏭"
	}

	return "•"
}

	private String formatDurationSeconds(def millisValue) {
		try {
			long millis = millisValue ? millisValue.toString().toLong() : 0L
			if (millis <= 0L) {
				return "-"
			}
			return String.format(Locale.US, "%.2fs", millis / 1000.0D)
		} catch (Throwable ignored) {
			return "-"
		}
	}

	private String formatDurationNanos(def nanosValue) {
		try {
			long nanos = nanosValue ? nanosValue.toString().toLong() : 0L
			if (nanos <= 0L) {
				return ""
			}
			return String.format(Locale.US, "%.2fs", nanos / 1000000000.0D)
		} catch (Throwable ignored) {
			return ""
		}
	}

	private String safe(def value) {
		return value?.toString()?.trim() ?: ""
	}
}