# Karate API Test Automation Framework

A comprehensive, data-driven API test automation framework using Karate with Java-based mock endpoints for FICO PLOR/DM platform testing.

## 🚀 Features

- **Data-Driven Testing**: CSV-based test scenarios with dot-notation headers
- **Java Mock Server**: Spring Boot-based mock endpoints simulating FICO PLOR/DM APIs
- **YAML Schema Processing**: Pre-loaded API schema for dynamic payload creation
- **Jenkins Integration**: Automated twice-daily test execution
- **Comprehensive Test Coverage**: Smoke, regression, and performance test categories
- **Elegant Architecture**: Clean, reusable code with minimal clutter

## 📁 Project Structure

```
├── Jenkinsfile                                    # CI/CD pipeline configuration
├── build.gradle                                   # Gradle build configuration
├── src/
│   ├── main/java/com/fico/
│   │   ├── karate/utils/YamlSchemaUtils.java      # YAML schema preprocessing utility
│   │   └── mock/MockServerApplication.java        # Spring Boot mock server
│   └── test/
│       ├── java/com/fico/tests/
│       │   ├── TestRunner.java                    # JUnit 5 test runner
│       │   ├── features/plor/
│       │   │   ├── plor_api.feature              # Main API test scenarios
│       │   │   └── plor_api_template.json        # Base JSON payload template
│       │   └── karate-config.js                  # Karate configuration
│       └── resources/
│           ├── api_schema.yaml                    # OpenAPI schema definition
│           └── testdata/plor_test_scenarios.csv   # Test data scenarios
```

## 🛠️ Setup Instructions

### Prerequisites

- Java 17 or higher
- Gradle 8.5 or higher
- Git

### Quick Start

1. **Clone the repository**
   ```bash
   git clone https://github.com/hmallepally/karate.git
   cd karate
   ```

2. **Build the project**
   ```bash
   ./gradlew clean build
   ```

3. **Start the mock server**
   ```bash
   ./gradlew bootRun
   ```
   The mock server will start on `http://localhost:8090`

4. **Run tests** (in a separate terminal)
   ```bash
   ./gradlew test
   ```

### Environment Configuration

The framework supports multiple environments:

- **dev** (default): `http://localhost:8090`
- **test**: `http://test-server:8090`
- **prod**: `https://api.fico.com`

Set environment using:
```bash
./gradlew test -Dkarate.env=test
```

## 🧪 Testing Guide

### Test Categories

Run specific test categories using tags:

```bash
# Smoke tests only
./gradlew test -Dkarate.options="--tags @smoke"

# Regression tests only
./gradlew test -Dkarate.options="--tags @regression"

# Exclude specific tags
./gradlew test -Dkarate.options="--tags ~@slow"
```

### Dynamic JSON Processing

The framework uses **dynamic JSON manipulation** to keep tests agnostic to payload structure changes:

#### Request Population
- CSV headers use **dot-notation** (a.b.c format) to map to JSON paths
- `YamlSchemaUtils.populateJsonFromCsv()` automatically populates request templates
- Supports nested objects and arrays: `applicants[0].firstName`
- Automatic type conversion (strings → numbers, booleans)

#### Response Validation  
- CSV headers with **"response."** prefix define expected response values
- `YamlSchemaUtils.validateResponseFromCsv()` validates using JSON paths
- Detailed validation results with clear error messages
- Comprehensive logging for debugging

### Creating New Test Scenarios

1. **Add test data to CSV**
   
   Edit `src/test/resources/testdata/plor_test_scenarios.csv`:
   ```csv
   testCaseName,application.applicationId,applicants[0].firstName,applicants[0].lastName,response.creditDecisioning.decisioning.subProductDecisions[0].decisionSummary.automatedDecisionCode,response.creditDecisioning.decisioning.subProductDecisions[0].creditLineAssignment.creditLimitAmount
   NewTestCase,APP-2001,Test,User,A,20000
   ```

