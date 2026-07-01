package support

import com.kms.katalon.core.configuration.RunConfiguration
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import io.cucumber.java.AfterStep
import io.cucumber.java.Scenario
import zs.demo.ExcelExecutionContext

class CucumberStepScreenshotHook {

	//@AfterStep
	void afterStep(Scenario scenario) {

		try {

			if (DriverFactory.getWebDriver() == null) {
				return
			}

			String caseId =
				ExcelExecutionContext.get('caseId') ?: 'NO_CASE'

			File dir = new File(
				"${RunConfiguration.getProjectDir()}/Reports/step-screenshots/${caseId}"
			)

			if (!dir.exists()) {
				dir.mkdirs()
			}

			String path =
				"${dir.absolutePath}/step_${System.currentTimeMillis()}.png"

			WebUI.takeScreenshot(path)

			ExcelExecutionContext.addStepScreenshot(path)

		} catch (Throwable ignored) {
			// Nunca romper ejecución por evidencia
		}
	}
}