package com.iot.smarthome.service;

import com.iot.smarthome.dto.request.DeviceCommandRequest;
import com.iot.smarthome.dto.request.HistorySearchRequest;
import com.iot.smarthome.dto.response.DeviceHistoryResponse;
import com.iot.smarthome.dto.response.PageResponse;

import java.util.Map;

public interface DeviceService {

    /**
     * Gửi lệnh bật/tắt thiết bị qua MQTT và lưu lịch sử.
     *
     * @param request    Lệnh điều khiển
     * @param performedBy Username người thực hiện
     */
    void sendCommand(DeviceCommandRequest request, String performedBy);

    /** Lấy trạng thái hiện tại tất cả thiết bị (ON/OFF). */
    Map<String, String> getCurrentStates();

    /** Tìm kiếm lịch sử hoạt động thiết bị có phân trang. */
    PageResponse<DeviceHistoryResponse> searchHistory(HistorySearchRequest request);
}

