package com.rag.repository;

import com.rag.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByUserIdAndDocumentIdOrderByCreateTimeDesc(Long userId, Long documentId);
    List<Conversation> findByUserIdOrderByCreateTimeDesc(Long userId);

    @Modifying @Transactional
    @Query("delete from Conversation c where c.documentId = :documentId")
    void deleteByDocumentId(Long documentId);

    @Query("select count(c) from Conversation c where c.userId = :userId")
    long countByUserId(Long userId);

    @Query("select count(c) from Conversation c where c.userId = :userId and c.createTime >= :since")
    long countByUserIdAndCreateTimeAfter(Long userId, LocalDateTime since);

    @Query("select c from Conversation c where c.userId = :userId order by c.createTime desc limit 5")
    List<Conversation> findTop5Recent(Long userId);

    @Query("select c.documentId, count(c) as cnt from Conversation c where c.userId = :userId group by c.documentId order by cnt desc limit 3")
    List<Object[]> findTopDocumentIds(Long userId);
}
