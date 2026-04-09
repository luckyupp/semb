package cz.andel.parking.model;

public class NearestFreeSpotResult {
    private final boolean success;
    private final Integer requestedSpot;
    private final Integer freeSpot;
    private final String message;

    private NearestFreeSpotResult(boolean success, Integer requestedSpot, Integer freeSpot, String message) {
        this.success = success;
        this.requestedSpot = requestedSpot;
        this.freeSpot = freeSpot;
        this.message = message;
    }

    public static NearestFreeSpotResult success(int requestedSpot, int freeSpot, String message) {
        return new NearestFreeSpotResult(true, requestedSpot, freeSpot, message);
    }

    public static NearestFreeSpotResult failure(String message) {
        return new NearestFreeSpotResult(false, null, null, message);
    }

    public boolean success() {
        return success;
    }

    public Integer requestedSpot() {
        return requestedSpot;
    }

    public Integer freeSpot() {
        return freeSpot;
    }

    public String message() {
        return message;
    }
}
