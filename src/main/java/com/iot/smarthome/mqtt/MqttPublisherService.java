package com.iot.smarthome.mqtt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

// gửi lệnh diều khiển thiết bị từ backend tới ESP qua giao thức MQTT

@Slf4j
@Service
@RequiredArgsConstructor
public class MqttPublisherService {

    private final MqttClient mqttClient;

    @Value("${mqtt.topic.device.cmd.led1}") // Anotation lây giá trị của file cấu hình rồi gán vào các biến
    private String topicLed1;

    @Value("${mqtt.topic.device.cmd.led2}")
    private String topicLed2;

    @Value("${mqtt.topic.device.cmd.led3}")
    private String topicLed3;

    @Value("${mqtt.topic.device.cmd.ledall}")
    private String topicLedAll;

    /** Map tên thiết bị từ FE → MQTT command topic. */
    private Map<String, String> getTopicMap() {
        return Map.of(
            "light", topicLed1,
            "fan",   topicLed2,
            "ac",    topicLed3,
            "all",   topicLedAll
        );
    }

    /**
     * Gửi lệnh bật/tắt thiết bị.
     *
     * @param device  Tên thiết bị: "light" | "fan" | "ac" | "all"
     * @param command Lệnh: "ON" | "OFF"
     * @throws IllegalArgumentException nếu tên thiết bị không hợp lệ
     * @throws MqttException nếu gửi thất bại
     */
    public void sendCommand(String device, String command) throws MqttException {
        String topic = getTopicMap().get(device.toLowerCase());
        if (topic == null) {
            throw new IllegalArgumentException("Thiết bị không hợp lệ: " + device);
        }

        String cmd = command.toUpperCase();
        if (!cmd.equals("ON") && !cmd.equals("OFF")) {
            throw new IllegalArgumentException("Lệnh không hợp lệ: " + command);
        }

        MqttMessage mqttMessage = new MqttMessage(cmd.getBytes());
        mqttMessage.setQos(1);
        mqttMessage.setRetained(true); // ESP32 nhận được trạng thái khi kết nối lại, lưu lại thông điệp cuối cùng của topic để có thể gửi lại

        mqttClient.publish(topic, mqttMessage);
        log.info("Đã gửi lệnh MQTT: topic={}, command={}", topic, cmd);
    }
}

