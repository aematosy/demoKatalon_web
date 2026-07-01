import com.kms.katalon.core.annotation.AfterTestSuite
import com.kms.katalon.core.annotation.BeforeTestSuite
import com.kms.katalon.core.context.TestSuiteContext
import internal.GlobalVariable
import zs.demo.ExcelExecutionQueue
import zs.demo.ZephyrScaleService
import zs.demo.jira.JiraEvidenceService

class ExcelZephyrEvidenceListener {

	private ZephyrScaleService zephyr
	private JiraEvidenceService jira

	@BeforeTestSuite
	def beforeSuite(TestSuiteContext suiteContext) {
		if (!enabled()) {
			println '[ZS-DEMO] Integración Zephyr/Jira desactivada'
			return
		}

		try {
			initializeServices()

			println "[ZS-DEMO] Listener Web/Excel inicializado correctamente"
			println "[ZS-DEMO] Suite=${suiteContext.getTestSuiteId()}"
			println "[ZS-DEMO] Proyecto=${getGlobal('ZS_PROJECT_KEY')}"
			println "[ZS-DEMO] Ciclo=${getGlobal('ZS_TEST_CYCLE_KEY')}"
		} catch (Throwable e) {
			println "[ZS-DEMO][WARN] No se pudo inicializar listener Web/Excel: ${e.message}"
			e.printStackTrace()
		}
	}

	@AfterTestSuite
	def afterSuite(TestSuiteContext suiteContext) {
		if (!enabled()) {
			return
		}

		List<Map> executions = ExcelExecutionQueue.consumeAll()

		println "[ZS-DEMO] Total ejecuciones Web/Excel a publicar: ${executions.size()}"

		if (!executions) {
			println '[ZS-DEMO][WARN] No hay ejecuciones Web/Excel acumuladas'
			return
		}

		try {
			ensureServicesReady()
		} catch (Throwable e) {
			println "[ZS-DEMO][ERROR] No se pudo preparar servicios Zephyr/Jira. Se omite publicación, pero no se rompe la suite: ${e.message}"
			e.printStackTrace()
			return
		}

		executions.eachWithIndex { Map item, int index ->
			publishExecution(item, index + 1)
		}

		println '[ZS-DEMO] Publicación Web/Excel finalizada'
	}

	private void publishExecution(Map item, int index) {
		Map data = (item?.data ?: [:]) as Map

		String caseId = safe(data.caseId)
		String testName = safe(data.testName)
		String status = normalizeStatus(safe(item.status))
		String testCaseName = buildTestCaseName(caseId, testName)
		String description = safe(data.description) ?: testName ?: caseId

		if (!caseId && !testName) {
			println "[ZS-DEMO][WARN] Ejecución ${index} sin caseId/testName. No se publica."
			return
		}

		boolean createIfMissing = getGlobal('ZS_TC_CREATE_IF_MISSING').equalsIgnoreCase('true')

		println ''
		println "[ZS-DEMO] =================================================="
		println "[ZS-DEMO] Publicando ejecución Web/Excel ${index}"
		println "[ZS-DEMO] caseId=${caseId}"
		println "[ZS-DEMO] testName=${testCaseName}"
		println "[ZS-DEMO] status=${status}"
		println "[ZS-DEMO] steps=${(item.steps ?: []).size()}"
		println "[ZS-DEMO] =================================================="

		try {
			String testCaseKey = zephyr.findOrCreateTestCase(
				testCaseName,
				description,
				createIfMissing
			)

			Map execution = zephyr.createExecution(
				testCaseKey,
				status,
				"Ejecutado desde Katalon Web/Excel BDD. caseId=${caseId}, testName=${testName}",
				item.durationMs
			)

			String executionKey = safe(execution?.key)

			String evidenceIssueKey = jira.createEvidenceIssue(
				caseId ?: testCaseName,
				description,
				executionKey,
				status,
				item.steps ?: [],
				data,
				item
			)

			try {
				Long issueId = jira.getIssueId(evidenceIssueKey)
				zephyr.linkIssueToExecution(executionKey, issueId)
			} catch (Throwable linkEx) {
				println "[ZS-DEMO][WARN] No se pudo vincular issue Jira con ejecución Zephyr: ${linkEx.message}"
			}

			println "[ZS-DEMO] Publicación Web/Excel OK"
			println "[ZS-DEMO] TestCase=${testCaseKey}"
			println "[ZS-DEMO] Execution=${executionKey}"
			println "[ZS-DEMO] Evidence=${evidenceIssueKey}"

		} catch (Throwable e) {
			println "[ZS-DEMO][ERROR] No se pudo publicar Web/Excel caseId=${caseId}: ${e.message}"
			e.printStackTrace()
		}
	}

	private void initializeServices() {
		validateRequiredGlobals()

		zephyr = new ZephyrScaleService(
			getGlobal('ZS_ZEPHYR_BASE_URL'),
			getGlobal('ZS_ZEPHYR_TOKEN'),
			getGlobal('ZS_PROJECT_KEY'),
			getGlobal('ZS_TEST_CYCLE_KEY')
		)

		jira = new JiraEvidenceService(
			getGlobal('ZS_JIRA_URL'),
			getGlobal('ZS_JIRA_USER'),
			getGlobal('ZS_JIRA_TOKEN'),
			getGlobal('ZS_PROJECT_KEY'),
			getGlobal('ZS_EVIDENCE_ISSUE_TYPE_ID')
		)

		zephyr.validateExistingCycle()
	}

	private void ensureServicesReady() {
		if (zephyr == null || jira == null) {
			initializeServices()
		}
	}

	private boolean enabled() {
		return getGlobal('ZS_ENABLE_INTEGRATION').equalsIgnoreCase('true')
	}

	private String normalizeStatus(String status) {
		switch ((status ?: '').toUpperCase()) {
			case 'PASSED':
			case 'PASS':
				return 'PASSED'
			case 'SKIPPED':
			case 'SKIP':
				return 'SKIPPED'
			default:
				return 'FAILED'
		}
	}

	private String buildTestCaseName(String caseId, String testName) {
		String format = getGlobal('ZS_TC_NAME_FORMAT')

		if (format == 'CASE_ID_TEST_NAME' && caseId && testName) {
			return "${caseId}_${testName}"
		}

		return caseId ?: testName
	}

	private void validateRequiredGlobals() {
		List<String> missing = []

		required('ZS_ENABLE_INTEGRATION', missing)
		required('ZS_TEST_CYCLE_KEY', missing)
		required('ZS_PROJECT_KEY', missing)
		required('ZS_ZEPHYR_BASE_URL', missing)
		required('ZS_ZEPHYR_TOKEN', missing)
		required('ZS_JIRA_URL', missing)
		required('ZS_JIRA_USER', missing)
		required('ZS_JIRA_TOKEN', missing)
		required('ZS_EVIDENCE_ISSUE_TYPE_ID', missing)

		if (missing) {
			throw new RuntimeException("Faltan GlobalVariables obligatorias: ${missing.join(', ')}")
		}
	}

	private void required(String name, List<String> missing) {
		if (!getGlobal(name)) {
			missing << name
		}
	}

	private String getGlobal(String name) {
		try {
			def field = GlobalVariable.class.getField(name)
			def value = field.get(null)
			return value?.toString()?.trim() ?: ''
		} catch (Throwable ignored) {
			return ''
		}
	}

	private String safe(def value) {
		return value?.toString()?.trim() ?: ''
	}
}