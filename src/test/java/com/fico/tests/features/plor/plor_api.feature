Feature: FICO PLOR API Decisioning

Background:
  * url baseUrl
  * def basePayload = karate.basePayload
  * def schema = karate.schema
  * def utils = karate.utils

@smoke @regression
Scenario Outline: Process PLOR application and validate decisioning for <testCaseName>

  # Convert CSV row to a map for dynamic processing
  * def csvRow = { testCaseName: '<testCaseName>', 'application.applicationId': '<application.applicationId>', 'application.bomVersionId': '<application.bomVersionId>', 'applicants[0].firstName': '<applicants[0].firstName>', 'applicants[0].lastName': '<applicants[0].lastName>', 'applicants[0].creditProfile.creditScore': '<applicants[0].creditProfile.creditScore>', 'response.creditDecisioning.decisioning.subProductDecisions[0].decisionSummary.automatedDecisionCode': '<response.creditDecisioning.decisioning.subProductDecisions[0].decisionSummary.automatedDecisionCode>', 'response.creditDecisioning.decisioning.subProductDecisions[0].creditLineAssignment.creditLimitAmount': '<response.creditDecisioning.decisioning.subProductDecisions[0].creditLineAssignment.creditLimitAmount>' }
  
  # Dynamically populate request payload from CSV using schema paths
  * def requestPayload = utils.populateRequestFromCsv(basePayload, csvRow)
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
  
  # Dynamically validate response against CSV expectations using JSON paths
  * def validationResults = utils.validateResponseFromCsv(response, csvRow)
  * utils.logValidationResults(validationResults)
  
  # Assert that all validations passed
  * def allPassed = utils.allValidationsPassed(validationResults)
  * assert allPassed == true
  
  # Additional structural validations
  * match response.creditDecisioning.applicationId == csvRow['application.applicationId']

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
