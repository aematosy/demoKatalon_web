import static com.kms.katalon.core.testdata.TestDataFactory.findTestData

import com.kms.katalon.core.cucumber.keyword.CucumberBuiltinKeywords as CucumberKW
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.configuration.RunConfiguration
import zs.demo.cucumber.CucumberReportExtractor
import support.ExcelTestContext
import zs.demo.ExcelExecutionContext
import zs.demo.ExcelExecutionQueue

String dataFileName = 'TestControlBDD'

CucumberKW.GLUE = ['stepDefinitions']

def controlData = findTestData(dataFileName)

int totalRows = controlData.getRowNumbers()

KeywordUtil.logInfo("Iniciando ejecución controlada por Excel: ${dataFileName}")
KeywordUtil.logInfo("Total de filas detectadas por Katalon: ${totalRows}")

boolean hasFailure = false

ExcelExecutionQueue.clear()

for (int row = 1; row <= totalRows; row++) {

	String execute = value(controlData, 'execute', row)
	String featurePath = value(controlData, 'featurePath', row)
	String tagValue = value(controlData, 'tags', row)

	if (!execute && !featurePath && !tagValue) {
		KeywordUtil.logInfo("Fila ${row} vacía. Se omite.")
		continue
	}

	if (execute != '1') {
		KeywordUtil.logInfo("Fila ${row} omitida porque execute=${execute}")
		continue
	}

	if (!featurePath || !tagValue) {
		hasFailure = true
		KeywordUtil.logInfo("Fila ${row} inválida: featurePath o tags vacío")
		continue
	}

	Map data = [
		row         : row.toString(),
		caseId      : value(controlData, 'caseId', row),
		testName    : value(controlData, 'testName', row),
		description : value(controlData, 'description', row),
		username    : value(controlData, 'username', row),
		password    : value(controlData, 'password', row),
		firstName   : value(controlData, 'firstName', row),
		lastName    : value(controlData, 'lastName', row),
		postalCode  : value(controlData, 'postalCode', row),
		featurePath : featurePath,
		tags        : tagValue
	]

	if (!data.caseId) {
		hasFailure = true
		KeywordUtil.logInfo("Fila ${row} inválida: caseId vacío")
		continue
	}

	ExcelTestContext.currentRow = row
	ExcelTestContext.data = data

	ExcelExecutionContext.start(data)

	KeywordUtil.logInfo("========================================")
	KeywordUtil.logInfo("Ejecutando fila ${row}")
	KeywordUtil.logInfo("CaseId: ${data.caseId}")
	KeywordUtil.logInfo("TestName: ${data.testName}")
	KeywordUtil.logInfo("Feature: ${featurePath}")
	KeywordUtil.logInfo("Tag: ${tagValue}")
	KeywordUtil.logInfo("========================================")

	long start = System.currentTimeMillis()

	String status = 'PASSED'
	String errorMessage = ''
	String screenshotPath = ''

	try {

		String[] tags = [tagValue] as String[]

		CucumberKW.runFeatureFileWithTags(featurePath, tags)

		if (ExcelExecutionContext.getSteps().isEmpty()) {

			ExcelExecutionContext.addPassedStep(
				'Ejecución Cucumber',
				'El escenario debe ejecutarse correctamente',
				'Feature ejecutado correctamente'
			)
		}

	} catch (Throwable e) {

	status = 'FAILED'

	errorMessage = e.toString()

	hasFailure = true

		try {

			String projectDir = RunConfiguration.getProjectDir()

			File dir = new File("${projectDir}/Reports/error-screenshots")

			if (!dir.exists()) {
				dir.mkdirs()
			}

			String safeCaseId =
				data.caseId.replaceAll('[^A-Za-z0-9._-]', '_')

			screenshotPath =
				"${dir.absolutePath}/${safeCaseId}_${System.currentTimeMillis()}.png"

			WebUI.takeScreenshot(screenshotPath)

			KeywordUtil.logInfo(
				"Screenshot de error guardado: ${screenshotPath}"
			)

		} catch (Throwable ssEx) {

			KeywordUtil.logInfo(
				"No se pudo tomar screenshot de error: ${ssEx.message}"
			)
		}

		ExcelExecutionContext.addFailedStep(
			'Error técnico en ejecución Cucumber',
			'El feature debe ejecutarse sin errores',
			errorMessage
		)

		KeywordUtil.logInfo("Fila ${row} falló:")
		KeywordUtil.logInfo(errorMessage)

	} finally {

		long end = System.currentTimeMillis()

		if (status == 'FAILED' &&
			ExcelExecutionContext.getSteps().isEmpty()) {

			ExcelExecutionContext.addFailedStep(
				'Fallo de ejecución',
				'El escenario debe ejecutarse correctamente',
				errorMessage
			)
		}
		
		def cucumberReport = CucumberReportExtractor.extractLatest(featurePath, tagValue)

		ExcelExecutionQueue.add([
			data            : ExcelExecutionContext.getData(),
			steps           : ExcelExecutionContext.getSteps(),
			status          : status,
			error           : errorMessage,
			errorScreenshot : screenshotPath,
			cucumberReport : cucumberReport,
			stepScreenshots : ExcelExecutionContext.getStepScreenshots(),
			startMillis     : start,
			endMillis       : end,
			durationMs      : end - start
		])

		ExcelExecutionContext.clear()
	}
}

if (hasFailure) {

	KeywordUtil.logInfo(
		'Ejecución controlada por Excel finalizada con casos fallidos.'
	)

} else {

	KeywordUtil.markPassed(
		'Ejecución controlada por Excel finalizada correctamente'
	)
}

String value(def testData, String column, int row) {

	try {
		return testData.getValue(column, row)?.toString()?.trim() ?: ''
	}
	catch (Throwable ignored) {
		return ''
	}
}