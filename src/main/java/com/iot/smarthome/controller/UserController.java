package com.iot.smarthome.controller;

import com.iot.smarthome.dto.response.ApiResponse;
import com.iot.smarthome.entity.User;
import com.iot.smarthome.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller trả về thông tin hồ sơ người dùng đang đăng nhập.
 * Base URL: /api/users
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    /**
     * GET /api/users/me
     * Lấy thông tin cá nhân của người dùng đang đăng nhập (từ JWT).
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = authService.getUserByUsername(userDetails.getUsername());

        // dateOfBirth là String (dd/MM/yyyy), lấy thẳng không cần format
        String dobStr = user.getDateOfBirth() != null ? user.getDateOfBirth() : "";

        Map<String, Object> profile = new HashMap<>();
        profile.put("username",    user.getUsername());
        profile.put("fullName",    user.getFullName() != null ? user.getFullName() : "");
        profile.put("email",       user.getEmail() != null ? user.getEmail() : "");
        profile.put("phone",       user.getSdt() != null ? user.getSdt() : "");
        profile.put("dateOfBirth", dobStr);
        profile.put("avatarUrl",   user.getAvatarUrl() != null ? user.getAvatarUrl() : "");
        profile.put("role",        user.getRole());

        return ResponseEntity.ok(ApiResponse.ok(profile));
    }
}
