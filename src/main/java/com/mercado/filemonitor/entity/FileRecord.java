package com.mercado.filemonitor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "last_modified")
    private LocalDateTime lastModified;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ProcessingStatus status;

    @Column(name = "output_path")
    private String outputPath;

    @Column(name = "records_count")
    private Integer recordsCount;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    public enum ProcessingStatus {
        PENDING, PROCESSING, COMPLETED, ERROR
    }
}