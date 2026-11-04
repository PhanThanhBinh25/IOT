package com.iot.smarthome.service;

import com.iot.smarthome.dto.request.SensorSearchRequest;
import com.iot.smarthome.dto.response.PageResponse;
import com.iot.smarthome.dto.response.SensorDataResponse;

import java.util.List;

public interface SensorDataService {

    /** Lấy giá trị mới nhất của từng loại cảm biến. */
    SensorDataResponse getLatest(String sensorName);

    /** Lấy danh sách 7 điểm gần nhất cho biểu đồ dashboard. */
    List<SensorDataResponse> getChartData(String sensorName);

    /** Tìm kiếm dữ liệu cảm biến có phân trang và lọc. */
    PageResponse<SensorDataResponse> search(SensorSearchRequest request);
}

