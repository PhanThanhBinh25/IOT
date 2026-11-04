package com.iot.smarthome.mqtt;

import com.iot.smarthome.entity.DataSensor;
import com.iot.smarthome.entity.Device;
import com.iot.smarthome.entity.Sensor;
import com.iot.smarthome.repository.DataSensorRepository;
import com.iot.smarthome.repository.DeviceRepository;
import com.iot.smarthome.repository.SensorRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// nhận dữ liệu từ esp32 gửi về và lưu vào database
@Slf4j
@Service
@RequiredArgsConstructor
public class MqttSubscriberService implements MqttCallbackExtended {

    private final MqttClient mqttClient; // Nhận dữ liệu và đăng ký MQTT topic
    private final DataSensorRepository dataSensorRepository; // truy vấn và lưu dữ liệu cảm biến
    private final SensorRepository sensorRepository; // quản lý thông tin các loại cảm biến
    private final DeviceRepository deviceRepository; // quản lý thông tin và trạng thái thiết bị

    // Cache trạng thái LED hiện tại (topic -> "ON"|"OFF")
    private final ConcurrentHashMap<String, String> deviceStateCache = new ConcurrentHashMap<>();

    // Cache thông tin cảm biến để tránh truy vấn DB liên tục
    private final ConcurrentHashMap<String, Sensor> sensorCache = new ConcurrentHashMap<>();

    @Value("${mqtt.topic.sensor.temperature}")
    private String topicTemperature;

    @Value("${mqtt.topic.sensor.humidity}")
    private String topicHumidity;

    @Value("${mqtt.topic.sensor.light}")
    private String topicLight;

    @Value("${mqtt.topic.device.state.led1}")
    private String topicLed1State;

    @Value("${mqtt.topic.device.state.led2}")
    private String topicLed2State;

    @Value("${mqtt.topic.device.state.led3}")
    private String topicLed3State;

    /** Map MQTT topic → tên hiển thị thiết bị. */
    private final Map<String, String> topicToDeviceName = new HashMap<>();

    @PostConstruct
    public void init() throws MqttException {
        mqttClient.setCallback(this);

        // Khởi tạo map topic → device name
        topicToDeviceName.put(topicLed1State, "Đèn");
        topicToDeviceName.put(topicLed2State, "Quạt");
        topicToDeviceName.put(topicLed3State, "Điều hòa");

        // Đồng bộ trạng thái từ cơ sở dữ liệu vào cache khi khởi động hệ thống
        syncDeviceStateFromDatabase();

        // Subscribe tất cả các topic nếu client đang kết nối
        if (mqttClient.isConnected()) {
            subscribeAllTopics();
        }
    }

    /**
     * Nạp trạng thái thiết bị đã lưu từ database vào cache bộ nhớ.
     */
    public void syncDeviceStateFromDatabase() {
        for (Map.Entry<String, String> entry : topicToDeviceName.entrySet()) {
            String topic = entry.getKey();
            String deviceName = entry.getValue();
            deviceRepository.findByName(deviceName).ifPresent(dev -> {
                if (dev.getStatus() != null && !dev.getStatus().isBlank()) {
                    String status = dev.getStatus().toUpperCase();
                    deviceStateCache.put(topic, status);
                    log.info("Khởi tạo cache trạng thái từ DB: {} [{}] = {}", deviceName, topic, status);
                }
            });
        }
    }

    /**
     * Đăng ký tất cả các topic MQTT cần lắng nghe.
     */
    public void subscribeAllTopics() {
        String[] topics = {
            topicTemperature, topicHumidity, topicLight,
            topicLed1State, topicLed2State, topicLed3State
        };

        for (String topic : topics) {
            try {
                mqttClient.subscribe(topic, 1);// mức QoS = 1
                log.info("Đã subscribe topic: {}", topic);
            } catch (MqttException e) {
                log.error("Lỗi khi subscribe topic {}: {}", topic, e.getMessage());
            }
        }
    }

