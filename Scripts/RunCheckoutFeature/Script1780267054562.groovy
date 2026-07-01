import com.kms.katalon.core.cucumber.keyword.CucumberBuiltinKeywords as CucumberKW

CucumberKW.GLUE = ['stepDefinitions']
CucumberKW.runFeatureFile('Include/features/sauceDemo/checkout.feature')
