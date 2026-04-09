package cz.andel.parking.service;

import cz.andel.ds.Treap;
import cz.andel.parking.model.NearestFreeSpotResult;
import cz.andel.parking.model.OccupiedSpot;
import cz.andel.parking.model.OperationResult;
import cz.andel.parking.model.ParkingRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ParkingFloorService {
    private final int floorNumber;
    private final int capacity;
    private final Treap<Integer, ParkingRecord> occupiedTreap;

    public ParkingFloorService(int floorNumber, int capacity) {
        this.floorNumber = floorNumber;
        this.capacity = capacity;
        this.occupiedTreap = new Treap<>();
    }

    public int floorNumber() {
        return floorNumber;
    }

    public int capacity() {
        return capacity;
    }

    public OperationResult occupySpot(int spotNumber, ParkingRecord record) {
        return occupySpotInternal(spotNumber, record, null);
    }

    public OperationResult occupySpotWithPriorityForDemo(int spotNumber, ParkingRecord record, int forcedPriority) {
        return occupySpotInternal(spotNumber, record, forcedPriority);
    }

    private OperationResult occupySpotInternal(int spotNumber, ParkingRecord record, Integer forcedPriority) {
        List<String> details = new ArrayList<>();
        if (!isSpotInRange(spotNumber)) {
            return OperationResult.failure(
                    "Spot " + spotNumber + " is out of range <1.." + capacity + ">",
                    details
            );
        }

        if (occupiedTreap.containsKey(spotNumber)) {
            details.add("Occupy failed: spot " + spotNumber + " is already occupied.");
            return OperationResult.failure("Spot " + spotNumber + " is already occupied.", details);
        }

        boolean inserted = forcedPriority == null
                ? occupiedTreap.insert(spotNumber, record)
                : occupiedTreap.insertWithPriority(spotNumber, record, forcedPriority);
        appendTreapDetails(details);

        if (!inserted) {
            return OperationResult.failure("Failed to occupy spot " + spotNumber, details);
        }

        return OperationResult.success(
                "Spot " + spotNumber + " on floor " + floorNumber + " occupied successfully.",
                details
        );
    }

    public OperationResult releaseSpot(int spotNumber) {
        List<String> details = new ArrayList<>();
        if (!isSpotInRange(spotNumber)) {
            return OperationResult.failure(
                    "Spot " + spotNumber + " is out of range <1.." + capacity + ">",
                    details
            );
        }

        if (!occupiedTreap.containsKey(spotNumber)) {
            details.add("Release failed: spot " + spotNumber + " is not occupied.");
            return OperationResult.failure("Spot " + spotNumber + " is not occupied.", details);
        }

        boolean deleted = occupiedTreap.delete(spotNumber);
        appendTreapDetails(details);

        if (!deleted) {
            return OperationResult.failure("Failed to release spot " + spotNumber, details);
        }

        return OperationResult.success(
                "Spot " + spotNumber + " on floor " + floorNumber + " released successfully.",
                details
        );
    }

    public boolean isOccupied(int spotNumber) {
        return isSpotInRange(spotNumber) && occupiedTreap.containsKey(spotNumber);
    }

    public List<OccupiedSpot> listOccupiedSpotsSorted() {
        List<OccupiedSpot> spots = new ArrayList<>();
        for (Map.Entry<Integer, ParkingRecord> entry : occupiedTreap.inOrder()) {
            int priority = occupiedTreap.priorityOf(entry.getKey()).orElse(-1);
            spots.add(new OccupiedSpot(entry.getKey(), entry.getValue(), priority));
        }
        return spots;
    }

    public NearestFreeSpotResult findNearestFreeSpot(int requestedSpot) {
        if (!isSpotInRange(requestedSpot)) {
            return NearestFreeSpotResult.failure("Requested spot " + requestedSpot + " is out of range <1.." + capacity + ">");
        }

        if (!occupiedTreap.containsKey(requestedSpot)) {
            return NearestFreeSpotResult.success(
                    requestedSpot,
                    requestedSpot,
                    "Requested spot " + requestedSpot + " is already free."
            );
        }

        int leftBoundary = requestedSpot;
        int rightBoundary = requestedSpot;

        while (true) {
            Optional<Integer> predecessor = occupiedTreap.predecessor(leftBoundary);
            if (predecessor.isPresent() && predecessor.get() == leftBoundary - 1) {
                leftBoundary = predecessor.get();
            } else {
                break;
            }
        }

        while (true) {
            Optional<Integer> successor = occupiedTreap.successor(rightBoundary);
            if (successor.isPresent() && successor.get() == rightBoundary + 1) {
                rightBoundary = successor.get();
            } else {
                break;
            }
        }

        Integer leftCandidate = leftBoundary > 1 ? leftBoundary - 1 : null;
        Integer rightCandidate = rightBoundary < capacity ? rightBoundary + 1 : null;

        if (leftCandidate == null && rightCandidate == null) {
            return NearestFreeSpotResult.failure("No free spot available on floor " + floorNumber);
        }
        if (leftCandidate == null) {
            return NearestFreeSpotResult.success(
                    requestedSpot,
                    rightCandidate,
                    "Nearest free spot is " + rightCandidate + " (right side of occupied block)."
            );
        }
        if (rightCandidate == null) {
            return NearestFreeSpotResult.success(
                    requestedSpot,
                    leftCandidate,
                    "Nearest free spot is " + leftCandidate + " (left side of occupied block)."
            );
        }

        int leftDistance = requestedSpot - leftCandidate;
        int rightDistance = rightCandidate - requestedSpot;

        if (leftDistance <= rightDistance) {
            return NearestFreeSpotResult.success(
                    requestedSpot,
                    leftCandidate,
                    "Nearest free spot is " + leftCandidate + " (tie resolved to lower spot number)."
            );
        }

        return NearestFreeSpotResult.success(
                requestedSpot,
                rightCandidate,
                "Nearest free spot is " + rightCandidate + "."
        );
    }

    public String treapSnapshot() {
        return occupiedTreap.levelOrderSnapshot();
    }

    public List<List<Optional<Map.Entry<Integer, Integer>>>> treapLayout() {
        return occupiedTreap.levelLayout();
    }

    public List<String> lastTreapEvents() {
        return occupiedTreap.lastOperationEvents();
    }

    public int occupiedCount() {
        return occupiedTreap.size();
    }

    public int priorityBound() {
        return occupiedTreap.priorityBound();
    }

    public void setPriorityBound(int priorityBound) {
        occupiedTreap.setPriorityBound(priorityBound);
    }

    private boolean isSpotInRange(int spotNumber) {
        return spotNumber >= 1 && spotNumber <= capacity;
    }

    private void appendTreapDetails(List<String> details) {
        details.addAll(occupiedTreap.lastOperationEvents());
        details.add("Invariant check: " + occupiedTreap.lastInvariantMessage());
    }
}
