package cz.andel.parking.ui;

import cz.andel.parking.io.ParkingDataLoader;
import cz.andel.parking.io.ParkingImportRow;
import cz.andel.parking.model.NearestFreeSpotResult;
import cz.andel.parking.model.OccupiedSpot;
import cz.andel.parking.model.OperationResult;
import cz.andel.parking.model.ParkingRecord;
import cz.andel.parking.service.ParkingGarageService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class ParkingGarageFrame extends JFrame {
    private final ParkingGarageService garageService;

    private final JComboBox<Integer> floorCombo;
    private final JTextField spotField;
    private final JTextField plateField;
    private final JTextField ownerField;
    private final JTextField typeField;
    private final JTextField priorityRangeField;
    private final JTextArea logArea;
    private final JTextArea floorStateArea;
    private final ParkingHousePanel parkingHousePanel;
    private final TreapVisualizationPanel treapVisualizationPanel;

    public ParkingGarageFrame(ParkingGarageService garageService) {
        super("Treap Parking Visual Lab");
        this.garageService = garageService;

        floorCombo = new JComboBox<>(garageService.floorNumbers().toArray(new Integer[0]));
        spotField = new JTextField(8);
        plateField = new JTextField(10);
        ownerField = new JTextField(12);
        typeField = new JTextField(8);
        priorityRangeField = new JTextField(5);
        priorityRangeField.setText(String.valueOf(garageService.priorityBound()));
        logArea = createTextArea();
        floorStateArea = createTextArea();
        parkingHousePanel = new ParkingHousePanel(garageService, this::selectedFloor);
        treapVisualizationPanel = new TreapVisualizationPanel(
            () -> garageService.floorTreapLayout(selectedFloor()),
            () -> garageService.floorLastTreapEvents(selectedFloor())
        );

        buildUi();
        installDefaultDataIfPresent();
        refreshFloorViews();
    }

    private void buildUi() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1440, 860);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBackground(new Color(243, 246, 252));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(createInputPanel(), BorderLayout.NORTH);
        root.add(createCenterPanel(), BorderLayout.CENTER);
        root.add(createButtonPanel(), BorderLayout.SOUTH);
        add(root, BorderLayout.CENTER);
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Control Deck"));
        panel.setBackground(new Color(250, 252, 255));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.anchor = GridBagConstraints.WEST;
        Font labelFont = new Font("Segoe UI", Font.BOLD, 12);

        c.gridx = 0;
        c.gridy = 0;
        panel.add(styledLabel("Floor:", labelFont), c);
        c.gridx = 1;
        panel.add(floorCombo, c);

        c.gridx = 2;
        panel.add(styledLabel("Spot:", labelFont), c);
        c.gridx = 3;
        panel.add(spotField, c);

        c.gridx = 4;
        panel.add(styledLabel("License Plate:", labelFont), c);
        c.gridx = 5;
        panel.add(plateField, c);

        c.gridx = 6;
        panel.add(styledLabel("Owner:", labelFont), c);
        c.gridx = 7;
        panel.add(ownerField, c);

        c.gridx = 8;
        panel.add(styledLabel("Vehicle Type:", labelFont), c);
        c.gridx = 9;
        panel.add(typeField, c);

        c.gridx = 10;
        panel.add(styledLabel("Priority range:", labelFont), c);
        c.gridx = 11;
        panel.add(priorityRangeField, c);

        floorCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        styleInputField(spotField);
        styleInputField(plateField);
        styleInputField(ownerField);
        styleInputField(typeField);
        styleInputField(priorityRangeField);
        return panel;
    }

    private JPanel createCenterPanel() {
        JSplitPane visualSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        visualSplit.setResizeWeight(0.62);
        visualSplit.setTopComponent(wrapComponent("Parking House Overview", parkingHousePanel));
        visualSplit.setBottomComponent(wrapComponent("Treap Tree (Selected Floor)", treapVisualizationPanel));

        JTabbedPane dataTabs = new JTabbedPane();
        dataTabs.setFont(new Font("Segoe UI", Font.BOLD, 12));
        dataTabs.addTab("Event Log", wrap("Operation Log", logArea));
        dataTabs.addTab("Occupied List", wrap("Occupied Spots (Sorted)", floorStateArea));

        JSplitPane horizontal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        horizontal.setResizeWeight(0.74);
        horizontal.setLeftComponent(visualSplit);
        horizontal.setRightComponent(dataTabs);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(2, 2, 2, 2));
        panel.add(horizontal, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        panel.setBackground(new Color(246, 248, 252));

        JButton occupyButton = new JButton("Occupy Spot");
        occupyButton.addActionListener(e -> onOccupy());

        JButton releaseButton = new JButton("Release Spot");
        releaseButton.addActionListener(e -> onRelease());

        JButton releaseAllButton = new JButton("Release All (Floor)");
        releaseAllButton.addActionListener(e -> onReleaseAll());

        JButton checkButton = new JButton("Check Occupied");
        checkButton.addActionListener(e -> onCheckOccupied());

        JButton nearestButton = new JButton("Find Nearest Free");
        nearestButton.addActionListener(e -> onFindNearestFree());

        JButton listButton = new JButton("Refresh Floor View");
        listButton.addActionListener(e -> refreshFloorViews());

        JButton loadCsvButton = new JButton("Load CSV");
        loadCsvButton.addActionListener(e -> onLoadCsv());

        JButton loadDemoButton = new JButton("Load Demo Data");
        loadDemoButton.addActionListener(e -> onLoadDemoData());

        JButton runScenariosButton = new JButton("Run Scenarios 1-6");
        runScenariosButton.addActionListener(e -> onRunScenarios());

        JButton clearLogButton = new JButton("Clear Log");
        clearLogButton.addActionListener(e -> logArea.setText(""));

        JButton applyPriorityRangeButton = new JButton("Apply Priority Range");
        applyPriorityRangeButton.addActionListener(e -> onApplyPriorityRange());

        floorCombo.addActionListener(e -> refreshFloorViews());

        panel.add(occupyButton);
        panel.add(releaseButton);
        panel.add(releaseAllButton);
        panel.add(checkButton);
        panel.add(nearestButton);
        panel.add(listButton);
        panel.add(loadCsvButton);
        panel.add(loadDemoButton);
        panel.add(runScenariosButton);
        panel.add(applyPriorityRangeButton);
        panel.add(clearLogButton);
        return panel;
    }

    private JScrollPane wrap(String title, JTextArea area) {
        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setBorder(BorderFactory.createTitledBorder(title));
        scrollPane.setPreferredSize(new Dimension(300, 220));
        return scrollPane;
    }

    private JPanel wrapComponent(String title, JComponent component) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(component, BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(300, 220));
        return panel;
    }

    private JTextArea createTextArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("Consolas", Font.PLAIN, 13));
        area.setBackground(new Color(252, 253, 255));
        area.setBorder(new EmptyBorder(8, 8, 8, 8));
        return area;
    }

    private JLabel styledLabel(String text, Font font) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(new Color(58, 67, 84));
        return label;
    }

    private void styleInputField(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(198, 204, 218)),
                new EmptyBorder(3, 6, 3, 6)
        ));
    }

    private int selectedFloor() {
        Object selected = floorCombo.getSelectedItem();
        return selected == null ? 1 : (Integer) selected;
    }

    private Integer typedSpot() {
        try {
            return Integer.parseInt(spotField.getText().trim());
        } catch (NumberFormatException ex) {
            appendLog("[ERR] Invalid spot number. Enter integer.");
            return null;
        }
    }

    private void onOccupy() {
        Integer spot = typedSpot();
        if (spot == null) {
            return;
        }
        String plate = plateField.getText().trim();
        String owner = ownerField.getText().trim();
        String type = typeField.getText().trim();

        if (plate.isEmpty() || owner.isEmpty() || type.isEmpty()) {
            appendLog("[ERR] Plate, owner and vehicle type must be filled.");
            return;
        }

        OperationResult result = garageService.occupySpot(
                selectedFloor(),
                spot,
                new ParkingRecord(plate, owner, type)
        );
        printResult(result);
        refreshFloorViews();
    }

    private void onRelease() {
        Integer spot = typedSpot();
        if (spot == null) {
            return;
        }
        OperationResult result = garageService.releaseSpot(selectedFloor(), spot);
        printResult(result);
        refreshFloorViews();
    }
    
    private void onReleaseAll() {
        OperationResult result = garageService.releaseAllOnFloor(selectedFloor());
        printResult(result);
        refreshFloorViews();
    }

    private void onCheckOccupied() {
        Integer spot = typedSpot();
        if (spot == null) {
            return;
        }
        boolean occupied = garageService.isOccupied(selectedFloor(), spot);
        appendLog(occupied
                ? "[OK] Spot " + spot + " is occupied."
                : "[OK] Spot " + spot + " is free.");
    }

    private void onFindNearestFree() {
        Integer spot = typedSpot();
        if (spot == null) {
            return;
        }
        NearestFreeSpotResult result = garageService.findNearestFreeSpot(selectedFloor(), spot);
        if (!result.success()) {
            appendLog("[ERR] " + result.message());
            return;
        }
        appendLog("[OK] " + result.message() + " (requested=" + result.requestedSpot() + ", free=" + result.freeSpot() + ")");
    }

    private void onLoadCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select CSV file");
        chooser.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path path = chooser.getSelectedFile().toPath();
        try {
            List<ParkingImportRow> rows = ParkingDataLoader.loadFromCsv(path);
            String importSummary = garageService.importRows(rows);
            appendLog("[OK] CSV loaded from " + path);
            appendLog(importSummary);
            refreshFloorViews();
        } catch (Exception ex) {
            appendLog("[ERR] CSV load failed: " + ex.getMessage());
        }
    }

    private void onLoadDemoData() {
        try {
            Path path = Path.of("data", "sample_occupancy.csv");
            List<ParkingImportRow> rows = ParkingDataLoader.loadFromCsv(path);
            appendLog("[OK] Demo data loaded from " + path);
            appendLog(garageService.importRows(rows));
            refreshFloorViews();
        } catch (Exception ex) {
            appendLog("[ERR] Demo data load failed: " + ex.getMessage());
        }
    }

    private void onRunScenarios() {
        appendLog("=== Mandatory scenarios ===");
        appendLog(garageService.runMandatoryScenarioDemo());
        refreshFloorViews();
    }

    private void onApplyPriorityRange() {
        String text = priorityRangeField.getText().trim();
        int bound;
        try {
            bound = Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            appendLog("[ERR] Priority range must be an integer >= 2.");
            return;
        }

        OperationResult result = garageService.setPriorityBound(bound);
        printResult(result);
        if (result.success()) {
            appendLog("[INFO] New random priority range is now <0.." + (bound - 1) + ">.");
        }
    }

    private void printResult(OperationResult result) {
        appendLog((result.success() ? "[OK] " : "[ERR] ") + result.message());
        for (String detail : result.details()) {
            appendLog("  - " + detail);
        }
    }

    private void refreshFloorViews() {
        int floor = selectedFloor();
        List<OccupiedSpot> spots = garageService.listOccupiedSpotsSorted(floor);
        if (spots.isEmpty()) {
            floorStateArea.setText("(No occupied spots)");
        } else {
            StringBuilder builder = new StringBuilder();
            for (OccupiedSpot spot : spots) {
                builder.append("Spot ").append(spot.spotNumber())
                        .append(" | priority=").append(spot.priority())
                        .append(" | ").append(spot.record())
                        .append(System.lineSeparator());
            }
            floorStateArea.setText(builder.toString());
        }
        parkingHousePanel.repaint();
        treapVisualizationPanel.repaint();
    }

    private void appendLog(String line) {
        logArea.append(line + System.lineSeparator());
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void installDefaultDataIfPresent() {
        Path demoPath = Path.of("data", "sample_occupancy.csv");
        if (!demoPath.toFile().exists()) {
            appendLog("[INFO] Demo data file not found: " + demoPath);
            appendLog("[INFO] Current model: " + garageService.minimalScopeStatus());
            return;
        }

        try {
            List<ParkingImportRow> rows = ParkingDataLoader.loadFromCsv(demoPath);
            appendLog("[OK] Auto-loaded demo data from " + demoPath);
            appendLog(garageService.importRows(rows));
            appendLog("[INFO] Current model: " + garageService.minimalScopeStatus());
        } catch (IOException ex) {
            appendLog("[ERR] Failed to auto-load demo data: " + ex.getMessage());
        }
    }

    public static void launch(ParkingGarageService service) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            ParkingGarageFrame frame = new ParkingGarageFrame(service);
            frame.setVisible(true);
        });
    }
}
