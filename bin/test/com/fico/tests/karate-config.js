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
        config.baseUrl = 'http://localhost:8090';
    } else if (env == 'test') {
        config.baseUrl = 'http://test-server:8090';
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
        }
    };
    
    karate.log('Environment:', env);
    karate.log('Base URL:', config.baseUrl);
    
    return config;
}
