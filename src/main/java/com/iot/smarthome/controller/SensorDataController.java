package com.iot.smarthome.controller;

import com.iot.smarthome.dto.request.SensorSearchRequest;
import com.iot.smarthome.dto.response.ApiResponse;
import com.iot.smarthome.dto.response.PageResponse;
import com.iot.smarthome.dto.response.SensorDataResponse;
import com.iot.smarthome.service.SensorDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller cung cấp API dữ liệu cảm biến.
 * Base URL: /api/sensors
 */
@RestController
@RequestMapping("/api/sensors")
@RequiredArgsConstructor
public class SensorDataController {

    private final SensorDataService sensorDataService;

    /**
     * GET /api/sensors/latest?sensorName=Nhiệt độ
     * Lấy giá trị mới nhất của một loại cảm biến.
     */
    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<SensorDataResponse>> getLatest(
            @RequestParam String sensorName) {
        return ResponseEntity.ok(ApiResponse.ok(sensorDataService.getLatest(sensorName)));
    }

    /**
     * GET /api/sensors/chart?sensorName=Nhiệt độ
     * Lấy 7 điểm dữ liệu gần nhất để vẽ biểu đồ dashboard.
     */
    @GetMapping("/chart")
    public ResponseEntity<ApiResponse<List<SensorDataResponse>>> getChartData(
            @RequestParam String sensorName) {
        return ResponseEntity.ok(ApiResponse.ok(sensorDataService.getChartData(sensorName)));
    }

    /**
     * GET /api/sensors?sensorName=&keyword=&from=&to=&page=0&size=10&sortBy=createdAt&sortDir=desc
     * Tìm kiếm dữ liệu cảm biến có phân trang, lọc, sắp xếp.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SensorDataResponse>>> search(
            SensorSearchRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(sensorDataService.search(request)));
    }
}

