package stepDefinitions

import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.testobject.ObjectRepository as OR
import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.util.KeywordUtil

import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import io.cucumber.java.en.Then

import zs.demo.ExcelExecutionContext

class ExcelCheckoutSteps {

	private static final int TIMEOUT = 15

	@Given("Excel user is logged in to SauceDemo")
	def excelUserIsLoggedInToSauceDemo() {
		String username = ExcelExecutionContext.username()
		String password = ExcelExecutionContext.password()

		if (!username || !password) {
			ExcelExecutionContext.addFailedStep(
				"Login SauceDemo",
				"El Excel debe enviar username y password",
				"username='${username}', passwordEmpty=${!password}"
			)
			throw new IllegalArgumentException("username/password vacío desde ExcelExecutionContext")
		}

		WebUI.openBrowser('')
		WebUI.navigateToUrl('https://www.saucedemo.com/')
		WebUI.maximizeWindow()

		def inputUser = OR.findTestObject('sauceDemo_record/Page_Swag Labs/input_Username')
		def inputPass = OR.findTestObject('sauceDemo_record/Page_Swag Labs/input_Password')
		def btnLogin = OR.findTestObject('sauceDemo_record/Page_Swag Labs/input_login-button')

		WebUI.waitForElementVisible(inputUser, TIMEOUT)
		WebUI.setText(inputUser, username)

		WebUI.waitForElementVisible(inputPass, TIMEOUT)
		WebUI.setText(inputPass, password)

		WebUI.waitForElementClickable(btnLogin, TIMEOUT)
		WebUI.click(btnLogin)

		WebUI.verifyTextPresent('Products', false)

		ExcelExecutionContext.addPassedStep(
			"Login SauceDemo",
			"El usuario debe iniciar sesión correctamente",
			"Login OK con usuario ${username}"
		)

		KeywordUtil.logInfo("Login OK desde Excel. Usuario: ${username}")
	}

	@When("Excel user adds Sauce Labs Backpack to the cart")
	def excelUserAddsBackpackToCart() {
		def btnAddBackpack = OR.findTestObject('sauceDemo_record/Page_Swag Labs/button_add-to-cart-sauce-labs-backpack')

		WebUI.waitForElementClickable(btnAddBackpack, TIMEOUT)
		WebUI.click(btnAddBackpack)

		ExcelExecutionContext.addPassedStep(
			"Agregar producto",
			"El producto Sauce Labs Backpack debe agregarse al carrito",
			"Producto agregado"
		)
	}

	@When("Excel user proceeds to checkout")
	def excelUserProceedsToCheckout() {
		def cart = OR.findTestObject('sauceDemo_record/Page_Swag Labs/span_Your Cart')
		def btnCheckout = OR.findTestObject('sauceDemo_record/Page_Swag Labs/button_checkout')

		WebUI.waitForElementClickable(cart, TIMEOUT)
		WebUI.click(cart)

		WebUI.waitForElementClickable(btnCheckout, TIMEOUT)
		WebUI.click(btnCheckout)

		WebUI.verifyTextPresent('Checkout: Your Information', false)

		ExcelExecutionContext.addPassedStep(
			"Iniciar checkout",
			"Debe mostrarse la pantalla Checkout: Your Information",
			"Checkout iniciado"
		)
	}

	@When("Excel user enters checkout information")
	def excelUserEntersCheckoutInformation() {
		String firstName = ExcelExecutionContext.firstName()
		String lastName = ExcelExecutionContext.lastName()
		String postalCode = ExcelExecutionContext.postalCode()

		if (!firstName || !lastName || !postalCode) {
			ExcelExecutionContext.addFailedStep(
				"Ingresar datos checkout",
				"El Excel debe enviar firstName, lastName y postalCode",
				"firstName='${firstName}', lastName='${lastName}', postalCode='${postalCode}'"
			)
			throw new IllegalArgumentException("Datos de checkout vacíos desde ExcelExecutionContext")
		}

		def inputFirstName = OR.findTestObject('sauceDemo_record/Page_Swag Labs/input_First Name')
		def inputLastName = OR.findTestObject('sauceDemo_record/Page_Swag Labs/input_Last Name')
		def inputPostalCode = OR.findTestObject('sauceDemo_record/Page_Swag Labs/input_Zip_Postal Code')
		def btnContinue = OR.findTestObject('sauceDemo_record/Page_Swag Labs/input_continue')

		WebUI.waitForElementVisible(inputFirstName, TIMEOUT)

		WebUI.setText(inputFirstName, firstName)
		WebUI.setText(inputLastName, lastName)
		WebUI.setText(inputPostalCode, postalCode)

		WebUI.waitForElementClickable(btnContinue, TIMEOUT)
		WebUI.click(btnContinue)

		WebUI.verifyTextPresent('Checkout: Overview', false)

		ExcelExecutionContext.addPassedStep(
			"Ingresar datos checkout",
			"Debe mostrarse Checkout: Overview",
			"Datos ingresados: ${firstName} ${lastName} ${postalCode}"
		)
	}

	@When("Excel user finishes the order")
	def excelUserFinishesTheOrder() {
		def btnFinish = OR.findTestObject('sauceDemo_record/Page_Swag Labs/button_finish')

		WebUI.waitForElementClickable(btnFinish, TIMEOUT)
		WebUI.click(btnFinish)

		ExcelExecutionContext.addPassedStep(
			"Finalizar orden",
			"La orden debe finalizar correctamente",
			"Click en Finish realizado"
		)
	}

	@Then("Excel user should see the confirmation message")
	def excelUserShouldSeeConfirmationMessage() {
		def confirmationMessage = OR.findTestObject('sauceDemo_record/Page_Swag Labs/h2_Thank you for your order')

		WebUI.waitForElementVisible(confirmationMessage, TIMEOUT, FailureHandling.CONTINUE_ON_FAILURE)
		WebUI.verifyElementText(confirmationMessage, 'Thank you for your order!')

		ExcelExecutionContext.addPassedStep(
			"Validar confirmación",
			"Debe mostrarse Thank you for your order!",
			"Confirmación visible"
		)

		WebUI.closeBrowser()
	}
}