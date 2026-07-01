package zs.demo.jira

class ErrorDiagnosticBuilder {

	static List<Map> build(Map executionItem) {

		if (!executionItem?.error) {
			return []
		}

		String error = executionItem.error.toString()

		List<List<String>> rows = []

		rows << ['Campo', 'Valor']

		rows << [
			'Tipo de excepción',
			extractExceptionType(error)
		]

		rows << [
			'Mensaje',
			extractExceptionMessage(error)
		]

		rows << [
			'Detalle completo',
			error
		]

		rows << [
			'Screenshot',
			executionItem?.errorScreenshot ?: '-'
		]

		return rows
	}

	private static String extractExceptionType(String error) {

		def matcher =
				(error =~ /Exception Type:\s*(.+)/)

		if (matcher.find()) {
			return matcher.group(1)
		}

		return '-'
	}

	private static String extractExceptionMessage(String error) {

		def matcher =
				(error =~ /Message:\s*(.+)/)

		if (matcher.find()) {
			return matcher.group(1)
		}

		return '-'
	}
}