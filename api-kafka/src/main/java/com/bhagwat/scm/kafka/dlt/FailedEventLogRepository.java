package com.bhagwat.scm.kafka.dlt;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FailedEventLogRepository extends JpaRepository<FailedEventLog, Long> {
    List<FailedEventLog> findByResolutionStatusOrderByCreatedAtDesc(String status);
    List<FailedEventLog> findByServiceNameAndResolutionStatusOrderByCreatedAtDesc(String serviceName, String status);
    List<FailedEventLog> findBySourceTopicOrderByCreatedAtDesc(String sourceTopic);
}
