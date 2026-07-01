import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

// Abrir navegador
WebUI.openBrowser('')

// Navegar a SauceDemo
WebUI.navigateToUrl('https://www.saucedemo.com/')

// Login
WebUI.setText(
	findTestObject('sauceDemo_record/Page_Swag Labs/input_Username'),
	'standard_user'
)

WebUI.setEncryptedText(
	findTestObject('sauceDemo_record/Page_Swag Labs/input_Password'),
	'qcu24s4901FyWDTwXGr6XA=='
)

WebUI.click(
	findTestObject('sauceDemo_record/Page_Swag Labs/input_login-button')
)

// Verificar inventario
WebUI.verifyTextPresent('Products', false)

// Agregar producto
WebUI.click(
	findTestObject('sauceDemo_record/Page_Swag Labs/button_add-to-cart-sauce-labs-backpack')
)

// Ir al carrito
WebUI.click(
	findTestObject('sauceDemo_record/Page_Swag Labs/span_Your Cart')
)

// Verificar carrito
WebUI.verifyTextPresent('Your Cart', false)

// Checkout
WebUI.click(
	findTestObject('sauceDemo_record/Page_Swag Labs/button_checkout')
)

// Verificar pantalla de información
WebUI.verifyTextPresent('Checkout: Your Information', false)

// Completar datos
WebUI.setText(
	findTestObject('sauceDemo_record/Page_Swag Labs/input_First Name'),
	'Adrian'
)

WebUI.setText(
	findTestObject('sauceDemo_record/Page_Swag Labs/input_Last Name'),
	'Matos'
)

WebUI.setText(
	findTestObject('sauceDemo_record/Page_Swag Labs/input_Zip_Postal Code'),
	'15001'
)

// Continuar
WebUI.click(
	findTestObject('sauceDemo_record/Page_Swag Labs/input_continue')
)

// Verificar overview
WebUI.verifyTextPresent('Checkout: Overview', false)

// Finalizar compra
WebUI.click(
	findTestObject('sauceDemo_record/Page_Swag Labs/button_finish')
)

// Validar compra exitosa
WebUI.verifyElementText(
	findTestObject('sauceDemo_record/Page_Swag Labs/h2_Thank you for your order'),
	'Thank you for your order!'
)

// Cerrar navegador
WebUI.closeBrowser()