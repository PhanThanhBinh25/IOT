package com.iot.smarthome.service.impl;

import com.iot.smarthome.dto.request.SensorSearchRequest;
import com.iot.smarthome.dto.response.PageResponse;
import com.iot.smarthome.dto.response.SensorDataResponse;
import com.iot.smarthome.entity.DataSensor;
import com.iot.smarthome.exception.ResourceNotFoundException;
import com.iot.smarthome.repository.DataSensorRepository;
import com.iot.smarthome.service.SensorDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SensorDataServiceImpl implements SensorDataService {

    private final DataSensorRepository dataSensorRepository;

    /**
     * Lấy bản ghi giá trị mới nhất của một loại cảm biến
     *
     * @param sensorName Loại hoặc tên cảm biến
     * @return SensorDataResponse
     */
    @Override
    public SensorDataResponse getLatest(String sensorName) {
        DataSensor data = dataSensorRepository
                .findTopBySensor_TypeOrderByCreateAtDesc(sensorName)
                .or(() -> dataSensorRepository.findTopBySensor_NameOrderByCreateAtDesc(sensorName))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Chưa có dữ liệu cho cảm biến: " + sensorName));
        return toResponse(data);
    }

    /**
     * Lấy 7 bản ghi dữ liệu cảm biến gần nhất theo thứ tự thời gian tăng dần để vẽ biểu đồ
     *
     * @param sensorName Loại cảm biến ('Nhiệt độ', 'Độ ẩm', 'Ánh sáng')
     * @return Danh sách SensorDataResponse
     */
    @Override
    public List<SensorDataResponse> getChartData(String sensorName) {
        List<DataSensor> list = new java.util.ArrayList<>(
                dataSensorRepository.findTop7BySensor_TypeOrderByCreateAtDesc(sensorName)
        );
        if (list.isEmpty()) {
            list = new java.util.ArrayList<>(
                    dataSensorRepository.findTop7BySensor_NameOrderByCreateAtDesc(sensorName)
            );
        }
        // Đảo ngược để hiển thị theo thứ tự thời gian tăng dần
        java.util.Collections.reverse(list);
        return list.stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Tìm kiếm, phân trang và sắp xếp dữ liệu lịch sử đo đạc của các cảm biến
     *
     * @param request Chứa các điều kiện lọc: loại cảm biến, khoảng thời gian, từ khóa, trang và số lượng
     * @return PageResponse chứa dữ liệu phân trang
     */
    @Override
    public PageResponse<SensorDataResponse> search(SensorSearchRequest request) {
        String sortBy = request.getSortBy();
        if (sortBy == null || "createdAt".equalsIgnoreCase(sortBy) || "time".equalsIgnoreCase(sortBy)) {
            sortBy = "createAt";
        } else if ("sensorName".equalsIgnoreCase(sortBy) || "sensor_name".equalsIgnoreCase(sortBy)) {
            sortBy = "sensor.name";
        }

        Sort sort = request.getSortDir().equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        PageRequest pageRequest = PageRequest.of(request.getPage(), request.getSize(), sort);

        String sensorName = blank(request.getSensorName());
        String keyword    = blank(request.getKeyword());

        Page<DataSensor> page = dataSensorRepository.search(
                sensorName, request.getFrom(), request.getTo(), keyword, pageRequest);

        return PageResponse.<SensorDataResponse>builder()
                .content(page.getContent().stream().map(this::toResponse).toList())
                .currentPage(page.getNumber())
                .totalPages(page.getTotalPages())
                .totalItems(page.getTotalElements())
                .pageSize(page.getSize())
                .build();
    }

    /**
     * Chuyển đổi Entity DataSensor sang DTO SensorDataResponse
     *
     * @param d Đối tượng Entity DataSensor
     * @return SensorDataResponse
     */
    private SensorDataResponse toResponse(DataSensor d) {
        String sensorType = d.getSensor() != null ? d.getSensor().getType() : "Cảm biến";
        String unit = d.getSensor() != null ? d.getSensor().getUnit() : "";
        Double value = d.getValue() != null ? Math.round(d.getValue() * 10.0) / 10.0 : null;
        String display = (value != null ? value : "") + " " + unit;

        return SensorDataResponse.builder()
                .id(Long.valueOf(d.getId()))
                .sensorName(sensorType)
                .value(value)
                .unit(unit)
                .displayValue(display.trim())
                .createdAt(d.getCreateAt())
                .build();
    }

    /**
     * Chuẩn hóa chuỗi tìm kiếm, chuyển chuỗi rỗng thành null
     *
     * @param s Chuỗi đầu vào
     * @return Chuỗi hợp lệ hoặc null
     */
    private String blank(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
