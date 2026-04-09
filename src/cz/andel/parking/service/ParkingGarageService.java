package cz.andel.parking.service;

import cz.andel.parking.io.ParkingImportRow;
import cz.andel.parking.model.NearestFreeSpotResult;
import cz.andel.parking.model.OccupiedSpot;
import cz.andel.parking.model.OperationResult;
import cz.andel.parking.model.ParkingRecord;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ParkingGarageService {
    private final int floorCount;
    private final int spotsPerFloor;
    private final Map<Integer, ParkingFloorService> floors;

    public ParkingGarageService(int floorCount, int spotsPerFloor) {
        if (floorCount < 1) {
            throw new IllegalArgumentException("Garage must have at least one floor.");
        }
        if (spotsPerFloor < 1) {
            throw new IllegalArgumentException("Each floor must have at least one spot.");
        }

        this.floorCount = floorCount;
        this.spotsPerFloor = spotsPerFloor;
        this.floors = new LinkedHashMap<>();
        for (int i = 1; i <= floorCount; i++) {
            floors.put(i, new ParkingFloorService(i, spotsPerFloor));
        }
    }

    public int floorCount() {
        return floorCount;
    }

    public int spotsPerFloor() {
        return spotsPerFloor;
    }

    public int priorityBound() {
        ParkingFloorService floor = getFloorOrNull(1);
        return floor == null ? 50 : floor.priorityBound();
    }

    public OperationResult setPriorityBound(int priorityBound) {
        if (priorityBound < 2) {
            return OperationResult.failure("Priority range must be at least 2.", List.of());
        }

        for (ParkingFloorService floor : floors.values()) {
            floor.setPriorityBound(priorityBound);
        }
        return OperationResult.success(
                "Treap random priority range updated to <0.." + (priorityBound - 1) + "> for all floors.",
                List.of()
        );
    }

    public List<Integer> floorNumbers() {
        return new ArrayList<>(floors.keySet());
    }

    public OperationResult occupySpot(int floorNumber, int spotNumber, ParkingRecord record) {
        ParkingFloorService floor = getFloorOrNull(floorNumber);
        if (floor == null) {
            return OperationResult.failure("Floor " + floorNumber + " does not exist.", List.of());
        }
        return floor.occupySpot(spotNumber, record);
    }

    public OperationResult releaseSpot(int floorNumber, int spotNumber) {
        ParkingFloorService floor = getFloorOrNull(floorNumber);
        if (floor == null) {
            return OperationResult.failure("Floor " + floorNumber + " does not exist.", List.of());
        }
        return floor.releaseSpot(spotNumber);
    }

    public OperationResult releaseAllOnFloor(int floorNumber) {
        ParkingFloorService floor = getFloorOrNull(floorNumber);
        if (floor == null) {
            return OperationResult.failure("Floor " + floorNumber + " does not exist.", List.of());
        }

        List<OccupiedSpot> occupied = floor.listOccupiedSpotsSorted();
        if (occupied.isEmpty()) {
            return OperationResult.success("No occupied spots to release on floor " + floorNumber + ".", List.of());
        }

        List<String> details = new ArrayList<>();
        int released = 0;
        for (OccupiedSpot spot : occupied) {
            OperationResult result = floor.releaseSpot(spot.spotNumber());
            if (result.success()) {
                released++;
            }
            details.addAll(result.details());
        }

        return OperationResult.success(
                "Released " + released + " occupied spots on floor " + floorNumber + ".",
                details
        );
    }

    public boolean isOccupied(int floorNumber, int spotNumber) {
        ParkingFloorService floor = getFloorOrNull(floorNumber);
        return floor != null && floor.isOccupied(spotNumber);
    }

    public List<OccupiedSpot> listOccupiedSpotsSorted(int floorNumber) {
        ParkingFloorService floor = getFloorOrNull(floorNumber);
        if (floor == null) {
            return List.of();
        }
        return floor.listOccupiedSpotsSorted();
    }

    public NearestFreeSpotResult findNearestFreeSpot(int floorNumber, int requestedSpot) {
        ParkingFloorService floor = getFloorOrNull(floorNumber);
        if (floor == null) {
            return NearestFreeSpotResult.failure("Floor " + floorNumber + " does not exist.");
        }
        return floor.findNearestFreeSpot(requestedSpot);
    }

    public String floorTreapSnapshot(int floorNumber) {
        ParkingFloorService floor = getFloorOrNull(floorNumber);
        return floor == null ? "Floor does not exist." : floor.treapSnapshot();
    }

    public List<List<Optional<Map.Entry<Integer, Integer>>>> floorTreapLayout(int floorNumber) {
        ParkingFloorService floor = getFloorOrNull(floorNumber);
        return floor == null ? List.of() : floor.treapLayout();
    }

    public List<String> floorLastTreapEvents(int floorNumber) {
        ParkingFloorService floor = getFloorOrNull(floorNumber);
        return floor == null ? List.of() : floor.lastTreapEvents();
    }

    public String importRows(List<ParkingImportRow> rows) {
        int successCount = 0;
        List<String> failures = new ArrayList<>();

        for (ParkingImportRow row : rows) {
            OperationResult result = occupySpot(row.floorNumber(), row.spotNumber(), row.record());
            if (result.success()) {
                successCount++;
            } else {
                failures.add(
                        "floor=" + row.floorNumber()
                                + ", spot=" + row.spotNumber()
                                + " -> " + result.message()
                );
            }
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Imported ").append(successCount).append(" records.");
        if (!failures.isEmpty()) {
            builder.append(System.lineSeparator())
                    .append("Skipped ").append(failures.size()).append(" records:")
                    .append(System.lineSeparator())
                    .append(String.join(System.lineSeparator(), failures));
        }
        return builder.toString();
    }

    public int totalOccupiedCount() {
        int total = 0;
        for (ParkingFloorService floor : floors.values()) {
            total += floor.occupiedCount();
        }
        return total;
    }

    public String minimalScopeStatus() {
        return "floors=" + floorCount
                + ", spotsPerFloor=" + spotsPerFloor
                + ", totalOccupied=" + totalOccupiedCount();
    }

    public String runMandatoryScenarioDemo() {
        StringBuilder out = new StringBuilder();
        int floor = 1;
        int rotationFloor = 2;

        int clearedMain = clearFloorForDemo(floor);
        int clearedRotation = clearFloorForDemo(rotationFloor);
        out.append("Scenario setup: cleared ").append(clearedMain)
            .append(" spots on floor ").append(floor)
            .append(" and ").append(clearedRotation)
            .append(" spots on floor ").append(rotationFloor)
            .append(".")
            .append(System.lineSeparator())
            .append(System.lineSeparator());

        out.append("Scenario 1: successful occupation of free spot").append(System.lineSeparator());
        OperationResult s1 = occupySpot(floor, 1, new ParkingRecord("1AA-0001", "Demo Driver 1", "CAR"));
        appendResult(out, s1);

        out.append(System.lineSeparator()).append("Scenario 2: attempt to occupy already occupied spot").append(System.lineSeparator());
        OperationResult s2 = occupySpot(floor, 1, new ParkingRecord("1AA-0002", "Demo Driver 2", "CAR"));
        appendResult(out, s2);

        out.append(System.lineSeparator()).append("Scenario 3: release existing occupation").append(System.lineSeparator());
        OperationResult s3 = releaseSpot(floor, 1);
        appendResult(out, s3);

        out.append(System.lineSeparator()).append("Scenario 4: list occupied spots in sorted order").append(System.lineSeparator());
        occupySpot(floor, 4, new ParkingRecord("1AA-0004", "Demo Driver 4", "CAR"));
        occupySpot(floor, 2, new ParkingRecord("1AA-0005", "Demo Driver 5", "CAR"));
        occupySpot(floor, 7, new ParkingRecord("1AA-0006", "Demo Driver 6", "CAR"));
        String listed = listOccupiedSpotsSorted(floor).stream()
                .map(spot -> "spot=" + spot.spotNumber() + ", " + spot.record())
                .collect(Collectors.joining(System.lineSeparator()));
        out.append(listed.isBlank() ? "(none)" : listed).append(System.lineSeparator());

        out.append(System.lineSeparator()).append("Scenario 5: find nearest free spot").append(System.lineSeparator());
        occupySpot(floor, 5, new ParkingRecord("1AA-0007", "Demo Driver 7", "CAR"));
        occupySpot(floor, 6, new ParkingRecord("1AA-0008", "Demo Driver 8", "CAR"));
        NearestFreeSpotResult s5 = findNearestFreeSpot(floor, 5);
        out.append(s5.message()).append(System.lineSeparator())
                .append("requested=").append(s5.requestedSpot())
                .append(", free=").append(s5.freeSpot())
                .append(System.lineSeparator());

        out.append(System.lineSeparator()).append("Scenario 6: show rotations and invariant preservation").append(System.lineSeparator());
        ParkingFloorService scenarioFloor = getFloorOrNull(rotationFloor);
        if (scenarioFloor != null) {
            appendResult(out, scenarioFloor.occupySpotWithPriorityForDemo(3, new ParkingRecord("2AA-1001", "Rotation A", "CAR"), 100));
            appendResult(out, scenarioFloor.occupySpotWithPriorityForDemo(2, new ParkingRecord("2AA-1002", "Rotation B", "CAR"), 300));
            appendResult(out, scenarioFloor.occupySpotWithPriorityForDemo(1, new ParkingRecord("2AA-1003", "Rotation C", "CAR"), 500));
            appendResult(out, scenarioFloor.releaseSpot(2));
            out.append("Treap snapshot floor ").append(rotationFloor).append(":").append(System.lineSeparator())
                    .append(scenarioFloor.treapSnapshot()).append(System.lineSeparator());
        }

        out.append(System.lineSeparator())
                .append("Model status: ").append(minimalScopeStatus()).append(System.lineSeparator());
        return out.toString();
    }

    private static void appendResult(StringBuilder out, OperationResult result) {
        out.append(result.success() ? "[OK] " : "[ERR] ")
                .append(result.message()).append(System.lineSeparator());
        for (String detail : result.details()) {
            out.append("  - ").append(detail).append(System.lineSeparator());
        }
    }

    private int clearFloorForDemo(int floorNumber) {
        ParkingFloorService floor = getFloorOrNull(floorNumber);
        if (floor == null) {
            return 0;
        }

        List<OccupiedSpot> occupied = floor.listOccupiedSpotsSorted();
        int released = 0;
        for (OccupiedSpot spot : occupied) {
            OperationResult result = floor.releaseSpot(spot.spotNumber());
            if (result.success()) {
                released++;
            }
        }
        return released;
    }

    private ParkingFloorService getFloorOrNull(int floorNumber) {
        return floors.get(floorNumber);
    }
}
