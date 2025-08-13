package com.mercado.filemonitor.repository;

import com.mercado.filemonitor.entity.FileRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileRecordRepository extends JpaRepository<FileRecord, Long> {

    Optional<FileRecord> findByFilePathAndLastModified(String filePath, LocalDateTime lastModified);

    List<FileRecord> findByStatus(FileRecord.ProcessingStatus status);

    @Query("SELECT fr FROM FileRecord fr WHERE fr.processedAt BETWEEN :startDate AND :endDate")
    List<FileRecord> findByProcessedAtBetween(@Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(fr) FROM FileRecord fr WHERE fr.status = :status")
    long countByStatus(@Param("status") FileRecord.ProcessingStatus status);
}