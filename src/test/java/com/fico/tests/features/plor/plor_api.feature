Feature: FICO PLOR API Decisioning

Background:
  * url baseUrl
  * def basePayload = karate.basePayload
  * def schema = karate.schema

@smoke @regression
Scenario Outline: Process PLOR application and validate decisioning for <testCaseName>

  # Create a fresh request payload for this test run
  * def requestPayload = karate.clone(basePayload)
  
  # Dynamically set request values from the CSV
  * set requestPayload.application.applicationId = <application.applicationId>
  * set requestPayload.applicants[0].firstName = <applicants[0].firstName>
  * set requestPayload.applicants[0].lastName = <applicants[0].lastName>
  
  # Set additional fields if provided
  * if (<application.bomVersionId>) set requestPayload.application.bomVersionId = <application.bomVersionId>
  * if (<applicants[0].creditProfile.creditScore>) set requestPayload.applicants[0].creditProfile.creditScore = <applicants[0].creditProfile.creditScore>
  
  # Execute the API call
  Given path '/api/plor/v1/transaction'
  And request requestPayload
  When method post
  Then status 200
  
  # Validate the response structure
  * match response.status == 'SUCCESS'
  * match response.transactionId == '#string'
  * match response.creditDecisioning == '#object'
  
  # Validate the response against the CSV expectations
  * def expectedDecisionCode = <response.creditDecisioning.decisioning.subProductDecisions[0].decisionSummary.automatedDecisionCode>
  * def expectedCreditLimit = <response.creditDecisioning.decisioning.subProductDecisions[0].creditLineAssignment.creditLimitAmount>
  
  * match response.creditDecisioning.decisioning.subProductDecisions[0].decisionSummary.automatedDecisionCode == expectedDecisionCode
  * match response.creditDecisioning.decisioning.subProductDecisions[0].creditLineAssignment.creditLimitAmount == expectedCreditLimit
  
  # Additional validations
  * match response.creditDecisioning.decisioning.subProductDecisions[0].decisionSummary.decisionCode == expectedDecisionCode
  * match response.creditDecisioning.applicationId == <application.applicationId>

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
