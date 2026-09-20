package com.example.reservation;

import java.time.LocalDate;
import java.time.LocalTime;

public class TimeSlot {

    private final LocalDate date;
    private final LocalTime startTime;
    private final LocalTime endTime;

    public TimeSlot(LocalDate date, LocalTime startTime, LocalTime endTime) {
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public LocalDate getDate() { return date; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
}