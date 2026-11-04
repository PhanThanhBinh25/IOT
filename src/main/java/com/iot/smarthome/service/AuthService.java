package com.iot.smarthome.service;

import com.iot.smarthome.dto.request.LoginRequest;
import com.iot.smarthome.dto.response.LoginResponse;
import com.iot.smarthome.entity.User;

public interface AuthService {

    /** Xác thực tài khoản và trả về JWT token. */
    LoginResponse login(LoginRequest request);

    /** Lấy thông tin user hiện tại theo username. */
    User getUserByUsername(String username);
}

