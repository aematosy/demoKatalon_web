package stepDefinitions

import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.testobject.ObjectRepository as OR
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.util.KeywordUtil

import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import io.cucumber.java.en.Then

class LoginSteps {

	private static final int TIMEOUT = 15

	@Given("Que estoy en la página de login de SauceDemo")
	def queEstoyEnLaPaginaDeLogin() {
		WebUI.openBrowser('')
		WebUI.navigateToUrl('https://www.saucedemo.com/')
		WebUI.maximizeWindow()
		
		WebUI.waitForElementPresent(OR.findTestObject('sauceDemo/Login/input_Username'), TIMEOUT)
		WebUI.verifyElementPresent(OR.findTestObject('sauceDemo/Login/input_Username'), TIMEOUT, FailureHandling.CONTINUE_ON_FAILURE)
		
		KeywordUtil.logInfo("Página de login cargada correctamente")
	}

	@When(/^Ingreso el nombre de usuario "([^"]*)" y la contraseña "([^"]*)"$/)
	def ingresoCredenciales(String username, String password) {
		def inputUser = OR.findTestObject('sauceDemo/Login/input_Username')
		def inputPass = OR.findTestObject('sauceDemo/Login/input_Password')
		
		WebUI.waitForElementVisible(inputUser, TIMEOUT)
		WebUI.waitForElementClickable(inputUser, TIMEOUT)
		
		WebUI.setText(inputUser, username)
		WebUI.setText(inputPass, password)
		
		KeywordUtil.logInfo("Credenciales ingresadas: ${username}")
	}

	@When("Hago clic en el botón de login")
	def hagoClickEnLogin() {
		def btnLogin = OR.findTestObject('sauceDemo/Login/button_Login')
		
		WebUI.waitForElementClickable(btnLogin, TIMEOUT)
		WebUI.click(btnLogin)
		
		KeywordUtil.logInfo("✅ Click en botón Login realizado")
	}

	@Then("Debería ser redirigido al inventario y ver el carrito de compras")
	def verificarInventario() {
		// Espera más robusta para la página de inventario
		WebUI.waitForElementPresent(OR.findTestObject('sauceDemo/Inventory/icon_ShoppingCart'), TIMEOUT, FailureHandling.CONTINUE_ON_FAILURE)
		WebUI.waitForElementVisible(OR.findTestObject('sauceDemo/Inventory/icon_ShoppingCart'), TIMEOUT)
		
		WebUI.verifyElementPresent(OR.findTestObject('sauceDemo/Inventory/icon_ShoppingCart'), TIMEOUT)
		WebUI.verifyTextPresent('Products', false)
		
		KeywordUtil.logInfo("Redirección al inventario exitosa")
	}

	@Then("Debería ver un mensaje de error indicando que el usuario está bloqueado")
	def verificarMensajeErrorBloqueo() {
		WebUI.waitForPageLoad(TIMEOUT)
		
		WebUI.verifyTextPresent("Epic sadface: Sorry, this user has been locked out.", false, FailureHandling.CONTINUE_ON_FAILURE)
		WebUI.closeBrowser()
		
		KeywordUtil.logInfo("Mensaje de usuario bloqueado verificado")
	}
}