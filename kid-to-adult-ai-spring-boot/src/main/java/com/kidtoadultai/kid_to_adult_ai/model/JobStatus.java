package com.kidtoadultai.kid_to_adult_ai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobStatus {
    private String jobId;
    private String status;  // PROCESSING, COMPLETED, FAILED
    private String imageUrl;
    private Date createdAt;
    private Date completedAt;
}

