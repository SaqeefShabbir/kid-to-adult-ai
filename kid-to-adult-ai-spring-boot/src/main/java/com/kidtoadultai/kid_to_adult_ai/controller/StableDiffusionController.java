package com.kidtoadultai.kid_to_adult_ai.controller;

import com.kidtoadultai.kid_to_adult_ai.ai.StableDiffusionService;
import com.kidtoadultai.kid_to_adult_ai.dto.ImageResponse;
import com.kidtoadultai.kid_to_adult_ai.model.JobStatus;
import com.kidtoadultai.kid_to_adult_ai.service.JobTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/stable-diffusion")
@CrossOrigin(origins = "*")
public class StableDiffusionController {

    @Autowired
    private StableDiffusionService stableDiffusionService;

    @Autowired
    private JobTrackingService jobTrackingService;

    @Value("${image.upload.dir:./uploads}")
    private String uploadDir;

    private static final List<String> PROFESSIONS = Arrays.asList(
            "doctor", "engineer", "teacher", "astronaut",
            "scientist", "artist", "pilot", "firefighter",
            "chef", "athlete"
    );

    /**
     * Generate adult version using img2img
     */
    @PostMapping("/generate")
    public ResponseEntity<ImageResponse> generateAdultVersion(
            @RequestParam("image") MultipartFile file,
            @RequestParam("profession") String profession,
            @RequestParam(value = "age", defaultValue = "30") int targetAge) {

        try {
            if (!PROFESSIONS.contains(profession.toLowerCase())) {
                return ResponseEntity.badRequest()
                        .body(new ImageResponse("ERROR", "Invalid profession. Choose from: " + PROFESSIONS));
            }

            // Validate age
            if (targetAge < 20 || targetAge > 60) {
                return ResponseEntity.badRequest()
                        .body(new ImageResponse("ERROR", "Age must be between 20 and 60"));
            }

            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ImageResponse("ERROR", "Please upload an image"));
            }

            // Create job tracking entry
            JobStatus jobStatus = jobTrackingService.createJob(
                    profession, targetAge, file.getOriginalFilename()
            );
            String jobId = jobStatus.getJobId();

            // Start async generation
            CompletableFuture<String> futureResult = stableDiffusionService
                    .generateAdultVersionImg2Img(file, profession, targetAge);

            // Store future and process result
            processGenerationAsync(jobId, futureResult);

            // Return immediate response
            ImageResponse response = new ImageResponse();
            response.setJobId(jobId);
            response.setStatus("PROCESSING");
            response.setMessage("Image generation started. Use the jobId to check status.");
            response.setProfession(profession);
            response.setAge(targetAge);
            response.setCreatedAt(new Date());

