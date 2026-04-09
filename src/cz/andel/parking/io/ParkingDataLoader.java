package cz.andel.parking.io;

import cz.andel.parking.model.ParkingRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class ParkingDataLoader {
    private ParkingDataLoader() {
    }

    public static List<ParkingImportRow> loadFromCsv(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return parseRows(reader);
        }
    }

    public static List<ParkingImportRow> loadFromStream(InputStream stream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return parseRows(reader);
        }
    }

    private static List<ParkingImportRow> parseRows(BufferedReader reader) throws IOException {
        List<ParkingImportRow> rows = new ArrayList<>();
        String line;
        int lineNumber = 0;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            String[] parts = trimmed.split(",");
            if (parts.length < 5) {
                throw new IllegalArgumentException("CSV line " + lineNumber + " must have 5 columns: floor,spot,spz,owner,type");
            }

            int floor = Integer.parseInt(parts[0].trim());
            int spot = Integer.parseInt(parts[1].trim());
            String licensePlate = parts[2].trim();
            String owner = parts[3].trim();
            String vehicleType = parts[4].trim();

            ParkingRecord record = new ParkingRecord(licensePlate, owner, vehicleType, LocalDateTime.now());
            rows.add(new ParkingImportRow(floor, spot, record));
        }
        return rows;
    }
}
