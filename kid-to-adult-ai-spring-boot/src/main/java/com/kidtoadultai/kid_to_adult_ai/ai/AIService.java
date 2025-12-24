package com.kidtoadultai.kid_to_adult_ai.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
        import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class AIService {

    @Value("${replicate.api.key}")
    private String replicateApiKey;

    @Value("${stablediffusion.api.key}")
    private String stableDiffusionKey;

    private final Map<String, String> professionPrompts = new HashMap<>();

    public AIService() {
        initializePrompts();
    }

    private void initializePrompts() {
        professionPrompts.put("doctor", "professional doctor in white coat, medical setting, mature face, confident expression");
        professionPrompts.put("engineer", "engineer wearing safety helmet, technical background, focused expression, professional attire");
        professionPrompts.put("teacher", "teacher in classroom, holding books, warm smile, professional educator");
        professionPrompts.put("astronaut", "astronaut in space suit, space background, heroic pose");
        professionPrompts.put("scientist", "scientist in lab coat, laboratory setting, holding test tube, intelligent look");
        professionPrompts.put("artist", "artist in studio, holding paintbrush, creative expression, artistic background");
        professionPrompts.put("pilot", "airline pilot in uniform, cockpit background, confident and professional");
        professionPrompts.put("firefighter", "firefighter in full gear, fire station background, heroic and strong");
        professionPrompts.put("chef", "professional chef in kitchen, culinary setting, holding cooking utensils");
        professionPrompts.put("athlete", "professional athlete in sportswear, stadium background, athletic build");
    }

    @Async
    public CompletableFuture<String> generateAdultImage(String base64Image, String profession, int targetAge) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Prepare the prompt
                String prompt = professionPrompts.getOrDefault(profession.toLowerCase(),
                        "professional adult, office setting, mature appearance") +
                        ", age " + targetAge + ", realistic face, high quality, detailed";

                // Using Replicate API (or alternative)
                return callReplicateAPI(base64Image, prompt);

            } catch (Exception e) {
                throw new RuntimeException("AI generation failed", e);
            }
        });
    }

    private String callReplicateAPI(String base64Image, String prompt) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Token " + replicateApiKey);

        Map<String, Object> requestBody = new HashMap<>();
//       requestBody.put("version", "stability-ai/sdxl:39ed52f2a78e934b3ba6e2a89f5b1c712de7dfea535525255b1aa35c5565e08b");
        requestBody.put("version", "google/imagen-4");

        Map<String, Object> input = new HashMap<>();
        input.put("prompt", prompt);
        input.put("image", base64Image);
        input.put("num_outputs", 1);
        input.put("image_dimensions", "768x768");
        input.put("num_inference_steps", 50);

        requestBody.put("input", input);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                "https://api.replicate.com/v1/predictions",
                HttpMethod.POST,
                entity,
                Map.class
        );

        if (response.getStatusCode() == HttpStatus.CREATED) {
            Map<String, Object> responseBody = response.getBody();
            return (String) ((Map<String, Object>) responseBody.get("output")).get("image_url");
        }

        throw new RuntimeException("Failed to generate image");
    }
}
