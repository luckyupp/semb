package cz.andel.parking.ui;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class TreapVisualizationPanel extends JPanel {
    private final Supplier<List<List<Optional<Map.Entry<Integer, Integer>>>>> layoutSupplier;
    private final Supplier<List<String>> eventsSupplier;

    public TreapVisualizationPanel(
            Supplier<List<List<Optional<Map.Entry<Integer, Integer>>>>> layoutSupplier,
            Supplier<List<String>> eventsSupplier
    ) {
        this.layoutSupplier = layoutSupplier;
        this.eventsSupplier = eventsSupplier;
        setBackground(new Color(252, 252, 255));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        List<List<Optional<Map.Entry<Integer, Integer>>>> levels = layoutSupplier.get();
        List<String> events = eventsSupplier.get();
        RotationKeys rotationKeys = parseRotationKeys(events);

        GradientPaint bg = new GradientPaint(
                0, 0, new Color(247, 250, 255),
                getWidth(), getHeight(), new Color(236, 241, 251)
        );
        g.setPaint(bg);
        g.fillRect(0, 0, getWidth(), getHeight());

        String rotationInfo = findLastRotation(events);
        g.setColor(new Color(53, 65, 92));
        g.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g.drawString(rotationInfo == null ? "Rotation: none in last mutation" : "Rotation: " + rotationInfo, 14, 18);

        if (levels.isEmpty()) {
            g.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            g.setColor(new Color(120, 126, 140));
            g.drawString("Tree is empty", 16, 42);
            g.dispose();
            return;
        }

        int top = 40;
        int bottom = getHeight() - 24;
        int nodeRadius = 21;
        int levelGap = Math.max(50, (bottom - top) / Math.max(1, levels.size()));
        Map<String, Point> positions = new HashMap<>();

        for (int level = 0; level < levels.size(); level++) {
            List<Optional<Map.Entry<Integer, Integer>>> levelNodes = levels.get(level);
            int count = levelNodes.size();
            int y = top + level * levelGap;
            for (int i = 0; i < count; i++) {
                Optional<Map.Entry<Integer, Integer>> node = levelNodes.get(i);
                if (node.isEmpty()) {
                    continue;
                }
                int x = ((i + 1) * getWidth()) / (count + 1);
                String id = level + ":" + i;
                positions.put(id, new Point(x, y));
            }
        }

        g.setColor(new Color(149, 160, 189));
        g.setStroke(new BasicStroke(2f));
        for (int level = 0; level < levels.size() - 1; level++) {
            List<Optional<Map.Entry<Integer, Integer>>> levelNodes = levels.get(level);
            List<Optional<Map.Entry<Integer, Integer>>> nextLevel = levels.get(level + 1);

            for (int i = 0; i < levelNodes.size(); i++) {
                if (levelNodes.get(i).isEmpty()) {
                    continue;
                }

                Point parent = positions.get(level + ":" + i);
                int leftIndex = i * 2;
                int rightIndex = i * 2 + 1;

                if (leftIndex < nextLevel.size() && nextLevel.get(leftIndex).isPresent()) {
                    Point left = positions.get((level + 1) + ":" + leftIndex);
                    g.drawLine(parent.x, parent.y + nodeRadius - 2, left.x, left.y - nodeRadius + 2);
                    drawArrow(g, parent.x, parent.y + nodeRadius - 2, left.x, left.y - nodeRadius + 2);
                }
                if (rightIndex < nextLevel.size() && nextLevel.get(rightIndex).isPresent()) {
                    Point right = positions.get((level + 1) + ":" + rightIndex);
                    g.drawLine(parent.x, parent.y + nodeRadius - 2, right.x, right.y - nodeRadius + 2);
                    drawArrow(g, parent.x, parent.y + nodeRadius - 2, right.x, right.y - nodeRadius + 2);
                }
            }
        }

        for (int level = 0; level < levels.size(); level++) {
            List<Optional<Map.Entry<Integer, Integer>>> levelNodes = levels.get(level);
            for (int i = 0; i < levelNodes.size(); i++) {
                Optional<Map.Entry<Integer, Integer>> node = levelNodes.get(i);
                if (node.isEmpty()) {
                    continue;
                }

                Point p = positions.get(level + ":" + i);
                int x = p.x;
                int y = p.y;
                int key = node.get().getKey();
                boolean highlighted = rotationKeys.matches(key);

                if (highlighted) {
                    g.setColor(new Color(255, 214, 102, 130));
                    g.fillOval(x - nodeRadius - 6, y - nodeRadius - 6, (nodeRadius * 2) + 12, (nodeRadius * 2) + 12);
                }

                g.setColor(new Color(70, 115, 214));
                g.fillOval(x - nodeRadius, y - nodeRadius, nodeRadius * 2, nodeRadius * 2);
                g.setColor(new Color(23, 62, 143));
                g.setStroke(new BasicStroke(highlighted ? 2.6f : 1.4f));
                g.drawOval(x - nodeRadius, y - nodeRadius, nodeRadius * 2, nodeRadius * 2);

                g.setColor(new Color(255, 255, 255, 45));
                g.fillOval(x - nodeRadius + 3, y - nodeRadius + 3, (nodeRadius * 2) - 6, nodeRadius - 2);

                String keyText = String.valueOf(key);
                g.setFont(new Font("Segoe UI", Font.BOLD, 13));
                FontMetrics fmKey = g.getFontMetrics();
                int keyX = x - (fmKey.stringWidth(keyText) / 2);
                g.setColor(Color.WHITE);
                g.drawString(keyText, keyX, y + 4);

                String pText = "p=" + node.get().getValue();
                g.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                FontMetrics fmPr = g.getFontMetrics();
                g.setColor(new Color(42, 52, 76));
                g.drawString(pText, x - (fmPr.stringWidth(pText) / 2), y + nodeRadius + 12);
            }
        }

        g.dispose();
    }

    private static String findLastRotation(List<String> events) {
        for (int i = events.size() - 1; i >= 0; i--) {
            String e = events.get(i);
            if (e.startsWith("Rotate ")) {
                return e;
            }
        }
        return null;
    }

    private void drawArrow(Graphics2D g, int x1, int y1, int x2, int y2) {
        double angle = Math.atan2(y2 - y1, x2 - x1);
        int len = 7;
        int ax = (int) (x2 - Math.cos(angle) * 5);
        int ay = (int) (y2 - Math.sin(angle) * 5);
        int lx = (int) (ax - Math.cos(angle - Math.PI / 6) * len);
        int ly = (int) (ay - Math.sin(angle - Math.PI / 6) * len);
        int rx = (int) (ax - Math.cos(angle + Math.PI / 6) * len);
        int ry = (int) (ay - Math.sin(angle + Math.PI / 6) * len);
        g.drawLine(ax, ay, lx, ly);
        g.drawLine(ax, ay, rx, ry);
    }

    private RotationKeys parseRotationKeys(List<String> events) {
        String rotation = findLastRotation(events);
        if (rotation == null) {
            return RotationKeys.none();
        }

        Integer key = extractIntAfter(rotation, "key=");
        Integer pivot = extractIntAfter(rotation, "pivot key=");
        if (key == null && pivot == null) {
            return RotationKeys.none();
        }
        return new RotationKeys(key, pivot);
    }

    private Integer extractIntAfter(String text, String marker) {
        int start = text.indexOf(marker);
        if (start < 0) {
            return null;
        }
        start += marker.length();
        StringBuilder digits = new StringBuilder();
        while (start < text.length()) {
            char ch = text.charAt(start);
            if (Character.isDigit(ch) || (ch == '-' && digits.isEmpty())) {
                digits.append(ch);
                start++;
            } else {
                break;
            }
        }
        if (digits.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(digits.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static final class RotationKeys {
        private final Integer key;
        private final Integer pivot;

        private RotationKeys(Integer key, Integer pivot) {
            this.key = key;
            this.pivot = pivot;
        }

        private static RotationKeys none() {
            return new RotationKeys(null, null);
        }

        private boolean matches(int value) {
            return (key != null && key == value) || (pivot != null && pivot == value);
        }
    }
}
