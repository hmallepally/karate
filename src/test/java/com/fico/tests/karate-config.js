function fn() {
    var config = {};
    
    var YamlSchemaUtils = Java.type('com.fico.karate.utils.YamlSchemaUtils');
    config.schema = YamlSchemaUtils.loadSchemaAsMap('classpath:api_schema.yaml');
    
    config.basePayload = read('classpath:com/fico/tests/features/plor/plor_api_template.json');
    
    var env = karate.env;
    if (!env) {
        env = 'dev';
    }
    
    config.env = env;
    
    if (env == 'dev') {
        config.baseUrl = 'http://localhost:8080';
    } else if (env == 'test') {
        config.baseUrl = 'http://test-server:8080';
    } else if (env == 'prod') {
        config.baseUrl = 'https://api.fico.com';
    }
    
    config.headers = {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
    };
    
    config.utils = {
        generateUniqueId: function() {
            return 'APP-' + java.lang.System.currentTimeMillis();
        },
        getCurrentTimestamp: function() {
            return new java.util.Date().toString();
        },
        
        populateRequestFromCsv: function(templatePayload, csvRow) {
            try {
                return YamlSchemaUtils.populateJsonFromCsv(templatePayload, csvRow);
            } catch (e) {
                karate.log('Error populating request from CSV:', e.message);
                throw e;
            }
        },
        
        validateResponseFromCsv: function(responseJson, csvRow) {
            try {
                return YamlSchemaUtils.validateResponseFromCsv(responseJson, csvRow);
            } catch (e) {
                karate.log('Error validating response from CSV:', e.message);
                throw e;
            }
        },
        
        allValidationsPassed: function(validationResults) {
            var allPassed = true;
            for (var key in validationResults) {
                if (!validationResults[key].valid) {
                    karate.log('Validation failed for', key + ':', validationResults[key].message);
                    karate.log('Expected:', validationResults[key].expectedValue, 'Actual:', validationResults[key].actualValue);
                    allPassed = false;
                }
            }
            return allPassed;
        },
        
        logValidationResults: function(validationResults) {
            karate.log('=== Response Validation Results ===');
            for (var key in validationResults) {
                var result = validationResults[key];
                karate.log(key + ':', result.valid ? 'PASS' : 'FAIL', 
                    '| Expected:', result.expectedValue, 
                    '| Actual:', result.actualValue,
                    '| Message:', result.message);
            }
            karate.log('===================================');
        }
    };
    
    karate.log('Environment:', env);
    karate.log('Base URL:', config.baseUrl);
    karate.log('Dynamic JSON processing utilities loaded');
    
    return config;
}
