package com.iot.smarthome.repository;

import com.iot.smarthome.entity.DeviceAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface DeviceActionRepository extends JpaRepository<DeviceAction, Integer> {

    @Query("""
        SELECT a FROM DeviceAction a
        WHERE (:deviceName IS NULL OR LOWER(a.device.name) LIKE LOWER(CONCAT('%', :deviceName, '%')))
          AND (:action     IS NULL OR a.action = :action)
          AND (:status     IS NULL OR a.status = :status)
          AND (:from       IS NULL OR a.time >= :from)
          AND (:to         IS NULL OR a.time <= :to)
        """)
    Page<DeviceAction> search(
            @Param("deviceName") String deviceName,
            @Param("action")     String action,
            @Param("status")     String status,
            @Param("from")       LocalDateTime from,
            @Param("to")         LocalDateTime to,
            Pageable pageable
    );
}