2. **CSV Header Convention**
   - Use **dot-notation** for nested JSON fields: `application.applicationId`
   - Use **bracket notation** for arrays: `applicants[0].firstName`
   - Prefix response validations with `response.`: `response.creditDecisioning.decisioning...`

3. **Test automatically picks up new CSV rows** - no code changes needed!

### Schema-Driven Architecture

**Key Benefit**: Only the YAML schema and payload template need updates when API structure changes!

- **Feature files remain unchanged** when API structure evolves
- **CSV structure stays consistent** across API versions
- **Dynamic processing** handles complex nested JSON automatically
- **Error handling** provides clear feedback for debugging

### Mock Server Endpoints

The mock server provides the following endpoints:

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/health` | GET | Health check endpoint |
| `/oauth/token` | POST | Authentication endpoint |
| `/api/plor/v1/transaction` | POST | Main PLOR processing endpoint |
| `/api/dm/decision` | POST | DM decision endpoint |

### Mock Response Logic

The mock server implements intelligent response logic:

- **Approval (A)**: Default for most applicants, credit limit: 15,000
- **Decline (D)**: Triggered by lastName "Smith" or "Risk", credit limit: 0
- **Pending (P)**: Triggered by lastName "Pending", credit limit: 5,000

## 🏗️ Developer Guide

### Adding New Feature Files

1. Create feature file in `src/test/java/com/fico/tests/features/`
2. Follow existing patterns for Background and Scenario Outline
3. Use global variables: `basePayload`, `schema`, `baseUrl`

### Extending Mock Server

Add new endpoints in `MockServerApplication.java`:

```java
@PostMapping("/api/new-endpoint")
public ResponseEntity<Map<String, Object>> newEndpoint(@RequestBody Map<String, Object> request) {
    Map<String, Object> response = new HashMap<>();
    response.put("status", "SUCCESS");
    return ResponseEntity.ok(response);
}
```

### YAML Schema Updates

Modify `src/test/resources/api_schema.yaml` to add new API structures:

```yaml
components:
  schemas:
    NewSchema:
      type: object
      properties:
        field1:
          type: string
        field2:
          type: number
```

### Dynamic JSON Processing

The enhanced `YamlSchemaUtils` provides powerful dynamic capabilities:

#### Core Methods
```java
// Populate request from CSV using dot-notation paths
Map<String, Object> populatedJson = YamlSchemaUtils.populateJsonFromCsv(templateJson, csvRow);

// Validate response against CSV expectations
Map<String, ValidationResult> results = YamlSchemaUtils.validateResponseFromCsv(responseJson, csvRow);
```

#### Supported Path Formats
- **Simple paths**: `application.applicationId`
- **Array access**: `applicants[0].firstName`
- **Nested objects**: `creditProfile.creditScore`
- **Complex paths**: `decisioning.subProductDecisions[0].decisionSummary.automatedDecisionCode`

#### Type Conversion
- **Numbers**: `"750"` → `750` (integer), `"15.5"` → `15.5` (double)
- **Booleans**: `"true"` → `true`, `"false"` → `false`
- **Strings**: Preserved as-is

### Custom Utilities

The utility class provides comprehensive JSON manipulation:

```java
// Load schema once at startup
Map<String, Object> schema = YamlSchemaUtils.loadSchemaAsMap("classpath:api_schema.yaml");

// Dynamic request population
Map<String, Object> request = YamlSchemaUtils.populateJsonFromCsv(template, csvData);

