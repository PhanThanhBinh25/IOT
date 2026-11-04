package com.iot.smarthome.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity lưu thông tin thiết bị (Đèn, Quạt, Điều hòa...).
 * Bảng: device theo thiết kế ERD
 */
@Entity
@Table(name = "device")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "Name", nullable = false, length = 255)
    private String name;

    @Column(name = "Create_at")
    private LocalDateTime createAt;

    @Column(name = "Status", length = 50)
    private String status; // "ON", "OFF"

    @PrePersist
    void prePersist() {
        if (this.createAt == null) {
            this.createAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = "OFF";
        }
    }
}
