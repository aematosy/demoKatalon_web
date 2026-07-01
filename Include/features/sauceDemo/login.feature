Feature: Login en SauceDemo

  Como usuario de SauceDemo,
  Quiero poder iniciar y cerrar sesión en el sistema,
  Para acceder a la tienda y proteger mi cuenta.

  Background:
    # El background se ejecuta antes de cada escenario.
    Given Que estoy en la página de login de SauceDemo

  @ValidLogin
  Scenario Outline: Inicio de sesión exitoso con credenciales válidas
    When Ingreso el nombre de usuario "<username>" y la contraseña "<password>"
    And Hago clic en el botón de login
    Then Debería ser redirigido al inventario y ver el carrito de compras

    Examples:
      | username       | password     |
      | standard_user  | secret_sauce |
      | problem_user   | secret_sauce |
      | performance_glitch_user | secret_sauce |

  @InvalidLogin
  Scenario: Inicio de sesión fallido con credenciales inválidas
    When Ingreso el nombre de usuario "locked_out_user" y la contraseña "secret_sauce"
    And Hago clic en el botón de login
    Then Debería ver un mensaje de error indicando que el usuario está bloqueado