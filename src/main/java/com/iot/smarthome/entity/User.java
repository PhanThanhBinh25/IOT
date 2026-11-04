package com.iot.smarthome.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity lưu thông tin người dùng hệ thống.
 * Bảng: users theo thiết kế ERD
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "Username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "Password", nullable = false, length = 255)
    private String password;

    @Column(name = "Fullname", nullable = false, length = 255)
    private String fullName;

    @Column(name = "Email", unique = true, length = 255)
    private String email;

    @Column(name = "Date_of_birth", length = 20)
    private String dateOfBirth;

    @Column(name = "SDT", length = 20)
    private String sdt;

    @Column(name = "Avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "Role", length = 50)
    @Builder.Default
    private String role = "ADMIN";

    @Column(name = "Create_at")
    private LocalDateTime createAt;

    @PrePersist
    void prePersist() {
        if (this.createAt == null) {
            this.createAt = LocalDateTime.now();
        }
        if (this.role == null) {
            this.role = "ADMIN";
        }
    }
}
