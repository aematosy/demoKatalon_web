package zs.demo.cucumber

import com.kms.katalon.core.configuration.RunConfiguration
import groovy.json.JsonSlurper

class CucumberReportExtractor {

	static CucumberExecutionReport extractLatest(String featurePath, String tagValue) {
		File jsonFile = findLatestCucumberJson()

		if (jsonFile == null) {
			return new CucumberExecutionReport()
		}

		return parse(jsonFile)
	}

	private static File findLatestCucumberJson() {
		String projectDir = RunConfiguration.getProjectDir()
		File reportsRoot = new File("${projectDir}/Reports")

		if (!reportsRoot.exists()) {
			return null
		}

		List<File> jsons = []

		reportsRoot.eachFileRecurse { File file ->
			if (file.isFile() && file.name.equalsIgnoreCase('k-cucumber.json')) {
				jsons << file
			}
		}

		if (jsons.isEmpty()) {
			reportsRoot.eachFileRecurse { File file ->
				if (file.isFile() && file.name.equalsIgnoreCase('cucumber.json')) {
					jsons << file
				}
			}
		}

		if (jsons.isEmpty()) {
			return null
		}

		return jsons.max { it.lastModified() }
	}

	private static CucumberExecutionReport parse(File jsonFile) {
		CucumberExecutionReport report = new CucumberExecutionReport()
		report.reportPath = jsonFile.absolutePath

		def parsed = new JsonSlurper().parse(jsonFile)

		List features = parsed instanceof List ? parsed : [parsed]

		features.each { feature ->
			report.feature = safe(feature?.name) ?: safe(feature?.uri)

			def elements = feature?.elements ?: feature?.scenarios ?: []

			elements.each { scenario ->
				report.scenario = safe(scenario?.name)

				def steps = scenario?.steps ?: []

				steps.each { step ->
					Map result = step?.result ?: [:]
					String status = safe(result?.status).toUpperCase()

					Map row = [
						keyword : safe(step?.keyword),
						name    : safe(step?.name),
						status  : status ?: 'UNKNOWN',
						duration: result?.duration ?: 0,
						error   : safe(result?.error_message)
					]

					report.steps << row

					if (status == 'FAILED' && !report.failedStep) {
						report.status = 'FAILED'
						report.failedStep = "${row.keyword} ${row.name}".trim()

						String error = row.error
						report.exceptionType = extractExceptionType(error)
						report.message = extractExceptionMessage(error)
						report.location = extractLocation(error)
					}
				}
			}
		}

		if (!report.status) {
			report.status = report.steps.any { it.status == 'FAILED' } ? 'FAILED' : 'PASSED'
		}

		return report
	}

	private static String extractExceptionType(String error) {
		if (!error) return ''

		def m = error =~ /([a-zA-Z0-9_.$]+(?:Exception|Error))/
		if (m.find()) {
			return m.group(1)
		}

		return ''
	}

	private static String extractExceptionMessage(String error) {
		if (!error) return ''

		String[] lines = error.readLines().collect { it.trim() }.findAll { it }

		if (lines.size() >= 1) {
			String first = lines[0]
			if (first.contains(':')) {
				return first.substring(first.indexOf(':') + 1).trim()
			}
			return first
		}

		return ''
	}

	private static String extractLocation(String error) {
		if (!error) return ''

		def m = error =~ /\(([^()]+\.groovy:\d+)\)/
		if (m.find()) {
			return m.group(1)
		}

		m = error =~ /\(([^()]+\.java:\d+)\)/
		if (m.find()) {
			return m.group(1)
		}

		return ''
	}

	private static String safe(def value) {
		return value?.toString()?.trim() ?: ''
	}
}