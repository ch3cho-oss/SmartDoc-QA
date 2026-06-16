package com.rag.service;

import com.rag.entity.Conversation;
import com.rag.entity.Message;
import com.rag.repository.ConversationRepository;
import com.rag.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public Conversation createConversation(Long userId, Long documentId, String title) {
        Conversation conversation = Conversation.builder()
                .userId(userId)
                .documentId(documentId)
                .title(title != null ? title : "新对话")
                .build();
        return conversationRepository.save(conversation);
    }

    public List<Conversation> getConversations(Long userId, Long documentId) {
        if (documentId != null) {
            return conversationRepository.findByUserIdAndDocumentIdOrderByCreateTimeDesc(userId, documentId);
        }
        return conversationRepository.findByUserIdOrderByCreateTimeDesc(userId);
    }

    public List<Message> getMessages(Long conversationId) {
        return messageRepository.findByConversationIdOrderByCreateTimeAsc(conversationId);
    }

    public Message saveMessage(Long conversationId, Message.MessageRole role, String content, String references) {
        Message message = Message.builder()
                .conversationId(conversationId)
                .role(role)
                .content(content)
                .references(references)
                .build();
        return messageRepository.save(message);
    }

    public List<Message> getRecentMessages(Long conversationId) {
        return messageRepository.findTop6ByConversationIdOrderByCreateTimeDesc(conversationId);
    }

    public void deleteConversation(Long conversationId) {
        messageRepository.deleteByConversationId(conversationId);
        conversationRepository.deleteById(conversationId);
    }

    /** 清空对话的所有消息（保留对话容器） */
    public void clearMessages(Long conversationId) {
        messageRepository.deleteByConversationId(conversationId);
    }

    public void updateTitle(Long conversationId, String title) {
        conversationRepository.findById(conversationId).ifPresent(conv -> {
            conv.setTitle(title);
            conversationRepository.save(conv);
        });
    }

    /**
     * 首条消息自动命名：截取问题前30个字符
     */
    public void autoTitleFirstMessage(Long conversationId, String question) {
        conversationRepository.findById(conversationId).ifPresent(conv -> {
            if ("新对话".equals(conv.getTitle())) {
                String title = question.length() > 30 ? question.substring(0, 30) + "..." : question;
                conv.setTitle(title);
                conversationRepository.save(conv);
            }
        });
    }
}
