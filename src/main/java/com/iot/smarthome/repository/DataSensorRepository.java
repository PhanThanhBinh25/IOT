package com.iot.smarthome.repository;

import com.iot.smarthome.entity.DataSensor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DataSensorRepository extends JpaRepository<DataSensor, Integer> {

    Optional<DataSensor> findTopBySensor_TypeOrderByCreateAtDesc(String sensorType);

    Optional<DataSensor> findTopBySensor_NameOrderByCreateAtDesc(String sensorName);

    List<DataSensor> findTop7BySensor_TypeOrderByCreateAtDesc(String sensorType);

    List<DataSensor> findTop7BySensor_NameOrderByCreateAtDesc(String sensorName);

    @Query("""
        SELECT d FROM DataSensor d
        WHERE (:sensorName IS NULL OR LOWER(d.sensor.name) LIKE LOWER(CONCAT('%', :sensorName, '%')) OR LOWER(d.sensor.type) LIKE LOWER(CONCAT('%', :sensorName, '%')))
          AND (:from IS NULL OR d.createAt >= :from)
          AND (:to   IS NULL OR d.createAt <= :to)
          AND (:keyword IS NULL OR LOWER(CAST(d.value AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """)
    Page<DataSensor> search(
            @Param("sensorName") String sensorName,
            @Param("from")       LocalDateTime from,
            @Param("to")         LocalDateTime to,
            @Param("keyword")    String keyword,
            Pageable pageable
    );
}
