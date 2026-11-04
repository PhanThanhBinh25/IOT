package com.iot.smarthome.config;

import com.iot.smarthome.entity.Device;
import com.iot.smarthome.entity.Sensor;
import com.iot.smarthome.entity.User;
import com.iot.smarthome.repository.DeviceRepository;
import com.iot.smarthome.repository.SensorRepository;
import com.iot.smarthome.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Khởi tạo dữ liệu mặc định khi ứng dụng chạy lần đầu:
 * - Tài khoản admin
 * - 3 thiết bị (Đèn, Quạt, Điều hòa)
 * - 3 cảm biến (Nhiệt độ, Độ ẩm, Ánh sáng)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final SensorRepository sensorRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Khởi chạy khi ứng dụng Spring Boot vừa hoàn tất nạp ngữ cảnh:
     * 1. Kiểm tra và tạo tài khoản quản trị viên (admin) mặc định nếu chưa tồn tại.
     * 2. Khởi tạo danh sách 3 thiết bị mẫu (Đèn, Quạt, Điều hòa) với trạng thái OFF.
     * 3. Khởi tạo danh sách 3 cảm biến mẫu (Nhiệt độ, Độ ẩm, Ánh sáng) kèm đơn vị đo tương ứng.
     *
     * @param args Tham số dòng lệnh khi khởi động ứng dụng
     */
    @Override
    public void run(String... args) {
        // 1. Tạo tài khoản admin mặc định
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("Phan Thanh Binh")
                    .email("phanthanhbinh@gmail.com")
                    .sdt("0349883754")
                    .dateOfBirth("28/08/2005")
                    .avatarUrl("avatar.png")
                    .role("ADMIN")
                    .build();
            userRepository.save(admin);
            log.info("✅ Đã tạo tài khoản admin mặc định (username=admin, password=admin123)");
        }

        // 2. Tạo các thiết bị mặc định
        List<String> defaultDevices = List.of("Đèn", "Quạt", "Điều hòa");
        for (String devName : defaultDevices) {
            if (deviceRepository.findByName(devName).isEmpty()) {
                Device device = Device.builder()
                        .name(devName)
                        .status("OFF")
                        .build();
                deviceRepository.save(device);
                log.info("✅ Đã khởi tạo thiết bị: {}", devName);
            }
        }

        // 3. Tạo các loại cảm biến mặc định
        List<Sensor> defaultSensors = List.of(
                Sensor.builder().name("Cảm biến nhiệt độ").type("Nhiệt độ").unit("°C").build(),
                Sensor.builder().name("Cảm biến độ ẩm").type("Độ ẩm").unit("%").build(),
                Sensor.builder().name("Cảm biến ánh sáng").type("Ánh sáng").unit("lux").build()
        );

        for (Sensor s : defaultSensors) {
            if (sensorRepository.findByType(s.getType()).isEmpty()) {
                sensorRepository.save(s);
                log.info("✅ Đã khởi tạo cảm biến: {} ({})", s.getName(), s.getUnit());
            }
        }
    }
}
