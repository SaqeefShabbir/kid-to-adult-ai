package com.kidtoadultai.kid_to_adult_ai.service;

import com.kidtoadultai.kid_to_adult_ai.model.JobStatus;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JobTrackingService {
    private final Map<String, JobStatus> jobStatusMap = new ConcurrentHashMap<>();

    public void createJob(String jobId) {
        JobStatus jobStatus = new JobStatus();
        jobStatus.setJobId(jobId);
        jobStatus.setStatus("PROCESSING");
        jobStatus.setCreatedAt(new Date());
        jobStatusMap.put(jobId, jobStatus);
    }

    public void updateJobStatus(String jobId, String status, String imageUrl) {
        JobStatus jobStatus = jobStatusMap.get(jobId);
        if (jobStatus != null) {
            jobStatus.setStatus(status);
            jobStatus.setImageUrl(imageUrl);
            jobStatus.setCompletedAt(new Date());
        }
    }

    public JobStatus getJobStatus(String jobId) {
        return jobStatusMap.get(jobId);
    }

    public void cleanupOldJobs() {
        // Clean up jobs older than 24 hours
        Date yesterday = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        jobStatusMap.entrySet().removeIf(entry ->
                entry.getValue().getCreatedAt().before(yesterday));
    }
}