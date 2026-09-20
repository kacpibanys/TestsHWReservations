package com.example.reservation;

public interface EventPublisher {

    void publish(String eventType, String details);
}