package com.oursecurity.delivery;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    List<Delivery> findAllByOrderByReceivedAtDesc();

    List<Delivery> findByStatusOrderByReceivedAtDesc(DeliveryStatus status);

    List<Delivery> findByFlatNumberIgnoreCaseOrderByReceivedAtDesc(String flatNumber);

    List<Delivery> findByFlatNumberIgnoreCaseAndStatusOrderByReceivedAtDesc(String flatNumber, DeliveryStatus status);

    long countByStatus(DeliveryStatus status);
}
