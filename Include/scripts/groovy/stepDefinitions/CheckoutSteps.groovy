package stepDefinitions

import static com.kms.katalon.core.testdata.TestDataFactory.findTestData

import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.testobject.ObjectRepository as OR
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.util.KeywordUtil

import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import io.cucumber.java.en.Then

class CheckoutSteps {

	private static final int TIMEOUT = 15

	private static final def CHECKOUT_DATA = findTestData('CheckoutData')
	private static final int ROW = 1

	@Given("I am logged in to SauceDemo")
	def iAmLoggedInToSauceDemo() {

		WebUI.openBrowser('')
		WebUI.navigateToUrl('https://www.saucedemo.com/')
		WebUI.maximizeWindow()

		def inputUser = OR.findTestObject('sauceDemo_record/Page_Swag Labs/input_Username')
		def inputPass = OR.findTestObject('sauceDemo_record/Page_Swag Labs/input_Password')
		def btnLogin = OR.findTestObject('sauceDemo_record/Page_Swag Labs/input_login-button')

		WebUI.waitForElementVisible(inputUser, TIMEOUT)
		WebUI.setText(inputUser, CHECKOUT_DATA.getValue('username', ROW))

		WebUI.waitForElementVisible(inputPass, TIMEOUT)
		WebUI.setEncryptedText(inputPass, 'qcu24s4901FyWDTwXGr6XA==')

		WebUI.waitForElementClickable(btnLogin, TIMEOUT)
		WebUI.click(btnLogin)

		WebUI.verifyTextPresent('Products', false)

		KeywordUtil.logInfo("Login exitoso con usuario: ${CHECKOUT_DATA.getValue('username', ROW)}")
	}

	@When("I add Sauce Labs Backpack to the cart")
	def iAddSauceLabsBackpackToTheCart() {

		def btnAddBackpack = OR.findTestObject(
			'sauceDemo_record/Page_Swag Labs/button_add-to-cart-sauce-labs-backpack'
		)

		WebUI.waitForElementClickable(btnAddBackpack, TIMEOUT)
		WebUI.click(btnAddBackpack)

		KeywordUtil.logInfo("Producto agregado al carrito")
	}

	@When("I proceed to checkout")
	def iProceedToCheckout() {

		def cart = OR.findTestObject(
			'sauceDemo_record/Page_Swag Labs/span_Your Cart'
		)

		def btnCheckout = OR.findTestObject(
			'sauceDemo_record/Page_Swag Labs/button_checkout'
		)

		WebUI.waitForElementClickable(cart, TIMEOUT)
		WebUI.click(cart)

		WebUI.verifyTextPresent('Your Cart', false)

		WebUI.waitForElementClickable(btnCheckout, TIMEOUT)
		WebUI.click(btnCheckout)

		WebUI.verifyTextPresent('Checkout: Your Information', false)

		KeywordUtil.logInfo("Ingreso al checkout correctamente")
	}

	@When("I enter checkout information")
	def iEnterCheckoutInformation() {

		def inputFirstName = OR.findTestObject(
			'sauceDemo_record/Page_Swag Labs/input_First Name'
		)

		def inputLastName = OR.findTestObject(
			'sauceDemo_record/Page_Swag Labs/input_Last Name'
		)

		def inputPostalCode = OR.findTestObject(
			'sauceDemo_record/Page_Swag Labs/input_Zip_Postal Code'
		)

		def btnContinue = OR.findTestObject(
			'sauceDemo_record/Page_Swag Labs/input_continue'
		)

		WebUI.waitForElementVisible(inputFirstName, TIMEOUT)

		WebUI.setText(
			inputFirstName,
			CHECKOUT_DATA.getValue('firstName', ROW)
		)

		WebUI.setText(
			inputLastName,
			CHECKOUT_DATA.getValue('lastName', ROW)
		)

		WebUI.setText(
			inputPostalCode,
			CHECKOUT_DATA.getValue('postalCode', ROW)
		)

		WebUI.waitForElementClickable(btnContinue, TIMEOUT)
		WebUI.click(btnContinue)

		WebUI.verifyTextPresent('Checkout: Overview', false)

		KeywordUtil.logInfo(
			"Datos ingresados: ${CHECKOUT_DATA.getValue('firstName', ROW)} ${CHECKOUT_DATA.getValue('lastName', ROW)}"
		)
	}

	@When("I finish the order")
	def iFinishTheOrder() {

		def btnFinish = OR.findTestObject(
			'sauceDemo_record/Page_Swag Labs/button_finish'
		)

		WebUI.waitForElementClickable(btnFinish, TIMEOUT)
		WebUI.click(btnFinish)

		KeywordUtil.logInfo("Orden finalizada")
	}

	@Then("I should see the confirmation message")
	def iShouldSeeTheConfirmationMessage() {

		def confirmationMessage = OR.findTestObject(
			'sauceDemo_record/Page_Swag Labs/h2_Thank you for your order'
		)

		WebUI.waitForElementVisible(
			confirmationMessage,
			TIMEOUT,
			FailureHandling.CONTINUE_ON_FAILURE
		)

		WebUI.verifyElementText(
			confirmationMessage,
			'Thank you for your order!'
		)

		KeywordUtil.logInfo("Compra realizada exitosamente")

		WebUI.closeBrowser()
	}
}