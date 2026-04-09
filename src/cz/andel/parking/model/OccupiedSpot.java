package cz.andel.parking.model;

public record OccupiedSpot(int spotNumber, ParkingRecord record, int priority) {
}
