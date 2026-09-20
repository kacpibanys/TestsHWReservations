package com.example.reservation;

public interface ReservationRepository {

    Reservation save(Reservation reservation);

    boolean existsByRoomIdAndTimeSlot(String roomId, TimeSlot timeSlot);
}