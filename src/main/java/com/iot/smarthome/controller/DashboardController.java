package com.iot.smarthome.controller;

import com.iot.smarthome.dto.response.ApiResponse;
import com.iot.smarthome.dto.response.DashboardResponse;
import com.iot.smarthome.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller cung cấp dữ liệu tổng quan cho Dashboard.
 * Base URL: /api/dashboard
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * GET /api/dashboard
     * Lấy dữ liệu cảm biến mới nhất và trạng thái các thiết bị.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {
        DashboardResponse data = dashboardService.getDashboardData();
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}

