Feature: FICO PLOR API Decisioning

Background:
  * url baseUrl
  * def basePayload = basePayload
  * def schema = schema
  * def utils = utils

@smoke @regression
Scenario Outline: Process PLOR application and validate decisioning for <testCaseName>

  # Use a simple static request payload for now to test basic functionality
  * def requestPayload = basePayload
  * karate.log('Generated request payload:', requestPayload)
  
  # Execute the API call
  Given path '/api/plor/v1/transaction'
  And request requestPayload
  When method post
  Then status 200
  
  # Validate the response structure
  * match response.status == 'SUCCESS'
  * match response.transactionId == '#string'
  * match response.creditDecisioning == '#object'
  
  # Simplified validation for now
  * karate.log('Response received:', response)
  
  # Additional structural validations - simplified for now
  * match response.creditDecisioning.applicationId == '#string'

Examples:
| karate.read('classpath:testdata/plor_test_scenarios.csv') |

@smoke
Scenario: Verify mock server health endpoint
  Given path '/health'
  When method get
  Then status 200
  * match response.status == 'UP'

@regression
Scenario: Test authentication endpoint
  Given path '/oauth/token'
  And request { grant_type: 'client_credentials', client_id: 'test', client_secret: 'test' }
  When method post
  Then status 200
  * match response.access_token == '#string'
  * match response.token_type == 'Bearer'
  * match response.expires_in == '#number'

@regression
Scenario: Test DM decision endpoint
  Given path '/api/dm/decision'
  And request { applicationId: 'APP-TEST', riskFactors: ['income', 'credit_score'] }
  When method post
  Then status 200
  * match response.status == 'COMPLETED'
  * match response.decision.outcome == '#string'
  * match response.decision.confidence == '#number'
