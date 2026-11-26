package com.oursecurity.alert;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findAllByOrderByCreatedAtDesc();

    List<Alert> findByStatusOrderByCreatedAtDesc(AlertStatus status);

    List<Alert> findByRaisedByIdOrderByCreatedAtDesc(Long userId);

    List<Alert> findByRaisedByIdAndStatusOrderByCreatedAtDesc(Long userId, AlertStatus status);

    long countByStatusIn(Collection<AlertStatus> statuses);
}
