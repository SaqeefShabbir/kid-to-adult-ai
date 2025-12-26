package com.kidtoadultai.kid_to_adult_ai.repository;

import com.kidtoadultai.kid_to_adult_ai.model.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface JobStatusRepository extends JpaRepository<JobStatus, String> {

    List<JobStatus> findAllByOrderByCreatedAtDesc();

    List<JobStatus> findByStatusOrderByCreatedAtDesc(String status);

    List<JobStatus> findByProfessionOrderByCreatedAtDesc(String profession);

    List<JobStatus> findByCreatedAtBefore(Date date);

    long countByStatus(String status);

    @Query("SELECT AVG(j.processingTime) FROM JobStatus j WHERE j.status = 'COMPLETED' AND j.processingTime > 0")
    Double getAverageProcessingTime();

    @Query("SELECT j.profession, COUNT(j) as count FROM JobStatus j GROUP BY j.profession ORDER BY count DESC")
    List<Object[]> getProfessionStatistics();

    @Query("SELECT j FROM JobStatus j WHERE j.createdAt >= :startDate AND j.createdAt <= :endDate")
    List<JobStatus> findByDateRange(@Param("startDate") Date startDate,
                                    @Param("endDate") Date endDate);

    @Query("SELECT j FROM JobStatus j WHERE j.imageUrl IS NOT NULL ORDER BY j.createdAt DESC")
    List<JobStatus> findSuccessfulJobs();
}