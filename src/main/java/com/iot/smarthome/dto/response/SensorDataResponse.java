package com.iot.smarthome.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response dữ liệu cảm biến trả về FE.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SensorDataResponse {
    private Long id;
    private String sensorName;
    private Double value;
    private String unit;
    private String displayValue; // "27.5 °C"
    private LocalDateTime createdAt;
}

