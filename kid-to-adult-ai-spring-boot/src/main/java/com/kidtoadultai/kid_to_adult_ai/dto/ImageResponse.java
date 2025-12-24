package com.kidtoadultai.kid_to_adult_ai.dto;

public class ImageResponse {
    private String jobId;
    private String status;
    private String imageUrl;
    private String message;

    // Default constructor
    public ImageResponse() {}

    // Constructor for error responses
    public ImageResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }

    // Constructor with all fields
    public ImageResponse(String jobId, String status, String imageUrl, String message) {
        this.jobId = jobId;
        this.status = status;
        this.imageUrl = imageUrl;
        this.message = message;
    }

    // Getters and setters
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}