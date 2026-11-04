package com.iot.smarthome.service.impl;

import com.iot.smarthome.dto.response.DashboardResponse;
import com.iot.smarthome.entity.DataSensor;
import com.iot.smarthome.mqtt.MqttSubscriberService;
import com.iot.smarthome.repository.DataSensorRepository;
import com.iot.smarthome.repository.DeviceRepository;
import com.iot.smarthome.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final DataSensorRepository dataSensorRepository;
    private final DeviceRepository deviceRepository;
    private final MqttSubscriberService mqttSubscriber;

    @Value("${mqtt.topic.device.state.led1}")
    private String topicLed1State;

    @Value("${mqtt.topic.device.state.led2}")
    private String topicLed2State;

    @Value("${mqtt.topic.device.state.led3}")
    private String topicLed3State;

    /**
     * Tổng hợp dữ liệu hiển thị cho màn hình Dashboard chính
     *
     * @return DashboardResponse Chứa nhiệt độ, độ ẩm, ánh sáng và trạng thái 3 thiết bị
     */
    @Override
    public DashboardResponse getDashboardData() {
        // Lấy giá trị mới nhất của mỗi cảm biến
        Double temperature = getLatestValue("Nhiệt độ");
        Double humidity    = getLatestValue("Độ ẩm");
        Double light       = getLatestValue("Ánh sáng");

        // Lấy trạng thái thiết bị thực tế từ cache MQTT (phản hồi từ ESP32), nếu chưa có thì lấy từ DB
        Boolean led1 = resolveDeviceState("Đèn", topicLed1State);
        Boolean led2 = resolveDeviceState("Quạt", topicLed2State);
        Boolean led3 = resolveDeviceState("Điều hòa", topicLed3State);

        return DashboardResponse.builder()
                .temperature(temperature)
                .humidity(humidity)
                .light(light)
                .led1(led1)
                .led2(led2)
                .led3(led3)
                .build();
    }

    /**
     * Xác định trạng thái của thiết bị (ưu tiên từ MQTT cache, fallback sang database)
     *
     * @param deviceName Tên thiết bị hiển thị trong cơ sở dữ liệu
     * @param stateTopic Topic MQTT nhận trạng thái từ ESP32
     * @return Boolean true nếu thiết bị đang bật (ON), false nếu tắt (OFF)
     */
    private Boolean resolveDeviceState(String deviceName, String stateTopic) {
        // 1. Ưu tiên trạng thái thực tế từ MQTT (ESP32)
        String cached = mqttSubscriber.getDeviceState(stateTopic);
        if (cached != null) {
            return "ON".equalsIgnoreCase(cached);
        }
        // 2. Fallback trạng thái lưu trong cơ sở dữ liệu
        return deviceRepository.findByName(deviceName)
                .map(d -> "ON".equalsIgnoreCase(d.getStatus()))
                .orElse(false);
    }

    /**
     * Lấy giá trị đo đạc mới nhất của một loại cảm biến từ cơ sở dữ liệu
     *
     * @param sensorType Loại cảm biến ('Nhiệt độ', 'Độ ẩm', 'Ánh sáng')
     * @return Double Giá trị cảm biến đã làm tròn 1 chữ số thập phân
     */
    private Double getLatestValue(String sensorType) {
        return dataSensorRepository
                .findTopBySensor_TypeOrderByCreateAtDesc(sensorType)
                .map(d -> d.getValue() != null ? Math.round(d.getValue() * 10.0) / 10.0 : null)
                .orElse(null);
    }
}
