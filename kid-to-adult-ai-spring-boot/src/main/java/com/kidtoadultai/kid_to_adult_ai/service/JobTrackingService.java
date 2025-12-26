package com.kidtoadultai.kid_to_adult_ai.service;

import com.kidtoadultai.kid_to_adult_ai.model.JobStatus;
import com.kidtoadultai.kid_to_adult_ai.repository.JobStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class JobTrackingService {

    @Autowired
    private JobStatusRepository jobStatusRepository;

    // In-memory cache for quick access (optional)
    private final ConcurrentHashMap<String, JobStatus> jobCache = new ConcurrentHashMap<>();

    // Cache expiration time (1 hour)
    private static final long CACHE_EXPIRY_MS = TimeUnit.HOURS.toMillis(1);

    /**
     * Create a new job
     */
    @Transactional
    public JobStatus createJob(String profession, int targetAge, String originalFilename) {
        String jobId = UUID.randomUUID().toString();

        JobStatus jobStatus = new JobStatus();
        jobStatus.setJobId(jobId);
        jobStatus.setStatus("PROCESSING");
        jobStatus.setProfession(profession);
        jobStatus.setTargetAge(targetAge);
        jobStatus.setOriginalFilename(originalFilename);
        jobStatus.setStartedAt(new Date());

        JobStatus savedJob = jobStatusRepository.save(jobStatus);
        jobCache.put(jobId, savedJob);

        return savedJob;
    }

    /**
     * Update job status when completed successfully
     */
    @Transactional
    public void updateJobStatus(String jobId, String status, String imageUrl,
                                String generatedFilename, Map<String, String> metadata) {

        Optional<JobStatus> optionalJob = jobStatusRepository.findById(jobId);

        if (optionalJob.isPresent()) {
            JobStatus jobStatus = optionalJob.get();
            jobStatus.setStatus(status);
            jobStatus.setImageUrl(imageUrl);
            jobStatus.setGeneratedFilename(generatedFilename);
            jobStatus.setCompletedAt(new Date());

            if (jobStatus.getStartedAt() != null) {
                long processingTime = (jobStatus.getCompletedAt().getTime() -
                        jobStatus.getStartedAt().getTime()) / 1000;
                jobStatus.setProcessingTime((int) processingTime);
            }

            if (metadata != null) {
                jobStatus.getMetadata().putAll(metadata);
            }

            JobStatus updatedJob = jobStatusRepository.save(jobStatus);
            jobCache.put(jobId, updatedJob);
        }
    }

    /**
     * Update job status when failed
     */
    @Transactional
    public void updateJobStatus(String jobId, String status, String errorMessage) {
        Optional<JobStatus> optionalJob = jobStatusRepository.findById(jobId);

        if (optionalJob.isPresent()) {
            JobStatus jobStatus = optionalJob.get();
            jobStatus.setStatus(status);
            jobStatus.setErrorMessage(errorMessage);
            jobStatus.setCompletedAt(new Date());

            JobStatus updatedJob = jobStatusRepository.save(jobStatus);
            jobCache.put(jobId, updatedJob);
        }
    }

    /**
     * Get job status by ID
     */
    public JobStatus getJobStatus(String jobId) {
        // Check cache first
        JobStatus cachedJob = jobCache.get(jobId);
        if (cachedJob != null) {
            return cachedJob;
        }

        // If not in cache, get from database
        Optional<JobStatus> optionalJob = jobStatusRepository.findById(jobId);

        if (optionalJob.isPresent()) {
            JobStatus job = optionalJob.get();
            jobCache.put(jobId, job);
            return job;
        }

        return null;
    }

    /**
     * Get all jobs (for admin purposes)
     */
    public List<JobStatus> getAllJobs() {
        return jobStatusRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Get jobs by status
     */
    public List<JobStatus> getJobsByStatus(String status) {
        return jobStatusRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    /**
     * Get jobs by profession
     */
    public List<JobStatus> getJobsByProfession(String profession) {
        return jobStatusRepository.findByProfessionOrderByCreatedAtDesc(profession);
    }

    /**
     * Delete old jobs (older than 30 days)
     */
    @Transactional
    @Scheduled(cron = "0 0 2 * * ?") // Run daily at 2 AM
    public void cleanupOldJobs() {
        Date thirtyDaysAgo = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30));

        List<JobStatus> oldJobs = jobStatusRepository.findByCreatedAtBefore(thirtyDaysAgo);

        if (!oldJobs.isEmpty()) {
            jobStatusRepository.deleteAll(oldJobs);

            // Also remove from cache
            oldJobs.forEach(job -> jobCache.remove(job.getJobId()));

            System.out.println("Cleaned up " + oldJobs.size() + " old jobs");
        }
    }

    /**
     * Clean expired cache entries
     */
    @Scheduled(fixedRate = 3600000) // Run every hour
    public void cleanExpiredCache() {
        long currentTime = System.currentTimeMillis();

        jobCache.entrySet().removeIf(entry -> {
            JobStatus job = entry.getValue();
            if (job.getCompletedAt() != null) {
                long timeSinceCompletion = currentTime - job.getCompletedAt().getTime();
                return timeSinceCompletion > CACHE_EXPIRY_MS;
            }
            return false;
        });
    }

    /**
     * Get statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long totalJobs = jobStatusRepository.count();
        long completedJobs = jobStatusRepository.countByStatus("COMPLETED");
        long failedJobs = jobStatusRepository.countByStatus("FAILED");
        long processingJobs = jobStatusRepository.countByStatus("PROCESSING");

        stats.put("totalJobs", totalJobs);
        stats.put("completedJobs", completedJobs);
        stats.put("failedJobs", failedJobs);
        stats.put("processingJobs", processingJobs);

        if (completedJobs > 0) {
            Double avgProcessingTime = jobStatusRepository.getAverageProcessingTime();
            stats.put("averageProcessingTime", avgProcessingTime != null ?
                    String.format("%.2f seconds", avgProcessingTime) : "N/A");
        }

        // Get top professions
        List<Object[]> professionStats = jobStatusRepository.getProfessionStatistics();
        stats.put("professionStats", professionStats);

        return stats;
    }
}