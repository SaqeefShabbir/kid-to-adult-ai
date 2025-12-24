package com.kidtoadultai.kid_to_adult_ai.controller;

import com.kidtoadultai.kid_to_adult_ai.ai.AIService;
import com.kidtoadultai.kid_to_adult_ai.dto.ImageResponse;
import com.kidtoadultai.kid_to_adult_ai.model.JobStatus;
import com.kidtoadultai.kid_to_adult_ai.service.JobTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.concurrent.CompletableFuture;
import java.util.*;

@RestController
@RequestMapping("/api/images")
@CrossOrigin(origins = "*")
public class ImageController {

    @Autowired
    private AIService aiService;

    @Autowired
    private JobTrackingService jobTrackingService;

    private static final List<String> PROFESSIONS = Arrays.asList(
            "doctor", "engineer", "teacher", "astronaut",
            "scientist", "artist", "pilot", "firefighter",
            "chef", "athlete"
    );

    @GetMapping("/professions")
    public ResponseEntity<List<String>> getProfessions() {
        return ResponseEntity.ok(PROFESSIONS);
    }

    @PostMapping("/upload")
    public ResponseEntity<ImageResponse> uploadImage(
            @RequestParam("image") MultipartFile file,
            @RequestParam("profession") String profession,
            @RequestParam(value = "age", defaultValue = "30") int targetAge) {

        try {
            // Generate unique job ID
            String jobId = UUID.randomUUID().toString();

            // Create job entry
            jobTrackingService.createJob(jobId);

            // Process image asynchronously
            processImageAsync(jobId, file, profession, targetAge);

            // Return immediate response with job ID
            ImageResponse response = new ImageResponse();
            response.setJobId(jobId);
            response.setStatus("PROCESSING");
            response.setMessage("Image generation started. Use jobId to check status.");

            return ResponseEntity.accepted().body(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ImageResponse("ERROR", "Failed to process image"));
        }
    }

    @GetMapping("/status/{jobId}")
    public ResponseEntity<ImageResponse> getStatus(@PathVariable String jobId) {
        JobStatus jobStatus = jobTrackingService.getJobStatus(jobId);

        if (jobStatus == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ImageResponse("NOT_FOUND", "Job not found"));
        }

        ImageResponse response = new ImageResponse();
        response.setJobId(jobId);
        response.setStatus(jobStatus.getStatus());

        // Only include image URL if job is completed
        if ("COMPLETED".equals(jobStatus.getStatus()) && jobStatus.getImageUrl() != null) {
            response.setImageUrl(jobStatus.getImageUrl());
            response.setMessage("Image generation completed successfully");
        } else if ("FAILED".equals(jobStatus.getStatus())) {
            response.setMessage("Image generation failed");
        } else {
            response.setMessage("Image is still being processed");
        }

        return ResponseEntity.ok(response);
    }

    private void processImageAsync(String jobId, MultipartFile file,
                                   String profession, int targetAge) {
        CompletableFuture.runAsync(() -> {
            try {
                // Convert image to base64
                String base64Image = Base64.getEncoder()
                        .encodeToString(file.getBytes());

                // Call AI service (this is the actual AI generation)
                String generatedImageUrl = aiService.generateAdultImage(
                        base64Image, profession, targetAge
                ).get();  // Wait for completion

                // Update job status with actual image URL
                jobTrackingService.updateJobStatus(jobId, "COMPLETED", generatedImageUrl);

            } catch (Exception e) {
                // Update job status as failed
                jobTrackingService.updateJobStatus(jobId, "FAILED", null);
                e.printStackTrace();
            }
        });
    }
}