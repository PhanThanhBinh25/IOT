package com.iot.smarthome.controller;

import com.iot.smarthome.dto.request.DeviceCommandRequest;
import com.iot.smarthome.dto.request.HistorySearchRequest;
import com.iot.smarthome.dto.response.ApiResponse;
import com.iot.smarthome.dto.response.DeviceHistoryResponse;
import com.iot.smarthome.dto.response.PageResponse;
import com.iot.smarthome.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller điều khiển thiết bị và xem lịch sử hoạt động.
 * Base URL: /api/devices
 */
@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    /**
     * POST /api/devices/command
     * Gửi lệnh bật/tắt thiết bị.
     * Body: { "device": "light", "command": "ON" }
     */
    @PostMapping("/command")
    public ResponseEntity<ApiResponse<Void>> sendCommand(
            @Valid @RequestBody DeviceCommandRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String username = (userDetails != null) ? userDetails.getUsername() : "admin";
        deviceService.sendCommand(request, username);
        return ResponseEntity.ok(ApiResponse.ok("Lệnh đã được gửi thành công", null));
    }

    /**
     * GET /api/devices/states
     * Lấy trạng thái hiện tại của tất cả thiết bị.
     * Response: { "light": "ON", "fan": "OFF", "ac": "OFF" }
     */
    @GetMapping("/states")
    public ResponseEntity<ApiResponse<Map<String, String>>> getStates() {
        return ResponseEntity.ok(ApiResponse.ok(deviceService.getCurrentStates()));
    }

    /**
     * GET /api/devices/history?deviceName=&action=&status=&from=&to=&page=0&size=10
     * Tìm kiếm lịch sử hoạt động thiết bị có phân trang và lọc.
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<PageResponse<DeviceHistoryResponse>>> getHistory(
            HistorySearchRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(deviceService.searchHistory(request)));
    }
}

