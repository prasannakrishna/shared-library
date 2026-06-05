package com.bhagwat.scm.kafka.exception;

/**
 * Thrown when a Kafka message cannot be published to the broker.
 */
public class KafkaPublishException extends RuntimeException {

    public KafkaPublishException(String message) {
        super(message);
    }

    public KafkaPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
