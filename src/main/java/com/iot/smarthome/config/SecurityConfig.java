package com.iot.smarthome.config;

import com.iot.smarthome.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Cấu hình Spring Security: JWT, CORS, phân quyền endpoint.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final UserDetailsService userDetailsService;

    /**
     * Cấu hình chuỗi lọc bảo mật (SecurityFilterChain):
     * - Vô hiệu hóa CSRF (sử dụng JWT stateless).
     * - Kích hoạt CORS với cấu hình nguồn gốc mở.
     * - Quản lý phiên SessionCreationPolicy.STATELESS.
     * - Phân quyền endpoint: permitAll cho OPTIONS và POST /api/auth/login, các endpoint khác yêu cầu xác thực.
     * - Xử lý lỗi xác thực 401 trả về JSON đồng bộ.
     * - Tích hợp JwtAuthenticationFilter trước UsernamePasswordAuthenticationFilter.
     *
     * @param http Đối tượng cấu hình HttpSecurity
     * @return Chuỗi lọc SecurityFilterChain đã cấu hình
     * @throws Exception nếu xảy ra lỗi cấu hình
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Cho phép preflight CORS requests
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Public: chỉ đăng nhập
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                // Tất cả các endpoint khác cần xác thực
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"success\":false,\"message\":\"Yêu cầu xác thực. Vui lòng gửi kèm Bearer Token hợp lệ trong header Authorization.\"}");
                })
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Cấu hình chia sẻ tài nguyên giữa các nguồn gốc (CORS):
     * Cho phép Frontend (Live Server, Vite, web app) gửi request API đến Backend với mọi header và method HTTP.
     *
     * @return Nguồn cấu hình CORS cho toàn bộ đường dẫn URL
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * Cung cấp cơ chế xác thực người dùng dựa trên DaoAuthenticationProvider:
     * Kết nối UserDetailsService với cơ chế mã hóa mật khẩu BCrypt.
     *
     * @return Đối tượng AuthenticationProvider
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Bean quản lý xác thực trung tâm của Spring Security (AuthenticationManager).
     *
     * @param config Cấu hình xác thực từ Spring Security
     * @return Đối tượng AuthenticationManager
     * @throws Exception nếu xảy ra lỗi lấy authentication manager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Bean mã hóa mật khẩu người dùng sử dụng thuật toán băm an toàn BCrypt.
     *
     * @return Bộ mã hóa PasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

