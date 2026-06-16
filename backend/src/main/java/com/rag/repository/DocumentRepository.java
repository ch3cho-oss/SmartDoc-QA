package com.rag.repository;

import com.rag.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByUserIdOrderByUploadTimeDesc(Long userId);
    List<Document> findByUserIdAndStatusOrderByUploadTimeDesc(Long userId, Document.DocStatus status);

    @Query("select count(d) from Document d where d.userId = :userId")
    long countByUserId(Long userId);

    @Query("select count(d) from Document d where d.userId = :userId and d.uploadTime >= :since")
    long countByUserIdAndUploadTimeAfter(Long userId, LocalDateTime since);

    @Query("select count(d) from Document d where d.userId = :userId and d.status = :status")
    long countByUserIdAndStatus(Long userId, Document.DocStatus status);

    @Query("select coalesce(sum(d.fileSize), 0) from Document d where d.userId = :userId")
    long totalSizeByUserId(Long userId);

    @Query("select d from Document d where d.userId = :userId order by d.uploadTime desc limit 5")
    List<Document> findTop5Recent(Long userId);
}
