package com.iot.smarthome.service;

import com.iot.smarthome.dto.response.DashboardResponse;

public interface DashboardService {

    /** Tổng hợp giá trị cảm biến mới nhất và trạng thái thiết bị cho dashboard. */
    DashboardResponse getDashboardData();
}

