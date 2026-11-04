package com.iot.smarthome;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Điểm khởi chạy chính của ứng dụng SmartHome Backend Spring Boot.
 * Kích hoạt Spring Boot Autoconfiguration và tính năng định thời @EnableScheduling.
 */
@SpringBootApplication
@EnableScheduling
public class SmartHomeApplication {

    /**
     * Hàm main khởi động toàn bộ ứng dụng Spring Boot.
     *
     * @param args Tham số dòng lệnh truyền vào khi khởi động
     */
    public static void main(String[] args) {
        SpringApplication.run(SmartHomeApplication.class, args);
    }
}