    /**
     * Nhận và điều phối xử lý gói tin MQTT từ broker
     *
     * @param topic Tên topic gửi tới
     * @param message Nội dung gói tin MQTT nhận được
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload()).trim();
        log.debug("MQTT nhận - topic: {} | payload: {}", topic, payload);

        try {
            if (topic.equals(topicTemperature)) {
                saveSensorData("Nhiệt độ", payload);

            } else if (topic.equals(topicHumidity)) {
                saveSensorData("Độ ẩm", payload);

            } else if (topic.equals(topicLight)) {
                saveSensorData("Ánh sáng", payload);

            } else if (topicToDeviceName.containsKey(topic)) {
                handleDeviceState(topic, payload);
            }
        } catch (Exception e) {
            log.error("Lỗi xử lý MQTT message - topic: {}, payload: {}", topic, payload, e);
        }
    }

    /** Lưu dữ liệu cảm biến vào bảng data_sensor. */
    private void saveSensorData(String sensorType, String payload) {
        try {
            float value = Float.parseFloat(payload);
            // kiểm tra key trong cache nếu tồn tại thì lấy giá trị đang lưu còn không thì tự tạo giá trị mới và lưu
            Sensor sensor = sensorCache.computeIfAbsent(sensorType, type ->
                sensorRepository.findByType(type)
                    .orElseGet(() -> {
                        String unit = switch (type) {
                            case "Nhiệt độ" -> "°C";
                            case "Độ ẩm" -> "%";
                            case "Ánh sáng" -> "lux";
                            default -> "";
                        };
                        return sensorRepository.save(Sensor.builder()
                                .name("Cảm biến " + type.toLowerCase())
                                .type(type)
                                .unit(unit)
                                .build());
                    })
            );

            DataSensor data = DataSensor.builder()
                    .sensor(sensor)
                    .value(value)
                    .createAt(LocalDateTime.now())
                    .build();

            dataSensorRepository.save(data);
            log.debug("Đã lưu data_sensor [{}]: {} {}", sensorType, value, sensor.getUnit());
        } catch (NumberFormatException e) {
            log.warn("Giá trị số không hợp lệ từ cảm biến {}: {}", sensorType, payload);
        }
    }

    /** Xử lý cập nhật trạng thái thiết bị từ ESP32 vào DB và cache. */
    private void handleDeviceState(String topic, String payload) {
        String deviceName = topicToDeviceName.get(topic);
        String command = payload.toUpperCase();

        if (!command.equals("ON") && !command.equals("OFF")) {
            log.warn("Lệnh không hợp lệ từ topic {}: {}", topic, payload);
            return;
        }

        // Cập nhật cache
        deviceStateCache.put(topic, command);

        // Cập nhật bảng device trong Database
        deviceRepository.findByName(deviceName).ifPresent(dev -> {
            dev.setStatus(command);
            deviceRepository.save(dev);
        });

        log.info("Thiết bị {} → {} (cập nhật trạng thái)", deviceName, command);
    }

    /**
     * Lấy trạng thái thiết bị theo topic trạng thái từ cache bộ nhớ
     *
     * @param stateTopic Topic trạng thái MQTT
     * @return Chuỗi 'ON' hoặc 'OFF', hoặc null nếu chưa có trong cache
     */
    public String getDeviceState(String stateTopic) {
        return deviceStateCache.get(stateTopic);
    }

    /** Cập nhật ngay trạng thái thiết bị vào cache bộ nhớ. */
    public void updateDeviceState(String topic, String command) {
        if (topic != null && command != null) {
            deviceStateCache.put(topic, command.toUpperCase());
        }
    }

    /**
     * Lấy bản sao toàn bộ trạng thái thiết bị đang lưu trong cache
     *
     * @return Map ánh xạ topic sang trạng thái
     */
    public Map<String, String> getAllDeviceStates() {
        return Map.copyOf(deviceStateCache);
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        log.info("MQTT kết nối thành công: {} (reconnect = {})", serverURI, reconnect);
        if (reconnect) {
            log.info("Tự động đăng ký lại các topic sau khi kết nối lại MQTT...");
            subscribeAllTopics();
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.warn("Mất kết nối MQTT: {}", cause != null ? cause.getMessage() : "Unknown");
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }
}
