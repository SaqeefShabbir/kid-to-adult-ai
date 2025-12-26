package com.kidtoadultai.kid_to_adult_ai.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "job_status")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String jobId;

    @Column(nullable = false)
    private String status; // PROCESSING, COMPLETED, FAILED

    private String imageUrl;

    @Lob
    @Column(length = 10000)
    private String base64Image; // Store generated image directly if needed

    @Column(length = 1000)
    private String errorMessage;

    @Column(nullable = false)
    private String profession;

    @Column(nullable = false)
    private int targetAge;

    @Column(nullable = false)
    private String originalFilename;

    private String generatedFilename;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date startedAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date completedAt;

    private int processingTime; // in seconds

    @ElementCollection
    @CollectionTable(name = "job_metadata",
            joinColumns = @JoinColumn(name = "job_id"))
    @MapKeyColumn(name = "meta_key")
    @Column(name = "meta_value", length = 1000)
    private Map<String, String> metadata = new HashMap<>();

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
    }
}
