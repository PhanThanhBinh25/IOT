package com.iot.smarthome.dto.request;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/** DTO tham số tìm kiếm dữ liệu cảm biến (query params). */
@Data
public class SensorSearchRequest {

    /** Tên cảm biến: Nhiệt độ | Độ ẩm | Ánh sáng (null = tất cả) */
    private String sensorName;

    /** Từ khóa tìm trong giá trị */
    private String keyword;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime from;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime to;

    private int page = 0;
    private int size = 10;

    /** Cột sắp xếp: id | sensorName | value | createdAt */
    private String sortBy = "createdAt";

    /** Chiều sắp xếp: asc | desc */
    private String sortDir = "desc";
}

