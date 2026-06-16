package com.rag.repository;

import com.rag.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByConversationIdOrderByCreateTimeAsc(Long conversationId);
    List<Message> findTop6ByConversationIdOrderByCreateTimeDesc(Long conversationId);

    @Modifying @Transactional
    @Query("delete from Message m where m.conversationId = :conversationId")
    void deleteByConversationId(Long conversationId);

    @Query("select count(m) from Message m, Conversation c where m.conversationId = c.id and c.userId = :userId")
    long countByUserId(Long userId);

    @Query("select count(m) from Message m, Conversation c where m.conversationId = c.id and c.userId = :userId and m.createTime >= :since")
    long countByUserIdAndCreateTimeAfter(Long userId, LocalDateTime since);
}
