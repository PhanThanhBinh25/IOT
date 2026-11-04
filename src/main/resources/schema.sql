-- =============================================
-- IoT Smart Home - Database Schema
-- Thiết kế chuẩn hóa theo ERD 5 bảng
-- =============================================

CREATE DATABASE IF NOT EXISTS iot_system
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE iot_system;

-- 1. Bảng users
CREATE TABLE IF NOT EXISTS users (
    ID INT AUTO_INCREMENT PRIMARY KEY,
    Username VARCHAR(100) NOT NULL UNIQUE,
    Password VARCHAR(255) NOT NULL,
    Fullname VARCHAR(255) NOT NULL,
    Email VARCHAR(255) UNIQUE,
    Date_of_birth DATE,
    SDT VARCHAR(20),
    Avatar_url VARCHAR(500),
    Role VARCHAR(50) DEFAULT 'ADMIN',
    Create_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Bảng device (Đèn, Quạt, Điều hòa...)
CREATE TABLE IF NOT EXISTS device (
    ID INT AUTO_INCREMENT PRIMARY KEY,
    Name VARCHAR(255) NOT NULL,
    Create_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    Status VARCHAR(50) DEFAULT 'OFF'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Bảng device_action (Lịch sử thao tác thiết bị)
CREATE TABLE IF NOT EXISTS device_action (
    ID INT AUTO_INCREMENT PRIMARY KEY,
    Device_id INT NOT NULL,
    User_id INT,
    Action VARCHAR(100) NOT NULL,
    Status VARCHAR(50) NOT NULL,
    Time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_action_device FOREIGN KEY (Device_id) REFERENCES device(ID) ON DELETE CASCADE,
    CONSTRAINT fk_action_user FOREIGN KEY (User_id) REFERENCES users(ID) ON DELETE SET NULL,
    INDEX idx_action_device (Device_id),
    INDEX idx_action_time (Time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Bảng sensor (Nhiệt độ, Độ ẩm, Ánh sáng...)
CREATE TABLE IF NOT EXISTS sensor (
    ID INT AUTO_INCREMENT PRIMARY KEY,
    Name VARCHAR(255) NOT NULL,
    Type VARCHAR(100) NOT NULL,
    Unit VARCHAR(50) NOT NULL,
    Create_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. Bảng data_sensor (Dữ liệu đo được từ cảm biến)
CREATE TABLE IF NOT EXISTS data_sensor (
    ID INT AUTO_INCREMENT PRIMARY KEY,
    Sensor_id INT NOT NULL,
    Value FLOAT NOT NULL,
    Create_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_data_sensor FOREIGN KEY (Sensor_id) REFERENCES sensor(ID) ON DELETE CASCADE,
    INDEX idx_data_sensor_id (Sensor_id),
    INDEX idx_data_sensor_time (Create_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
