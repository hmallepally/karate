package com.fico.tests;

import com.intuit.karate.junit5.Karate;

class TestRunner {
    
    @Karate.Test
    Karate testPlor() {
        return Karate.run("features/plor/plor_api").relativeTo(getClass());
    }
    
    @Karate.Test
    Karate testAll() {
        return Karate.run("features/plor").relativeTo(getClass());
    }
    
    @Karate.Test
    Karate testSmoke() {
        return Karate.run("features/plor").tags("@smoke").relativeTo(getClass());
    }
    
    @Karate.Test
    Karate testRegression() {
        return Karate.run("features/plor").tags("@regression").relativeTo(getClass());
    }
}
