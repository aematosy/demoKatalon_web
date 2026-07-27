import org.apache.poi.xwpf.usermodel.XWPFDocument as XWPFDocument
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import com.kms.katalon.core.cucumber.keyword.CucumberBuiltinKeywords as CucumberKW
import com.kms.katalon.core.webservice.keyword.WSBuiltInKeywords as WS
import com.kms.katalon.core.windows.keyword.WindowsBuiltinKeywords as Windows
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import static com.kms.katalon.core.testobject.ObjectRepository.findWindowsObject
import static com.kms.katalon.core.testdata.TestDataFactory.findTestData
import static com.kms.katalon.core.testcase.TestCaseFactory.findTestCase
import static com.kms.katalon.core.checkpoint.CheckpointFactory.findCheckpoint
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.testcase.TestCase as TestCase
import com.kms.katalon.core.testdata.TestData as TestData
import com.kms.katalon.core.testobject.TestObject as TestObject
import com.kms.katalon.core.checkpoint.Checkpoint as Checkpoint
import org.openqa.selenium.Keys as Keys
import internal.GlobalVariable as GlobalVariable

WebUI.openBrowser(null)

WebUI.navigateToUrl('https://www.saucedemo.com/')

WebUI.setText(findTestObject('sauceDemo_record/login_exitoso/Page_Swag Labs/input_Username'), 'standard_user')

WebUI.setEncryptedText(findTestObject('sauceDemo_record/login_exitoso/Page_Swag Labs/input_Password'), 'qcu24s4901FyWDTwXGr6XA==')

WebUI.click(findTestObject('sauceDemo_record/login_exitoso/Page_Swag Labs/input_login-button'))

WebUI.click(findTestObject('sauceDemo_record/login_exitoso/Page_Swag Labs/button_add-to-cart-sauce-labs-backpack'))

