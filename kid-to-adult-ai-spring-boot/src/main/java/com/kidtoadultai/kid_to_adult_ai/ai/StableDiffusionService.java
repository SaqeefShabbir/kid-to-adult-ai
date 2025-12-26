package com.kidtoadultai.kid_to_adult_ai.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class StableDiffusionService {

    @Value("${stable.diffusion.api.url:http://localhost:7860}")
    private String sdApiUrl;

    @Value("${stable.diffusion.model.path:./models/Stable-diffusion}")
    private String modelPath;

    private final RestTemplate restTemplate;
    private final Map<String, String> professionPrompts;

    public StableDiffusionService() {
        this.restTemplate = new RestTemplate();
        this.professionPrompts = initializeProfessionPrompts();
    }

    private Map<String, String> initializeProfessionPrompts() {
        Map<String, String> prompts = new HashMap<>();
        prompts.put("doctor", "professional doctor, white coat, stethoscope, hospital, medical setting, age {age}, detailed face, photorealistic");
        prompts.put("engineer", "engineer, safety helmet, construction site, blueprint, technical, age {age}, professional");
        prompts.put("teacher", "teacher, classroom, books, glasses, kind expression, age {age}, educator");
        prompts.put("astronaut", "astronaut, space suit, NASA, space station, heroic, age {age}, detailed");
        prompts.put("scientist", "scientist, lab coat, laboratory, test tubes, intelligent, age {age}");
        prompts.put("artist", "artist, painter, studio, paintbrush, creative, age {age}, artistic");
        prompts.put("pilot", "pilot, airline uniform, cockpit, professional, confident, age {age}");
        prompts.put("firefighter", "firefighter, fire suit, helmet, heroic, strong, age {age}");
        prompts.put("chef", "chef, kitchen uniform, restaurant, culinary, professional, age {age}");
        prompts.put("athlete", "athlete, sports uniform, stadium, athletic, fit, age {age}");
        return prompts;
    }

    /**
     * Generate adult version using txt2img (text to image)
     */
    public CompletableFuture<String> generateAdultVersionTxt2Img(
            String profession, int targetAge, String base64InitImage) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                String prompt = generatePrompt(profession, targetAge);

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("prompt", prompt);
                requestBody.put("negative_prompt", getNegativePrompt());
                requestBody.put("steps", 30);
                requestBody.put("width", 512);
                requestBody.put("height", 512);
                requestBody.put("cfg_scale", 7.5);
                requestBody.put("sampler_index", "DPM++ 2M Karras");
                requestBody.put("seed", -1);
                requestBody.put("batch_size", 1);
                requestBody.put("n_iter", 1);

                // Add init image for img2img-like effect
                if (base64InitImage != null) {
                    requestBody.put("init_images", Arrays.asList(base64InitImage));
                    requestBody.put("denoising_strength", 0.75);
                }

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

                ResponseEntity<Map> response = restTemplate.exchange(
                        sdApiUrl + "/sdapi/v1/txt2img",
                        HttpMethod.POST,
                        entity,
                        Map.class
                );

                if (response.getStatusCode() == HttpStatus.OK) {
                    return extractImageFromResponse(response.getBody());
                }

                throw new RuntimeException("Failed to generate image: " + response.getStatusCode());

            } catch (Exception e) {
                throw new RuntimeException("Stable Diffusion generation failed", e);
            }
        });
    }

    /**
     * Generate adult version using img2img (better for transformations)
     */
    public CompletableFuture<String> generateAdultVersionImg2Img(
            MultipartFile childImage, String profession, int targetAge) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Convert image to base64
                String base64Image = Base64.getEncoder().encodeToString(childImage.getBytes());
                String prompt = generatePrompt(profession, targetAge);

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("init_images", Arrays.asList(base64Image));
                requestBody.put("prompt", prompt);
                requestBody.put("negative_prompt", getNegativePrompt());
                requestBody.put("denoising_strength", 0.75); // How much to change the image
                requestBody.put("steps", 50);
                requestBody.put("width", 512);
                requestBody.put("height", 512);
                requestBody.put("cfg_scale", 7.5);
                requestBody.put("sampler_index", "DPM++ 2M Karras");
                requestBody.put("seed", -1);
                requestBody.put("batch_size", 1);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

                ResponseEntity<Map> response = restTemplate.exchange(
                        sdApiUrl + "/sdapi/v1/img2img",
                        HttpMethod.POST,
                        entity,
                        Map.class
                );

                if (response.getStatusCode() == HttpStatus.OK) {
                    return extractImageFromResponse(response.getBody());
                }

                throw new RuntimeException("Failed to generate image: " + response.getStatusCode());

            } catch (Exception e) {
                throw new RuntimeException("Stable Diffusion generation failed", e);
            }
        });
    }

    /**
     * Using ControlNet for better face preservation
     */
    public CompletableFuture<String> generateWithControlNet(
            MultipartFile childImage, String profession, int targetAge) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                String base64Image = Base64.getEncoder().encodeToString(childImage.getBytes());
                String prompt = generatePrompt(profession, targetAge);

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("prompt", prompt);
                requestBody.put("negative_prompt", getNegativePrompt());
                requestBody.put("steps", 30);
                requestBody.put("width", 512);
                requestBody.put("height", 512);
                requestBody.put("cfg_scale", 7.5);
                requestBody.put("sampler_index", "DPM++ 2M Karras");
                requestBody.put("seed", -1);
                requestBody.put("batch_size", 1);

                // ControlNet unit for face preservation
                Map<String, Object> controlNetUnit = new HashMap<>();
                controlNetUnit.put("input_image", base64Image);
                controlNetUnit.put("module", "depth"); // or "openpose" for pose preservation
                controlNetUnit.put("model", "control_v11f1p_sd15_depth [cfd03158]");
                controlNetUnit.put("weight", 1.0);
                controlNetUnit.put("guidance_start", 0.0);
                controlNetUnit.put("guidance_end", 1.0);

                Map<String, Object> alwaysonScripts = new HashMap<>();
                Map<String, Object> controlNetArgs = new HashMap<>();
                controlNetArgs.put("args", Arrays.asList(controlNetUnit));
                alwaysonScripts.put("ControlNet", controlNetArgs);

                requestBody.put("alwayson_scripts", alwaysonScripts);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

                ResponseEntity<Map> response = restTemplate.exchange(
                        sdApiUrl + "/sdapi/v1/txt2img",
                        HttpMethod.POST,
                        entity,
                        Map.class
                );

                if (response.getStatusCode() == HttpStatus.OK) {
                    return extractImageFromResponse(response.getBody());
                }

                throw new RuntimeException("Failed to generate image: " + response.getStatusCode());

            } catch (Exception e) {
                throw new RuntimeException("ControlNet generation failed", e);
            }
        });
    }

    /**
     * Extract base64 image from Stable Diffusion response
     */
    private String extractImageFromResponse(Map<String, Object> response) {
        try {
            List<String> images = (List<String>) response.get("images");
            if (images != null && !images.isEmpty()) {
                // Return as base64 data URL
                return "data:image/png;base64," + images.get(0);
            }

            throw new RuntimeException("No images in response");

        } catch (ClassCastException e) {
            System.err.println("Response structure: " + response);
            throw new RuntimeException("Invalid response format", e);
        }
    }

    /**
     * Generate prompt with profession and age
     */
    private String generatePrompt(String profession, int age) {
        String basePrompt = professionPrompts.getOrDefault(profession.toLowerCase(),
                "professional adult, office setting, age {age}, photorealistic");

        return basePrompt.replace("{age}", String.valueOf(age)) +
                ", highly detailed, sharp focus, studio lighting, masterpiece, best quality";
    }

    /**
     * Negative prompt to avoid common issues
     */
    private String getNegativePrompt() {
        return "deformed, blurry, bad anatomy, disfigured, poorly drawn face, " +
                "mutation, mutated, extra limb, ugly, poorly drawn hands, " +
                "missing limb, floating limbs, disconnected limbs, malformed hands, " +
                "out of focus, long neck, long body, unrealistic, doll, cartoon, " +
                "anime, 3d, cgi, render, sketch, painting, drawing";
    }

    /**
     * Get available models
     */
    public List<String> getAvailableModels() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    sdApiUrl + "/sdapi/v1/sd-models",
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                List<Map<String, Object>> models = (List<Map<String, Object>>) response.getBody();
                List<String> modelNames = new ArrayList<>();

                for (Map<String, Object> model : models) {
                    modelNames.add((String) model.get("model_name"));
                }

                return modelNames;
            }

        } catch (Exception e) {
            System.err.println("Failed to get available models");
            throw new RuntimeException("Invalid response", e);
        }

        return Arrays.asList("No models available");
    }

    /**
     * Change model
     */
    public boolean setModel(String modelName) {
        try {
            Map<String, String> request = new HashMap<>();
            request.put("sd_model_checkpoint", modelName);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    sdApiUrl + "/sdapi/v1/options",
                    entity,
                    String.class
            );

            return response.getStatusCode() == HttpStatus.OK;

        } catch (Exception e) {
            System.err.println("Failed to set model");
            throw new RuntimeException("Invalid response", e);
        }
    }

    /**
     * Get generation progress
     */
    public Map<String, Object> getProgress() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    sdApiUrl + "/sdapi/v1/progress",
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }

        } catch (Exception e) {
            System.err.println("Failed to get progress");
            throw new RuntimeException("Invalid response", e);
        }

        return new HashMap<>();
    }
}