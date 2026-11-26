package com.oursecurity.visitor;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VisitRepository extends JpaRepository<Visit, Long> {

    List<Visit> findAllByOrderByCreatedAtDesc();

    List<Visit> findByStatusOrderByCreatedAtDesc(VisitStatus status);

    List<Visit> findByFlatNumberIgnoreCaseOrderByCreatedAtDesc(String flatNumber);

    List<Visit> findByFlatNumberIgnoreCaseAndStatusOrderByCreatedAtDesc(String flatNumber, VisitStatus status);

    Optional<Visit> findFirstByPassCodeAndStatusIn(String passCode, Collection<VisitStatus> statuses);

    boolean existsByPassCodeAndStatusIn(String passCode, Collection<VisitStatus> statuses);

    long countByStatus(VisitStatus status);

    long countByCheckedInAtGreaterThanEqual(Instant since);
}
