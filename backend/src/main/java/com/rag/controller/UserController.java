package com.rag.controller;

import com.rag.dto.*;
import com.rag.entity.User;
import com.rag.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ApiResponse<User> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = userService.register(request);
            user.setPassword(null);
            return ApiResponse.success(user);
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/login")
    public ApiResponse<User> login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        try {
            User user = userService.login(request);
            session.setAttribute("userId", user.getId());
            user.setPassword(null);
            return ApiResponse.success(user);
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpSession session) {
        session.invalidate();
        return ApiResponse.success();
    }

    @GetMapping("/current")
    public ApiResponse<Long> getCurrentUserId(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ApiResponse.error(401, "未登录");
        return ApiResponse.success(userId);
    }

    @GetMapping("/info")
    public ApiResponse<java.util.Map<String, Object>> getUserInfo(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ApiResponse.error(401, "请先登录");
        return ApiResponse.success(userService.getUserInfo(userId));
    }

    @PutMapping("/change-password")
    public ApiResponse<Void> changePassword(@RequestBody Map<String, String> body,
                                             HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ApiResponse.error(401, "请先登录");
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        if (oldPassword == null || oldPassword.isEmpty())
            return ApiResponse.error(400, "请输入旧密码");
        if (newPassword == null || newPassword.length() < 6)
            return ApiResponse.error(400, "新密码至少6位");
        try {
            userService.changePassword(userId, oldPassword, newPassword);
            session.invalidate();
            return ApiResponse.success();
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }
}
