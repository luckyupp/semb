package cz.andel;

import cz.andel.parking.service.ParkingGarageService;
import cz.andel.parking.ui.ParkingGarageFrame;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        int floors = 4;
        int spotsPerFloor = 12;
        ParkingGarageService garageService = new ParkingGarageService(floors, spotsPerFloor);
        ParkingGarageFrame.launch(garageService);
    }
}
