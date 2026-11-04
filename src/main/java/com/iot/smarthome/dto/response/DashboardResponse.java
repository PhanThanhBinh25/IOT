package com.iot.smarthome.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response giá trị cảm biến mới nhất cho dashboard. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private Double temperature;   // Nhiệt độ (°C)
    private Double humidity;      // Độ ẩm (%)
    private Double light;         // Ánh sáng (lux)

    private Boolean led1;         // Trạng thái Đèn
    private Boolean led2;         // Trạng thái Quạt
    private Boolean led3;         // Trạng thái Điều hòa
}

