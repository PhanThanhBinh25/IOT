package com.iot.smarthome.config;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình kết nối MQTT broker (Eclipse Mosquitto).
 * Tạo MqttClient và kết nối với thông tin xác thực từ application.properties.
 */
@Slf4j
@Configuration
public class MqttConfig {

    @Value("${mqtt.broker.url}")
    private String brokerUrl;

    @Value("${mqtt.broker.username}")
    private String username;

    @Value("${mqtt.broker.password}")
    private String password;

    @Value("${mqtt.broker.client-id}")
    private String clientId;

    @Value("${mqtt.broker.keep-alive}")
    private int keepAlive;

    /**
     * Khởi tạo Bean MqttClient kết nối tới Mosquitto Broker với cấu hình tự động kết nối lại
     *
     * @return MqttClient Đối tượng kết nối MQTT
     * @throws MqttException nếu kết nối lần đầu thất bại
     */
    @Bean
    public MqttClient mqttClient() throws MqttException {
        MqttClient client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());

        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        options.setCleanSession(true);
        options.setKeepAliveInterval(keepAlive);
        options.setAutomaticReconnect(true);
        options.setConnectionTimeout(10);

        log.info("Đang kết nối MQTT broker: {}", brokerUrl);
        client.connect(options);
        log.info("MQTT kết nối thành công!");

        return client;
    }
}

