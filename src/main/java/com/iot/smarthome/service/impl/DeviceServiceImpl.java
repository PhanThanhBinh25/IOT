package com.iot.smarthome.service.impl;

import com.iot.smarthome.dto.request.DeviceCommandRequest;
import com.iot.smarthome.dto.request.HistorySearchRequest;
import com.iot.smarthome.dto.response.DeviceHistoryResponse;
import com.iot.smarthome.dto.response.PageResponse;
import com.iot.smarthome.entity.Device;
import com.iot.smarthome.entity.DeviceAction;
import com.iot.smarthome.entity.User;
import com.iot.smarthome.mqtt.MqttPublisherService;
import com.iot.smarthome.mqtt.MqttSubscriberService;
import com.iot.smarthome.repository.DeviceActionRepository;
import com.iot.smarthome.repository.DeviceRepository;
import com.iot.smarthome.repository.UserRepository;
import com.iot.smarthome.service.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final MqttPublisherService mqttPublisher;
    private final MqttSubscriberService mqttSubscriber;
    private final DeviceRepository deviceRepository;
    private final DeviceActionRepository deviceActionRepository;
    private final UserRepository userRepository;

    @Value("${mqtt.topic.device.state.led1}")
    private String topicLed1State;

    @Value("${mqtt.topic.device.state.led2}")
    private String topicLed2State;

    @Value("${mqtt.topic.device.state.led3}")
    private String topicLed3State;

    private static final Map<String, String> DEVICE_DISPLAY_NAMES = Map.of(
        "light", "Đèn",
        "fan",   "Quạt",
        "ac",    "Điều hòa",
        "all",   "Tất cả thiết bị"
    );

    /**
     * Gửi lệnh điều khiển bật/tắt thiết bị qua MQTT và lưu vết vào cơ sở dữ liệu
     *
     * @param request Chứa mã thiết bị ('light', 'fan', 'ac') và lệnh ('ON', 'OFF')
     * @param performedBy Tên tài khoản người thực hiện lệnh
     */
    @Override
    @Transactional
    public void sendCommand(DeviceCommandRequest request, String performedBy) {
        String deviceKey = request.getDevice().toLowerCase();
        String command   = request.getCommand().toUpperCase();

        String deviceDisplayName = DEVICE_DISPLAY_NAMES.getOrDefault(deviceKey, deviceKey);
        String status = "Thành công";

        try {
            mqttPublisher.sendCommand(deviceKey, command);
        } catch (MqttException e) {
            log.error("Gửi lệnh MQTT thất bại: {}", e.getMessage());
            status = "Thất bại";
        } catch (IllegalArgumentException e) {
            log.warn("Lệnh không hợp lệ: {}", e.getMessage());
            throw e;
        }

        // Tìm thiết bị trong DB hoặc tự động tạo nếu chưa có
        Device device = deviceRepository.findByName(deviceDisplayName)
                .orElseGet(() -> deviceRepository.save(Device.builder()
                        .name(deviceDisplayName)
                        .status(command)
                        .build()));

        // Tìm user thực hiện lệnh
        User user = (performedBy != null) ? userRepository.findByUsername(performedBy).orElse(null) : null;

        if ("Thành công".equals(status)) {
            device.setStatus(command);
            deviceRepository.save(device);

            // Cập nhật ngay cache trạng thái để phản hồi tức thì
            String stateTopic = switch (deviceKey) {
                case "light" -> topicLed1State;
                case "fan"   -> topicLed2State;
                case "ac"    -> topicLed3State;
                default      -> null;
            };
            if (stateTopic != null) {
                mqttSubscriber.updateDeviceState(stateTopic, command);
            }
        }

        // Lưu vào bảng device_action
        DeviceAction actionRecord = DeviceAction.builder()
                .device(device)
                .user(user)
                .action(command.equals("ON") ? "Bật" : "Tắt")
                .status(status)
                .time(LocalDateTime.now())
                .build();

        deviceActionRepository.save(actionRecord);
    }

    /**
     * Lấy trạng thái hiện tại của tất cả các thiết bị ('ON' hoặc 'OFF')
     *
     * @return Map ánh xạ mã thiết bị sang trạng thái chuỗi
     */
    @Override
    public Map<String, String> getCurrentStates() {
        Map<String, String> allStates = mqttSubscriber.getAllDeviceStates();
        Map<String, String> result = new HashMap<>();

        result.put("light", resolveDeviceState("Đèn", topicLed1State, allStates));
        result.put("fan",   resolveDeviceState("Quạt", topicLed2State, allStates));
        result.put("ac",    resolveDeviceState("Điều hòa", topicLed3State, allStates));
        return result;
    }

    /**
     * Xác định trạng thái của một thiết bị từ cache MQTT hoặc fallback DB
     *
     * @param deviceName Tên thiết bị
     * @param stateTopic Topic trạng thái MQTT
     * @param allStates Map toàn bộ cache trạng thái thiết bị
     * @return Chuỗi 'ON' hoặc 'OFF'
     */
    private String resolveDeviceState(String deviceName, String stateTopic, Map<String, String> allStates) {
        String state = allStates.get(stateTopic);
        if (state != null) {
            return state;
        }
        return deviceRepository.findByName(deviceName)
                .map(Device::getStatus)
                .orElse("OFF");
    }

    /**
     * Tìm kiếm và phân trang lịch sử thao tác bật/tắt thiết bị
     *
     * @param request Các tiêu chí tìm kiếm, lọc theo thiết bị, hành động, thời gian và phân trang
     * @return PageResponse chứa danh sách lịch sử và thông tin phân trang
     */
    @Override
    public PageResponse<DeviceHistoryResponse> searchHistory(HistorySearchRequest request) {
        String sortBy = request.getSortBy();
        if (sortBy == null || "createdAt".equalsIgnoreCase(sortBy) || "time".equalsIgnoreCase(sortBy)) {
            sortBy = "time";
        } else if ("deviceName".equalsIgnoreCase(sortBy) || "device".equalsIgnoreCase(sortBy)) {
            sortBy = "device.name";
        }

        Sort sort = request.getSortDir().equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        PageRequest pageRequest = PageRequest.of(request.getPage(), request.getSize(), sort);

        String deviceName = blank(request.getDeviceName());
        String action     = blank(request.getAction());
        String status     = blank(request.getStatus());

        Page<DeviceAction> page = deviceActionRepository.search(
                deviceName, action, status, request.getFrom(), request.getTo(), pageRequest);

        return PageResponse.<DeviceHistoryResponse>builder()
                .content(page.getContent().stream().map(this::toResponse).toList())
                .currentPage(page.getNumber())
                .totalPages(page.getTotalPages())
                .totalItems(page.getTotalElements())
                .pageSize(page.getSize())
                .build();
    }

    /**
     * Chuyển đổi Entity DeviceAction sang DTO DeviceHistoryResponse trả về cho Client
     *
     * @param a Đối tượng DeviceAction Entity
     * @return DeviceHistoryResponse
     */
    private DeviceHistoryResponse toResponse(DeviceAction a) {
        String performer = a.getUser() != null ? a.getUser().getUsername() : "Hệ thống";
        String devName = a.getDevice() != null ? a.getDevice().getName() : "Thiết bị";

        return DeviceHistoryResponse.builder()
                .id(Long.valueOf(a.getId()))
                .deviceName(devName)
                .action(a.getAction())
                .status(a.getStatus())
                .performedBy(performer)
                .createdAt(a.getTime())
                .build();
    }

    /**
     * Chuẩn hóa chuỗi tìm kiếm, chuyển chuỗi rỗng hoặc chỉ có khoảng trắng thành null
     *
     * @param s Chuỗi đầu vào
     * @return Chuỗi hợp lệ hoặc null
     */
    private String blank(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
