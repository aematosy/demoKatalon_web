@ExcelCheckout
Feature: SauceDemo Checkout desde Excel

  Scenario: Complete checkout using Excel data
    Given Excel user is logged in to SauceDemo
    When Excel user adds Sauce Labs Backpack to the cart
    And Excel user proceeds to checkout
    And Excel user enters checkout information
    And Excel user finishes the order
    Then Excel user should see the confirmation message