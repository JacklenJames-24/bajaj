package com.example.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class WebhookSubmitApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(WebhookSubmitApplication.class, args);
    }

    @Override
    public void run(String... args) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            // 1. Generate webhook
            String url = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

            Map<String, String> body = new HashMap<>();
            body.put("name", "Jacklen James");  
            body.put("regNo", "BCE7754");       
            body.put("email", "jacklen.22bce7754@vitapstudent.ac.in"); 

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                System.err.println("❌ Failed to generate webhook");
                return;
            }

            // Extract webhook + accessToken from API
            String webhook = (String) response.getBody().get("webhook");
            String accessToken = (String) response.getBody().get("accessToken");

            System.out.println("Webhook: " + webhook);
System.out.println("Access Token: " + accessToken);

// 2. Test webhook before submitting SQL
Map<String, String> testBody = new HashMap<>();
testBody.put("name", body.get("name"));
testBody.put("email", body.get("email"));

String[] headerKeys = {"Authorization", "Authorization", "accessToken"};
String[] headerValues = {"Bearer " + accessToken, accessToken, accessToken};

boolean success = false;

for (int i = 0; i < headerKeys.length; i++) {
    try {
        HttpHeaders testHeaders = new HttpHeaders();
        testHeaders.setContentType(MediaType.APPLICATION_JSON);
        testHeaders.set(headerKeys[i], headerValues[i]);

        HttpEntity<Map<String, String>> testRequest = new HttpEntity<>(testBody, testHeaders);

        ResponseEntity<String> testResponse = restTemplate.exchange(
                webhook,
                HttpMethod.POST,
                testRequest,
                String.class
        );

        System.out.println("✅ Test Success with header [" + headerKeys[i] + "] : " + testResponse.getBody());
        success = true;
        break; // mil gaya, aur try karne ki need nahi
    } catch (Exception ex) {
        System.out.println("❌ Failed with header [" + headerKeys[i] + "] → " + ex.getMessage());
    }
}

if (!success) {
    System.err.println("❌ All header formats failed! Ab SQL submit nahi hoga.");
    return;
}



// 3. Decide SQL query based on regNo last digit
String regNo = body.get("regNo");

         
            int lastDigit = Character.getNumericValue(regNo.charAt(regNo.length() - 1));

            String finalQuery;
            if (lastDigit % 2 == 1) {
                // Odd → Question 1
                finalQuery = "SELECT p.AMOUNT AS SALARY, " +
                             "CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS NAME, " +
                             "TIMESTAMPDIFF(YEAR, e.DOB, CURDATE()) AS AGE, " +
                             "d.DEPARTMENT_NAME " +
                             "FROM PAYMENTS p " +
                             "JOIN EMPLOYEE e ON p.EMP_ID = e.EMP_ID " +
                             "JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID " +
                             "WHERE DAY(p.PAYMENT_TIME) <> 1 " +
                             "ORDER BY p.AMOUNT DESC " +
                             "LIMIT 1;";
            } else {
                // Even → Question 2
                finalQuery = "SELECT e.EMP_ID, e.FIRST_NAME, e.LAST_NAME, d.DEPARTMENT_NAME, " +
                             "(SELECT COUNT(*) FROM EMPLOYEE e2 " +
                             "WHERE e2.DEPARTMENT = e.DEPARTMENT " +
                             "AND TIMESTAMPDIFF(YEAR, e2.DOB, CURDATE()) < TIMESTAMPDIFF(YEAR, e.DOB, CURDATE())) " +
                             "AS YOUNGER_EMPLOYEES_COUNT " +
                             "FROM EMPLOYEE e " +
                             "JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID " +
                             "ORDER BY e.EMP_ID DESC;";
            }

            // 3. Submit SQL query to webhook
            Map<String, String> submitBody = new HashMap<>();
            submitBody.put("finalQuery", finalQuery);

            HttpHeaders submitHeaders = new HttpHeaders();
        submitHeaders.setContentType(MediaType.APPLICATION_JSON);
        // IMPORTANT: same as test → explicit add karna
        submitHeaders.set("Authorization",accessToken);

        HttpEntity<Map<String, String>> submitRequest = new HttpEntity<>(submitBody, submitHeaders);

        ResponseEntity<String> submitResponse = restTemplate.exchange(
                webhook,
                HttpMethod.POST,
                submitRequest,
                String.class
        );

        System.out.println("✅ Submission Response: " + submitResponse.getBody());


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
