package cz.andel.parking.ui;

import cz.andel.parking.service.ParkingGarageService;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.function.Supplier;

public class ParkingHousePanel extends JPanel {
    private final ParkingGarageService garageService;
    private final Supplier<Integer> selectedFloorSupplier;

    public ParkingHousePanel(ParkingGarageService garageService, Supplier<Integer> selectedFloorSupplier) {
        this.garageService = garageService;
        this.selectedFloorSupplier = selectedFloorSupplier;
        setBackground(new Color(248, 249, 252));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        GradientPaint bg = new GradientPaint(
                0, 0, new Color(244, 248, 255),
                getWidth(), getHeight(), new Color(233, 239, 250)
        );
        g.setPaint(bg);
        g.fillRect(0, 0, getWidth(), getHeight());

        int floorCount = garageService.floorCount();
        int spotsPerFloor = garageService.spotsPerFloor();
        int selectedFloor = selectedFloorSupplier.get();

        int padding = 18;
        int floorGap = 14;
        int availableHeight = getHeight() - (2 * padding) - ((floorCount - 1) * floorGap);
        int floorHeight = Math.max(28, availableHeight / Math.max(1, floorCount));
        int spotGap = 5;
        int availableWidth = getWidth() - (2 * padding) - 62;
        int spotWidth = Math.max(26, (availableWidth - ((spotsPerFloor - 1) * spotGap)) / Math.max(1, spotsPerFloor));

        g.setFont(new Font("Segoe UI", Font.BOLD, 12));

        for (int floor = floorCount; floor >= 1; floor--) {
            int rowIndex = floorCount - floor;
            int y = padding + rowIndex * (floorHeight + floorGap);
            int labelX = padding;
            int rowStartX = padding + 58;

            int occupiedOnFloor = 0;
            for (int spot = 1; spot <= spotsPerFloor; spot++) {
                if (garageService.isOccupied(floor, spot)) {
                    occupiedOnFloor++;
                }
            }

            g.setColor(new Color(220, 227, 240));
            g.fillRoundRect(rowStartX - 12, y - 9, spotsPerFloor * (spotWidth + spotGap) + 18, floorHeight + 18, 14, 14);

            g.setColor(new Color(60, 72, 96));
            g.drawString("F" + floor, labelX, y + (floorHeight / 2) + 1);
            g.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g.setColor(new Color(84, 97, 124));
            g.drawString(occupiedOnFloor + "/" + spotsPerFloor, labelX, y + (floorHeight / 2) + 14);
            g.setFont(new Font("Segoe UI", Font.BOLD, 12));

            if (floor == selectedFloor) {
                g.setColor(new Color(35, 116, 224));
                g.setStroke(new BasicStroke(2.4f));
                g.drawRoundRect(rowStartX - 12, y - 9, spotsPerFloor * (spotWidth + spotGap) + 18, floorHeight + 18, 14, 14);
            }

            for (int spot = 1; spot <= spotsPerFloor; spot++) {
                int x = rowStartX + (spot - 1) * (spotWidth + spotGap);
                boolean occupied = garageService.isOccupied(floor, spot);

                Color fill = occupied ? new Color(226, 103, 76) : new Color(68, 177, 126);
                Color border = occupied ? new Color(161, 63, 42) : new Color(40, 124, 88);
                g.setColor(fill);
                g.fillRoundRect(x, y, spotWidth, floorHeight, 10, 10);
                g.setColor(border);
                g.drawRoundRect(x, y, spotWidth, floorHeight, 10, 10);

                g.setColor(new Color(255, 255, 255, 45));
                g.fillRoundRect(x + 2, y + 2, spotWidth - 4, Math.max(6, floorHeight / 4), 8, 8);

                if (occupied) {
                    int carW = Math.max(10, spotWidth - 10);
                    int carH = Math.max(8, floorHeight - 14);
                    int carX = x + (spotWidth - carW) / 2;
                    int carY = y + (floorHeight - carH) / 2;
                    g.setColor(new Color(249, 242, 233));
                    g.fillRoundRect(carX, carY, carW, carH, 8, 8);
                    g.setColor(new Color(196, 181, 164));
                    g.drawRoundRect(carX, carY, carW, carH, 8, 8);
                    g.setColor(new Color(165, 206, 234));
                    g.fillRoundRect(carX + 2, carY + 2, Math.max(4, carW - 4), Math.max(3, carH / 2), 6, 6);
                }

                String label = String.valueOf(spot);
                g.setColor(Color.WHITE);
                g.setFont(new Font("Segoe UI", Font.BOLD, 10));
                FontMetrics fm = g.getFontMetrics();
                int tx = x + (spotWidth - fm.stringWidth(label)) / 2;
                int ty = y + floorHeight - 6;
                g.drawString(label, tx, ty);
            }
        }

        g.setColor(new Color(92, 102, 122));
        g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g.drawString("Green = free, Red = occupied, blue frame = active floor", 14, getHeight() - 8);
        g.dispose();
    }
}
