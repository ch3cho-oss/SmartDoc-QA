package com.rag.service;

import com.rag.dto.LoginRequest;
import com.rag.dto.RegisterRequest;
import com.rag.entity.User;
import com.rag.repository.ConversationRepository;
import com.rag.repository.DocumentRepository;
import com.rag.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final ConversationRepository conversationRepository;

    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        User user = User.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .build();
        return userRepository.save(user);
    }

    public User login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!user.getPassword().equals(request.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        return user;
    }

    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!user.getPassword().equals(oldPassword)) {
            throw new RuntimeException("旧密码不正确");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("新密码至少6位");
        }
        user.setPassword(newPassword);
        userRepository.save(user);
    }

    public Map<String, Object> getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        Map<String, Object> info = new HashMap<>();
        info.put("username", user.getUsername());
        info.put("createTime", user.getCreateTime());
        info.put("docCount", documentRepository.countByUserId(userId));
        info.put("convCount", conversationRepository.countByUserId(userId));
        return info;
    }
}
