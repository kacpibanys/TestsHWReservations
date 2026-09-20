package com.example.reservation;

public class Room {

    private final String roomId;
    private final String name;
    private final int capacity;
    private final boolean hasProjector;

    public Room(String roomId, String name, int capacity, boolean hasProjector) {
        this.roomId = roomId;
        this.name = name;
        this.capacity = capacity;
        this.hasProjector = hasProjector;
    }

    public String getRoomId() { return roomId; }
    public String getName() { return name; }
    public int getCapacity() { return capacity; }
    public boolean hasProjector() { return hasProjector; }
}