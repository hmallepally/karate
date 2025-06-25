package com.fico.mock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.*;

@SpringBootApplication
@RestController
public class MockServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MockServerApplication.class, args);
    }

    @PostMapping("/oauth/token")
    public ResponseEntity<Map<String, Object>> authenticate(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        response.put("access_token", "mock_access_token_" + System.currentTimeMillis());
        response.put("token_type", "Bearer");
        response.put("expires_in", 3600);
        response.put("scope", "read write");
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/plor/v1/transaction")
    public ResponseEntity<Map<String, Object>> processTransaction(@RequestBody Map<String, Object> request) {
        try {
            String applicationId = extractApplicationId(request);
            String firstName = extractFirstName(request);
            String lastName = extractLastName(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("transactionId", "TXN_" + System.currentTimeMillis());
            response.put("status", "SUCCESS");
            response.put("timestamp", new Date().toString());
            
            Map<String, Object> creditDecisioning = createCreditDecisioningResponse(firstName, lastName, applicationId);
            response.put("creditDecisioning", creditDecisioning);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", "Processing failed: " + e.getMessage());
            errorResponse.put("timestamp", new Date().toString());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PostMapping("/api/dm/decision")
    public ResponseEntity<Map<String, Object>> makeDecision(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        response.put("decisionId", "DEC_" + System.currentTimeMillis());
        response.put("status", "COMPLETED");
        response.put("timestamp", new Date().toString());
        
        Map<String, Object> decision = new HashMap<>();
        decision.put("outcome", "APPROVED");
        decision.put("confidence", 0.95);
        decision.put("riskScore", 650);
        
        response.put("decision", decision);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", new Date().toString());
        return ResponseEntity.ok(response);
    }

    private String extractApplicationId(Map<String, Object> request) {
        if (request.containsKey("application")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> application = (Map<String, Object>) request.get("application");
            return (String) application.get("applicationId");
        }
        return "DEFAULT_APP_ID";
    }

    private String extractFirstName(Map<String, Object> request) {
        if (request.containsKey("applicants")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> applicants = (List<Map<String, Object>>) request.get("applicants");
            if (!applicants.isEmpty()) {
                return (String) applicants.get(0).get("firstName");
            }
        }
        return "John";
    }

    private String extractLastName(Map<String, Object> request) {
        if (request.containsKey("applicants")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> applicants = (List<Map<String, Object>>) request.get("applicants");
            if (!applicants.isEmpty()) {
                return (String) applicants.get(0).get("lastName");
            }
        }
        return "Doe";
    }

    private Map<String, Object> createCreditDecisioningResponse(String firstName, String lastName, String applicationId) {
        Map<String, Object> creditDecisioning = new HashMap<>();
        
        Map<String, Object> decisioning = new HashMap<>();
        List<Map<String, Object>> subProductDecisions = new ArrayList<>();
        
        Map<String, Object> subProductDecision = new HashMap<>();
        
        Map<String, Object> decisionSummary = new HashMap<>();
        String decisionCode = determineDecisionCode(firstName, lastName);
        decisionSummary.put("automatedDecisionCode", decisionCode);
        decisionSummary.put("decisionCode", decisionCode);
        decisionSummary.put("decisionReason", getDecisionReason(decisionCode));
        
        Map<String, Object> creditLineAssignment = new HashMap<>();
        int creditLimit = determineCreditLimit(decisionCode);
        creditLineAssignment.put("creditLimitAmount", creditLimit);
        creditLineAssignment.put("interestRate", decisionCode.equals("A") ? 12.99 : 0.0);
        
        subProductDecision.put("decisionSummary", decisionSummary);
        subProductDecision.put("creditLineAssignment", creditLineAssignment);
        subProductDecision.put("productCode", "CC001");
        
        subProductDecisions.add(subProductDecision);
        decisioning.put("subProductDecisions", subProductDecisions);
        
        creditDecisioning.put("decisioning", decisioning);
        creditDecisioning.put("applicationId", applicationId);
        creditDecisioning.put("processedTimestamp", new Date().toString());
        
        return creditDecisioning;
    }

    private String determineDecisionCode(String firstName, String lastName) {
        if ("Jane".equalsIgnoreCase(firstName) && "Smith".equalsIgnoreCase(lastName)) {
            return "D";
        } else if ("Risk".equalsIgnoreCase(lastName)) {
            return "D";
        } else if ("Pending".equalsIgnoreCase(lastName)) {
            return "P";
        }
        return "A";
    }

    private String getDecisionReason(String decisionCode) {
        switch (decisionCode) {
            case "A": return "Approved - Good credit profile";
            case "D": return "Declined - High risk profile";
            case "P": return "Pending - Manual review required";
            default: return "Unknown decision";
        }
    }

    private int determineCreditLimit(String decisionCode) {
        switch (decisionCode) {
            case "A": return 15000;
            case "D": return 0;
            case "P": return 5000;
            default: return 0;
        }
    }
}
