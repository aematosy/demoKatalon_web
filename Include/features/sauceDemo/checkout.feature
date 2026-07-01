@Checkout
Feature: SauceDemo Checkout

  Scenario: Complete checkout successfully
    Given I am logged in to SauceDemo
    When I add Sauce Labs Backpack to the cart
    And I proceed to checkout
    And I enter checkout information
    And I finish the order
    Then I should see the confirmation message
