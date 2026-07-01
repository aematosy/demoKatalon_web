package zs.demo

class ExcelExecutionContext {

	private static final ThreadLocal<Map> DATA = ThreadLocal.withInitial { [:] }
	private static final ThreadLocal<List<Map>> STEPS = ThreadLocal.withInitial { [] }
	private static final ThreadLocal<List<String>> STEP_SCREENSHOTS = ThreadLocal.withInitial { [] }

	static void start(Map data) {
		DATA.set(data ?: [:])
		STEPS.set([])
		STEP_SCREENSHOTS.set([])
	}

	static void setData(Map data) {
		start(data)
	}

	static Map getData() {
		return DATA.get() ?: [:]
	}

	static String get(String key) {
		return (getData()[key] ?: '').toString()
	}

	static String username() {
		return get('username')
	}

	static String password() {
		return get('password')
	}

	static String firstName() {
		return get('firstName')
	}

	static String lastName() {
		return get('lastName')
	}

	static String postalCode() {
		return get('postalCode')
	}

	static String caseId() {
		return get('caseId')
	}

	static String testName() {
		return get('testName')
	}

	static void addPassedStep(String stepName, String expectedResult, String actualResult = '') {
		addStep(stepName, expectedResult, actualResult, 'PASSED')
	}

	static void addFailedStep(String stepName, String expectedResult, String actualResult = '') {
		addStep(stepName, expectedResult, actualResult, 'FAILED')
	}

	static void addStep(String stepName, String expectedResult, String actualResult, String status) {
		String screenshotPath = captureStepScreenshot()
	
		STEPS.get() << [
			stepName      : stepName ?: '',
			expectedResult: expectedResult ?: '',
			actualResult  : actualResult ?: '',
			status        : status ?: 'PASSED',
			screenshot    : screenshotPath ?: ''
		]
	
		if (screenshotPath) {
			addStepScreenshot(screenshotPath)
		}
	}

	static List<Map> getSteps() {
		return STEPS.get() ?: []
	}

	static void addStepScreenshot(String path) {
		if (path) {
			STEP_SCREENSHOTS.get() << path
		}
	}

	static List<String> getStepScreenshots() {
		return STEP_SCREENSHOTS.get() ?: []
	}

	static void clear() {
		DATA.remove()
		STEPS.remove()
		STEP_SCREENSHOTS.remove()
	}
	
	private static String captureStepScreenshot() {
		try {
			if (!"true".equalsIgnoreCase(internal.GlobalVariable.ZS_ENABLE_INTEGRATION?.toString())) {
				return ''
			}
	
			def driver = com.kms.katalon.core.webui.driver.DriverFactory.getWebDriver()
	
			if (driver == null) {
				return ''
			}
	
			String caseId = get('caseId') ?: 'NO_CASE'
	
			File dir = new File("${com.kms.katalon.core.configuration.RunConfiguration.getProjectDir()}/Reports/step-screenshots/${caseId}")
	
			if (!dir.exists()) {
				dir.mkdirs()
			}
	
			String path = "${dir.absolutePath}/step_${System.currentTimeMillis()}.png"
	
			File src = driver.getScreenshotAs(org.openqa.selenium.OutputType.FILE)
	
			java.nio.file.Files.copy(
				src.toPath(),
				new File(path).toPath(),
				java.nio.file.StandardCopyOption.REPLACE_EXISTING
			)
	
			return path
	
		} catch (Throwable ignored) {
			return ''
		}
	}
}