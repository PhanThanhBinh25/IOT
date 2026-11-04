package com.iot.smarthome.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity lưu thông tin danh mục cảm biến (Nhiệt độ, Độ ẩm, Ánh sáng).
 * Bảng: sensor theo thiết kế ERD
 */
@Entity
@Table(name = "sensor")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Sensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "Name", nullable = false, length = 255)
    private String name; // Cảm biến nhiệt độ DHT22, etc.

    @Column(name = "Type", nullable = false, length = 100)
    private String type; // "Nhiệt độ", "Độ ẩm", "Ánh sáng"

    @Column(name = "Unit", nullable = false, length = 50)
    private String unit; // "°C", "%", "lux"

    @Column(name = "Create_at")
    private LocalDateTime createAt;

    @PrePersist
    void prePersist() {
        if (this.createAt == null) {
            this.createAt = LocalDateTime.now();
        }
    }
}
