package cz.andel.parking.ui;

import cz.andel.ds.Treap;

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
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class TreapVisualizationPanel extends JPanel {
    private final Supplier<Treap<Integer, ?>> treapSupplier;
    private TreeSnapshot lastSnapshot = TreeSnapshot.empty();
    private RotationInfo lastRotation = RotationInfo.none();

    public TreapVisualizationPanel(Supplier<Treap<Integer, ?>> treapSupplier) {
        this.treapSupplier = treapSupplier;
        setBackground(new Color(252, 252, 255));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        TreeSnapshot snapshot = TreeSnapshot.fromTreap(treapSupplier.get());
        if (!snapshot.structureSignature.equals(lastSnapshot.structureSignature)) {
            lastRotation = RotationInfo.detect(lastSnapshot, snapshot);
            lastSnapshot = snapshot;
        }

        List<List<Optional<Map.Entry<Integer, Integer>>>> levels = snapshot.levels;

        GradientPaint bg = new GradientPaint(
                0, 0, new Color(247, 250, 255),
                getWidth(), getHeight(), new Color(236, 241, 251)
        );
        g.setPaint(bg);
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(new Color(53, 65, 92));
        g.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g.drawString(lastRotation.label(), 14, 18);

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
                boolean highlighted = lastRotation.matches(key);

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

    private static final class TreeSnapshot {
        private final List<List<Optional<Map.Entry<Integer, Integer>>>> levels;
        private final Map<Integer, Integer> parentByKey;
        private final Map<Integer, Integer> leftChildByKey;
        private final Map<Integer, Integer> rightChildByKey;
        private final String structureSignature;

        private TreeSnapshot(
                List<List<Optional<Map.Entry<Integer, Integer>>>> levels,
                Map<Integer, Integer> parentByKey,
                Map<Integer, Integer> leftChildByKey,
                Map<Integer, Integer> rightChildByKey,
                String structureSignature
        ) {
            this.levels = levels;
            this.parentByKey = parentByKey;
            this.leftChildByKey = leftChildByKey;
            this.rightChildByKey = rightChildByKey;
            this.structureSignature = structureSignature;
        }

        private static TreeSnapshot empty() {
            return new TreeSnapshot(List.of(), Map.of(), Map.of(), Map.of(), "empty");
        }

        private static TreeSnapshot fromTreap(Treap<Integer, ?> treap) {
            if (treap == null) {
                return empty();
            }

            try {
                Field rootField = treap.getClass().getDeclaredField("root");
                rootField.setAccessible(true);
                Object root = rootField.get(treap);
                if (root == null) {
                    return empty();
                }

                Field keyField = root.getClass().getDeclaredField("key");
                Field priorityField = root.getClass().getDeclaredField("priority");
                Field leftField = root.getClass().getDeclaredField("left");
                Field rightField = root.getClass().getDeclaredField("right");
                keyField.setAccessible(true);
                priorityField.setAccessible(true);
                leftField.setAccessible(true);
                rightField.setAccessible(true);

                List<List<Optional<Map.Entry<Integer, Integer>>>> levels = new java.util.ArrayList<>();
                Map<Integer, Integer> parentByKey = new HashMap<>();
                Map<Integer, Integer> leftChildByKey = new HashMap<>();
                Map<Integer, Integer> rightChildByKey = new HashMap<>();
                List<Object> current = new java.util.ArrayList<>();
                current.add(root);

                while (!current.isEmpty()) {
                    List<Optional<Map.Entry<Integer, Integer>>> level = new java.util.ArrayList<>(current.size());
                    List<Object> next = new java.util.ArrayList<>(current.size() * 2);
                    boolean hasRealNode = false;
                    boolean hasNextRealNode = false;

                    for (Object node : current) {
                        if (node == null) {
                            level.add(Optional.empty());
                            next.add(null);
                            next.add(null);
                            continue;
                        }

                        hasRealNode = true;
                        int key = ((Number) keyField.get(node)).intValue();
                        int priority = priorityField.getInt(node);
                        level.add(Optional.of(Map.entry(key, priority)));

                        Object left = leftField.get(node);
                        Object right = rightField.get(node);
                        next.add(left);
                        next.add(right);

                        if (left != null) {
                            int leftKey = ((Number) keyField.get(left)).intValue();
                            parentByKey.put(leftKey, key);
                            leftChildByKey.put(key, leftKey);
                            hasNextRealNode = true;
                        }
                        if (right != null) {
                            int rightKey = ((Number) keyField.get(right)).intValue();
                            parentByKey.put(rightKey, key);
                            rightChildByKey.put(key, rightKey);
                            hasNextRealNode = true;
                        }
                    }

                    if (!hasRealNode) {
                        break;
                    }

                    levels.add(level);
                    if (!hasNextRealNode) {
                        break;
                    }
                    current = next;
                }

                String signature = parentByKey.toString() + "|" + leftChildByKey + "|" + rightChildByKey;
                return new TreeSnapshot(levels, parentByKey, leftChildByKey, rightChildByKey, signature);
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Treap reflection failed", ex);
            }
        }
    }

    private static final class RotationInfo {
        private final String direction;
        private final Integer key;
        private final Integer pivot;

        private RotationInfo(String direction, Integer key, Integer pivot) {
            this.direction = direction;
            this.key = key;
            this.pivot = pivot;
        }

        private static RotationInfo none() {
            return new RotationInfo(null, null, null);
        }

        private static RotationInfo detect(TreeSnapshot previous, TreeSnapshot current) {
            for (Map.Entry<Integer, Integer> currentParent : current.parentByKey.entrySet()) {
                Integer child = currentParent.getKey();
                Integer parent = currentParent.getValue();
                Integer previousParentOfParent = previous.parentByKey.get(parent);
                if (previousParentOfParent == null || !previousParentOfParent.equals(child)) {
                    continue;
                }

                if (parent.equals(previous.rightChildByKey.get(child))) {
                    return new RotationInfo("LEFT", child, parent);
                }
                if (parent.equals(previous.leftChildByKey.get(child))) {
                    return new RotationInfo("RIGHT", child, parent);
                }
                return new RotationInfo("UNKNOWN", child, parent);
            }
            return none();
        }

        private String label() {
            if (direction == null) {
                return "Rotation: none in last mutation";
            }
            return "Rotation: " + direction + " at key=" + key + " (pivot key=" + pivot + ")";
        }

        private boolean matches(int value) {
            return (key != null && key == value) || (pivot != null && pivot == value);
        }
    }
}