// Response validation with detailed results
Map<String, ValidationResult> validation = YamlSchemaUtils.validateResponseFromCsv(response, csvData);
```

### Adding New Utility Functions

Extend the `config.utils` object in `karate-config.js`:

```javascript
config.utils.customValidator = function(data) {
    // Custom validation logic
    return YamlSchemaUtils.validateResponseFromCsv(data.response, data.csvRow);
};
```

## 📋 Best Practices

### Code Standards

1. **Java Conventions**
   - Use camelCase for variables and methods
   - Use PascalCase for classes
   - Follow Spring Boot naming conventions
   - Add proper error handling and logging

2. **Karate Conventions**
   - Use descriptive scenario names
   - Group related tests in feature files
   - Use Background for common setup
   - Leverage global configuration variables

3. **Test Data Management**
   - Keep CSV files organized and well-documented
   - Use meaningful test case names
   - Include both positive and negative scenarios
   - Maintain data consistency across environments

### Performance Optimization

1. **Schema Loading**
   - YAML schema is loaded once at startup via `YamlSchemaUtils`
   - Reuse base payload templates across tests
   - Minimize file I/O operations during test execution

2. **Mock Server**
   - Stateless design for better scalability
   - Efficient response generation
   - Proper resource cleanup

3. **Test Execution**
   - Use parallel execution for faster feedback
   - Tag tests appropriately for selective execution
   - Implement proper test isolation

## 🔧 Troubleshooting

### Common Issues

1. **Mock Server Won't Start**
   ```bash
   # Check if port 8090 is already in use
   lsof -i :8090
   
   # Kill existing process if needed
   kill -9 <PID>
   ```

2. **Tests Fail to Connect**
   ```bash
   # Verify mock server is running
   curl http://localhost:8090/health
   
   # Check environment configuration
   ./gradlew test -Dkarate.env=dev --info
   ```

3. **YAML Schema Loading Issues**
   ```bash
   # Verify YAML syntax
   ./gradlew build --info
   
   # Check classpath resources
   ./gradlew dependencies
   ```

### Debug Mode

Enable debug logging:

```bash
# Karate debug mode
./gradlew test -Dkarate.options="--debug"

# Spring Boot debug mode
./gradlew bootRun --debug-jvm
```

## 🚀 CI/CD Integration

### Jenkins Pipeline

The framework includes a `Jenkinsfile` configured for:

- **Scheduled Execution**: Twice daily at 5 AM and 5 PM Central Time
- **Automated Build**: Clean build with dependency resolution
- **Test Execution**: Full test suite with reporting
- **Artifact Archival**: Test reports and build artifacts

### Pipeline Stages

1. **Checkout**: Source code retrieval
2. **Build**: Gradle clean build
3. **Start Mock Server**: Background service startup
4. **Test**: Karate test execution
5. **Cleanup**: Resource cleanup and reporting

### Custom Pipeline Configuration

Modify `Jenkinsfile` for custom requirements:

```groovy
pipeline {
    agent any
    
    triggers {
        cron('0 */4 * * *')
    }
    
    stages {
        stage('Custom Stage') {
            steps {
                script {
                }
            }
        }
    }
}
```

## 📚 API Documentation

### Request/Response Examples

#### PLOR Transaction Processing

**Request:**
```json
{
  "application": {
    "applicationId": "APP-1001",
    "bomVersionId": "v1.0",
    "productCode": "CC001"
  },
  "applicants": [{
    "firstName": "John",
    "lastName": "Doe",
    "creditProfile": {
      "creditScore": 750
    }
  }]
}
```

**Response:**
```json
{
  "status": "SUCCESS",
  "transactionId": "TXN_1234567890",
  "creditDecisioning": {
    "decisioning": {
      "subProductDecisions": [{
        "decisionSummary": {
          "automatedDecisionCode": "A",
          "decisionReason": "Approved - Good credit profile"
        },
        "creditLineAssignment": {
          "creditLimitAmount": 15000,
          "interestRate": 12.99
        }
      }]
    }
  }
}
```

## 🤝 Contributing

### Development Workflow

1. **Fork the repository**
2. **Create feature branch**: `git checkout -b feature/new-feature`
3. **Make changes** following coding standards
4. **Add tests** for new functionality
5. **Run test suite**: `./gradlew test`
6. **Submit pull request** with detailed description

### Code Review Guidelines

- Ensure all tests pass
- Follow established coding conventions
- Include appropriate documentation
- Add test coverage for new features
- Verify mock server compatibility

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 📞 Support

For questions or issues:

1. Check the troubleshooting section
2. Review existing GitHub issues
3. Create new issue with detailed description
4. Include relevant logs and configuration

---

**Built with ❤️ using Karate, Spring Boot, and Java**
