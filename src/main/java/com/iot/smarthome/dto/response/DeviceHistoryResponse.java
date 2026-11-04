package com.iot.smarthome.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Response lịch sử hoạt động thiết bị. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceHistoryResponse {
    private Long id;
    private String deviceName;
    private String action;
    private String status;
    private String performedBy;
    private LocalDateTime createdAt;
}

