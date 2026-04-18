package com.goviconnect.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Controller
public class DiseaseController {

    private final WebClient webClient;

    public DiseaseController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8000").build();
    }

    @GetMapping("/disease-predictor")
    public String diseasePredictorPage(Model model) {
        model.addAttribute("pageTitle", "AI Disease Predictor | GOVI CONNECT");
        return "disease-predictor/index";
    }

    @PostMapping("/api/disease-predict")
    @ResponseBody
    public Map<String, Object> predictDisease(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("fileName", file.getOriginalFilename());

        try {
            org.springframework.http.client.MultipartBodyBuilder builder = new org.springframework.http.client.MultipartBodyBuilder();
            builder.part("file", file.getResource());

            Map<String, Object> aiResponse = webClient.post()
                    .uri("/predict")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (aiResponse != null && "success".equals(aiResponse.get("status"))) {
                // Raw disease key (e.g. "normal", "blast") for UI logic
                response.put("disease_key",      aiResponse.get("disease"));
                // Human-readable name (e.g. "Healthy Plant", "Rice Blast")
                response.put("disease",          aiResponse.get("display_name"));
                response.put("confidence",       aiResponse.get("confidence"));
                response.put("severity",         aiResponse.get("severity"));
                response.put("pathogen",         aiResponse.get("pathogen"));
                response.put("paddy_confidence", aiResponse.get("paddy_confidence"));
                response.put("image_status",     aiResponse.get("image_status"));
                response.put("warning",          aiResponse.get("warning"));
                response.put("symptoms",         aiResponse.get("symptoms"));
                response.put("treatment",        aiResponse.get("treatment"));
                response.put("prevention",       aiResponse.get("prevention"));
                response.put("all_scores",       aiResponse.get("all_scores"));

                // Build a solution string from treatment list for backward-compat
                String solution = "";
                Object treatmentObj = aiResponse.get("treatment");
                if (treatmentObj instanceof List) {
                    solution = String.join(" ", (List<String>) treatmentObj);
                }
                response.put("solution", solution.isEmpty() ? "Consult an agricultural officer for detailed advice." : solution);
            } else {
                response.put("disease",     "Analysis Failed");
                response.put("disease_key", "error");
                response.put("solution",    "The AI service could not analyze the image.");
                response.put("confidence",  0);
            }

        } catch (WebClientResponseException e) {
            response.put("disease",     "Validation Failed");
            response.put("disease_key", "rejected");
            response.put("confidence",  0);
            try {
                String responseBody = e.getResponseBodyAsString();
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> errResponse = mapper.readValue(responseBody,
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>(){});
                response.put("solution", errResponse.getOrDefault("detail", e.getMessage()));
            } catch (Exception ex) {
                response.put("solution", e.getMessage());
            }
        } catch (Exception e) {
            response.put("disease",     "Service Unavailable");
            response.put("disease_key", "error");
            response.put("solution",    "The AI Prediction Engine is currently offline. Please ensure the Python backend is running on port 8000.");
            response.put("confidence",  0);
        }

        return response;
    }
}
