package com.example.reservation;

import java.time.LocalDateTime;

public class Reservation {

    private Long id;
    private String confirmationCode;
    private String roomId;
    private String organizerEmail;
    private TimeSlot timeSlot;
    private int attendees;
    private ReservationStatus status;
    private LocalDateTime createdAt;

    public Reservation() {
        this.status = ReservationStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    // Gettery
    public Long getId() {
        return id;
    }

    public String getConfirmationCode() {
        return confirmationCode;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getOrganizerEmail() {
        return organizerEmail;
    }

    public TimeSlot getTimeSlot() {
        return timeSlot;
    }

    public int getAttendees() {
        return attendees;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Settery
    public void setId(Long id) {
        this.id = id;
    }

    public void setConfirmationCode(String code) {
        this.confirmationCode = code;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public void setOrganizerEmail(String email) {
        this.organizerEmail = email;
    }

    public void setTimeSlot(TimeSlot timeSlot) {
        this.timeSlot = timeSlot;
    }

    public void setAttendees(int attendees) {
        this.attendees = attendees;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}