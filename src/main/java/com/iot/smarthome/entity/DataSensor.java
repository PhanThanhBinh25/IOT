package com.iot.smarthome.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity lưu số liệu đo đạc thực tế từ cảm biến.
 * Bảng: data_sensor theo thiết kế ERD
 */
@Entity
@Table(name = "data_sensor",
       indexes = {
           @Index(name = "idx_data_sensor_id",   columnList = "Sensor_id"),
           @Index(name = "idx_data_sensor_time", columnList = "Create_at")
       })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DataSensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "Sensor_id", nullable = false)
    private Sensor sensor;

    @Column(name = "Value", nullable = false)
    private Float value;

    @Column(name = "Create_at", nullable = false)
    private LocalDateTime createAt;

    @PrePersist
    void prePersist() {
        if (this.createAt == null) {
            this.createAt = LocalDateTime.now();
        }
    }
}
