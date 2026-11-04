package com.iot.smarthome.dto.request;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/** DTO tham số tìm kiếm lịch sử thiết bị. */
@Data
public class HistorySearchRequest {

    /** Tên thiết bị: Đèn | Quạt | Điều hòa (null = tất cả) */
    private String deviceName;

    /** Hành động: Bật | Tắt (null = tất cả) */
    private String action;

    /** Trạng thái: Thành công | Thất bại | Đang xử lý (null = tất cả) */
    private String status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime from;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime to;

    private int page = 0;
    private int size = 10;

    /** Cột sắp xếp: id | deviceName | action | status | createdAt */
    private String sortBy = "createdAt";

    /** Chiều sắp xếp: asc | desc */
    private String sortDir = "desc";
}

