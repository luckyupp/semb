package cz.andel.parking.io;

import cz.andel.parking.model.ParkingRecord;

public record ParkingImportRow(int floorNumber, int spotNumber, ParkingRecord record) {
}
