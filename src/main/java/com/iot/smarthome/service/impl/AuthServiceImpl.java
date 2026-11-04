package com.iot.smarthome.service.impl;

import com.iot.smarthome.dto.request.LoginRequest;
import com.iot.smarthome.dto.response.LoginResponse;
import com.iot.smarthome.entity.User;
import com.iot.smarthome.exception.ResourceNotFoundException;
import com.iot.smarthome.repository.UserRepository;
import com.iot.smarthome.security.JwtTokenProvider;
import com.iot.smarthome.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    /**
     * Xác thực thông tin đăng nhập và sinh JWT Token
     *
     * @param request Chứa username và password người dùng gửi lên
     * @return LoginResponse chứa Token, loại token và thông tin người dùng
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        // Spring Security xác thực username + password
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        String token = jwtTokenProvider.generateToken(userDetails);

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        log.info("Đăng nhập thành công: {}", user.getUsername());

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }

    /**
     * Tìm kiếm thông tin người dùng trong cơ sở dữ liệu theo username
     *
     * @param username Tên đăng nhập cần tìm
     * @return User Đối tượng Entity người dùng
     * @throws ResourceNotFoundException nếu không tìm thấy người dùng
     */
    @Override
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại: " + username));
    }
}

