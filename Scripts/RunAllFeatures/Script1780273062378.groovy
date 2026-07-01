import com.kms.katalon.core.cucumber.keyword.CucumberBuiltinKeywords as CucumberKW
import com.kms.katalon.core.util.KeywordUtil

String featureFolder = 'Include/features/sauceDemo'
String[] tags = ['@Checkout'] as String[]

KeywordUtil.logInfo("Iniciando ejecución Cucumber con tags: ${tags}")

CucumberKW.GLUE = ['stepDefinitions']

try {
	CucumberKW.runFeatureFolderWithTags(featureFolder, tags)
	KeywordUtil.markPassed('Ejecución Cucumber con tags finalizada correctamente')
} catch (Exception e) {
	KeywordUtil.markFailed("Ejecución Cucumber con tags fallida: ${e.getMessage()}")
	throw e
}