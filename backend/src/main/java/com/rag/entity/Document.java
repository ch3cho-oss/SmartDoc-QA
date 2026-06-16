package com.rag.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "document")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_path", nullable = false, length = 512)
    private String filePath;

    @Column(name = "total_chunks")
    @Builder.Default
    private Integer totalChunks = 0;

    @Column(name = "file_size")
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DocStatus status = DocStatus.PROCESSING;

    @Column(name = "upload_time")
    private LocalDateTime uploadTime;

    @PrePersist
    public void prePersist() {
        this.uploadTime = LocalDateTime.now();
    }

    public enum DocStatus {
        PROCESSING, COMPLETED, FAILED
    }
}
