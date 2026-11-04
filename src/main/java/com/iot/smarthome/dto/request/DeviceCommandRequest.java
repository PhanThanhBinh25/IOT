package com.iot.smarthome.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** DTO gửi lệnh điều khiển thiết bị (bật/tắt). */
@Data
public class DeviceCommandRequest {

    /**
     * Tên thiết bị: "light" | "fan" | "ac" | "all"
     * (ánh xạ sang LED1/LED2/LED3/ALL của ESP32)
     */
    @NotBlank(message = "Tên thiết bị không được để trống")
    private String device;

    /**
     * Lệnh: "ON" hoặc "OFF"
     */
    @NotBlank(message = "Lệnh không được để trống")
    private String command;
}

