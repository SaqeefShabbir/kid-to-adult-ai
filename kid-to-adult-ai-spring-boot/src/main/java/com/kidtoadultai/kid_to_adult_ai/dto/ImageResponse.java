package com.kidtoadultai.kid_to_adult_ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Date;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImageResponse {

    private String jobId;
    private String status; // PROCESSING, COMPLETED, FAILED, NOT_FOUND
    private String message;
    private String imageUrl;
    private String profession;
    private int age;
    private int progress; // 0-100
    private Date createdAt;
    private Date completedAt;
    private Map<String, Object> metadata;

    // Additional fields for error details
    private String errorCode;
    private String errorDetails;

    // Constructor for success responses
    public ImageResponse(String jobId, String status, String message, String imageUrl) {
        this.jobId = jobId;
        this.status = status;
        this.message = message;
        this.imageUrl = imageUrl;
        this.createdAt = new Date();
    }

    // Constructor for error responses
    public ImageResponse(String status, String message) {
        this.status = status;
        this.message = message;
        this.createdAt = new Date();
    }

    // Constructor with progress
    public ImageResponse(String jobId, String status, String message, int progress) {
        this.jobId = jobId;
        this.status = status;
        this.message = message;
        this.progress = progress;
        this.createdAt = new Date();
    }
}