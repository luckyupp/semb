package cz.andel.parking.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ParkingRecord {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String licensePlate;
    private final String ownerName;
    private final String vehicleType;
    private final LocalDateTime occupiedAt;

    public ParkingRecord(String licensePlate, String ownerName, String vehicleType) {
        this(licensePlate, ownerName, vehicleType, LocalDateTime.now());
    }

    public ParkingRecord(String licensePlate, String ownerName, String vehicleType, LocalDateTime occupiedAt) {
        this.licensePlate = licensePlate;
        this.ownerName = ownerName;
        this.vehicleType = vehicleType;
        this.occupiedAt = occupiedAt;
    }

    public String licensePlate() {
        return licensePlate;
    }

    public String ownerName() {
        return ownerName;
    }

    public String vehicleType() {
        return vehicleType;
    }

    public LocalDateTime occupiedAt() {
        return occupiedAt;
    }

    @Override
    public String toString() {
        return "SPZ=" + licensePlate
                + ", owner=" + ownerName
                + ", type=" + vehicleType
                + ", occupiedAt=" + occupiedAt.format(FORMATTER);
    }
}