            return ResponseEntity.accepted()
                    .header("Location", "/api/stable-diffusion/status/" + jobId)
                    .body(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ImageResponse("ERROR", e.getMessage()));
        }
    }

    /**
     * Generate using ControlNet (better face preservation)
     */
    @PostMapping("/generate-with-controlnet")
    public ResponseEntity<ImageResponse> generateWithControlNet(
            @RequestParam("image") MultipartFile file,
            @RequestParam("profession") String profession,
            @RequestParam(value = "age", defaultValue = "30") int targetAge) {

        try {
            // Create job tracking entry
            JobStatus jobStatus = jobTrackingService.createJob(
                    profession, targetAge, file.getOriginalFilename()
            );
            String jobId = jobStatus.getJobId();

            CompletableFuture<String> futureResult = stableDiffusionService
                    .generateWithControlNet(file, profession, targetAge);

            processGenerationAsync(jobId, futureResult);

            // Return immediate response
            ImageResponse response = new ImageResponse();
            response.setJobId(jobId);
            response.setStatus("PROCESSING");
            response.setMessage("Image generation started. Use the jobId to check status.");
            response.setProfession(profession);
            response.setAge(targetAge);
            response.setCreatedAt(new Date());

            return ResponseEntity.accepted()
                    .header("Location", "/api/stable-diffusion/status/" + jobId)
                    .body(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ImageResponse("ERROR", e.getMessage()));
        }
    }

    /**
     * Get available models
     */
    @GetMapping("/models")
    public ResponseEntity<List<String>> getModels() {
        List<String> models = stableDiffusionService.getAvailableModels();
        return ResponseEntity.ok(models);
    }

    /**
     * Set active model
     */
    @PostMapping("/models/{modelName}")
    public ResponseEntity<Map<String, String>> setModel(@PathVariable String modelName) {
        boolean success = stableDiffusionService.setModel(modelName);

        Map<String, String> response = new HashMap<>();
        if (success) {
            response.put("status", "SUCCESS");
            response.put("message", "Model changed to: " + modelName);
        } else {
            response.put("status", "ERROR");
            response.put("message", "Failed to change model");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get generation progress
     */
    @GetMapping("/progress")
    public ResponseEntity<Map<String, Object>> getProgress() {
        Map<String, Object> progress = stableDiffusionService.getProgress();
        return ResponseEntity.ok(progress);
    }

    /**
     * Process generation asynchronously
     */
    private void processGenerationAsync(String jobId, CompletableFuture<String> future) {
        CompletableFuture.runAsync(() -> {
            try {
                String base64Image = future.get(); // Wait for completion

                // Save to database or storage
                String imageUrl = saveGeneratedImage(base64Image, jobId);

                // Update job status (you need to implement job tracking)
                jobTrackingService.updateJobStatus(jobId, "COMPLETED", imageUrl);

            } catch (Exception e) {
                jobTrackingService.updateJobStatus(jobId, "FAILED", "Generation failed: " + e.getMessage());
            }
        });
    }

    private String saveGeneratedImage(String base64Image, String jobId) {
        try {
            // Remove data URL prefix
            String base64Data = base64Image.substring(base64Image.indexOf(",") + 1);
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);

            // Save to file
            String fileName = "generated_" + jobId + ".png";
            java.nio.file.Path filePath = java.nio.file.Paths.get(uploadDir, fileName);
            java.nio.file.Files.createDirectories(filePath.getParent());
            java.nio.file.Files.write(filePath, imageBytes);

            return "/api/images/" + fileName;

        } catch (Exception e) {
            throw new RuntimeException("Failed to save image", e);
        }
    }

    @GetMapping("/status/{jobId}")
    public ResponseEntity<ImageResponse> getStatus(@PathVariable String jobId) {
        JobStatus jobStatus = jobTrackingService.getJobStatus(jobId);

        if (jobStatus == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ImageResponse("NOT_FOUND", "Job not found: " + jobId));
        }

        ImageResponse response = new ImageResponse();
        response.setJobId(jobId);
        response.setStatus(jobStatus.getStatus());
        response.setProfession(jobStatus.getProfession());
        response.setAge(jobStatus.getTargetAge());
        response.setCreatedAt(jobStatus.getCreatedAt());

        // Add appropriate message based on status
        switch (jobStatus.getStatus()) {
            case "PROCESSING":
                response.setMessage("Image is being generated. Please wait...");
                response.setProgress(50); // Estimated progress
                break;
            case "COMPLETED":
                response.setMessage("Image generation completed successfully!");
                response.setImageUrl(jobStatus.getImageUrl());
                response.setProgress(100);
                if (jobStatus.getCompletedAt() != null) {
                    response.setCompletedAt(jobStatus.getCompletedAt());
                }
                break;
            case "FAILED":
                response.setMessage("Image generation failed: " + jobStatus.getErrorMessage());
                response.setProgress(0);
                break;
            default:
                response.setMessage("Unknown status");
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/jobs")
    public ResponseEntity<List<JobStatus>> getAllJobs(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String profession) {

        List<JobStatus> jobs;

        if (status != null && profession != null) {
            // Filter by both status and profession
            List<JobStatus> statusJobs = jobTrackingService.getJobsByStatus(status);
            jobs = new ArrayList<>();
            for (JobStatus job : statusJobs) {
                if (job.getProfession().equalsIgnoreCase(profession)) {
                    jobs.add(job);
                }
            }
        } else if (status != null) {
            jobs = jobTrackingService.getJobsByStatus(status);
        } else if (profession != null) {
            jobs = jobTrackingService.getJobsByProfession(profession);
        } else {
            jobs = jobTrackingService.getAllJobs();
        }

        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = jobTrackingService.getStatistics();
        return ResponseEntity.ok(stats);
    }

    @DeleteMapping("/jobs/{jobId}")
    public ResponseEntity<Map<String, String>> deleteJob(@PathVariable String jobId) throws IOException {
        JobStatus jobStatus = jobTrackingService.getJobStatus(jobId);

        if (jobStatus == null) {
            return ResponseEntity.notFound().build();
        }

        // Delete generated image
        if (jobStatus.getGeneratedFilename() != null) {
            Path generatedPath = Paths.get(uploadDir, jobStatus.getGeneratedFilename());
            if (Files.exists(generatedPath)) {
                Files.delete(generatedPath);
                System.out.println("Deleted generated file: " + generatedPath);
            }
        }

        Map<String, String> metadata = new HashMap<>();
        metadata.put("deletedAt", new Date().toString());
        metadata.put("deletedBy", "system"); // In real app, get from authentication

        jobTrackingService.updateJobStatus(jobId, "DELETED",
                jobStatus.getImageUrl(), jobStatus.getGeneratedFilename(), metadata);

        Map<String, String> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Job deleted successfully");

        return ResponseEntity.ok(response);
    }
}