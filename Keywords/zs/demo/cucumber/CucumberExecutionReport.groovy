package zs.demo.cucumber

class CucumberExecutionReport {

	String feature = ''
	String scenario = ''
	String status = ''
	String failedStep = ''
	String exceptionType = ''
	String message = ''
	String location = ''
	String reportPath = ''

	List<Map> steps = []

	boolean hasData() {
		return feature || scenario || steps
	}
}