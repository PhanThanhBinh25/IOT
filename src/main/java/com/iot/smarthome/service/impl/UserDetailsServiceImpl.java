package com.iot.smarthome.service.impl;

import com.iot.smarthome.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Triển khai UserDetailsService để Spring Security load user từ DB khi xác thực.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Tải thông tin tài khoản và quyền hạn của người dùng từ Database theo username
     *
     * @param username Tên đăng nhập của người dùng
     * @return UserDetails Đối tượng được Spring Security dùng để kiểm tra mật khẩu và phân quyền
     * @throws UsernameNotFoundException nếu không tìm thấy username
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .map(user -> new org.springframework.security.core.userdetails.User(
                        user.getUsername(),
                        user.getPassword(),
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
                ))
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Không tìm thấy người dùng: " + username));
    }
}

