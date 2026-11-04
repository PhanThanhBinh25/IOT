package com.iot.smarthome.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity lưu lịch sử thao tác bật/tắt thiết bị.
 * Bảng: device_action theo thiết kế ERD
 */
@Entity
@Table(name = "device_action",
       indexes = {
           @Index(name = "idx_action_device", columnList = "Device_id"),
           @Index(name = "idx_action_time",   columnList = "Time")
       })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DeviceAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "Device_id", nullable = false)
    private Device device;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "User_id")
    private User user;

    @Column(name = "Action", nullable = false, length = 100)
    private String action; // "Bật", "Tắt"

    @Column(name = "Status", nullable = false, length = 50)
    private String status; // "Thành công", "Thất bại"

    @Column(name = "Time", nullable = false)
    private LocalDateTime time;

    @PrePersist
    void prePersist() {
        if (this.time == null) {
            this.time = LocalDateTime.now();
        }
    }
}
