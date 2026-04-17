package com.pm.api_gateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pm.api_gateway.dto.PatientInsightsDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;

import java.util.Map;

@Service
public class AiService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AiService.class);

    @Value("${ai.openai.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public PatientInsightsDTO getInsights(String patientData) {
        try {
//            String url = "https://api.openai.com/v1/chat/completions";
//
//            Map<String, Object> request = Map.of(
//                    "model", "gpt-4.1",
//                    "temperature", 0.3,
//                    "messages", new Object[]{
//                            Map.of("role", "system",
//                                    "content", "You are a medical assistant. Return ONLY valid JSON."),
//                            Map.of("role", "user",
//                                    "content", buildPrompt(patientData))
//                    }
//

//            String url = "http://localhost:11434/api/generate";
            String url = "http://host.docker.internal:11434/api/generate";
            Map<String, Object> request = Map.of(
                    "model", "llama3",
                    "prompt", buildPrompt(patientData),
                    "stream", false
            );
            log.info("OpenAI Request: " + request);
            var headers = new org.springframework.http.HttpHeaders();
//            headers.set("Authorization", "Bearer " + apiKey); require for paid plans
            headers.set("Content-Type", "application/json");

            var entity = new org.springframework.http.HttpEntity<>(request, headers);

            var response = restTemplate.postForObject(url, entity, Map.class);
            log.info("OpenAI Response: " + response);
//            String content = ((Map)((Map)((java.util.List)response.get("choices")).get(0))
//                    .get("message")).get("content").toString();
//            assert response != null;
            String content = response.get("response").toString();
            // 🔥 Convert JSON string → DTO
            return objectMapper.readValue(content, PatientInsightsDTO.class);

        } catch (Exception e) {
            // fallback
            log.info("Error generating insights: " + e.getMessage());
            PatientInsightsDTO fallback = new PatientInsightsDTO();
            fallback.setRiskLevel("UNKNOWN");
            fallback.setSummary("Unable to generate insights");
            return fallback;
        }
    }

    private String buildPrompt(String patientData) {
        log.info("Patient Data: " + patientData + "");
        return """
                Analyze the patient and return ONLY JSON in this format:
                {
                  "riskLevel": "LOW | MEDIUM | HIGH",
                  "summary": "short explanation"
                }
                
                Do not add extra text.
                Do not make assumptions beyond given data.
                
                Patient:
                """ + patientData;
    }
}