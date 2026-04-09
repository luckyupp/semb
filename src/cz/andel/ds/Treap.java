package cz.andel.ds;

import java.util.ArrayDeque;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

public class Treap<K extends Comparable<K>, V> {
    private static final int DEFAULT_PRIORITY_BOUND = 50;

    // Internal node is intentionally hidden to keep tree structure encapsulated.
    private static final class Node<K, V> {
        private final K key;
        private V value;
        private final int priority;
        private Node<K, V> left;
        private Node<K, V> right;

        private Node(K key, V value, int priority) {
            this.key = key;
            this.value = value;
            this.priority = priority;
        }
    }

    private static final class ValidationState {
        private boolean valid = true;
        private String message = "Treap invariants OK";
    }

    private static final class InsertState {
        private boolean inserted;
    }

    private static final class DeleteState {
        private boolean deleted;
    }

    private final Random random;
    private int priorityBound;
    private Node<K, V> root;
    private int size;
    private List<String> lastOperationEvents = List.of();
    private boolean lastInvariantsValid = true;
    private String lastInvariantMessage = "Treap invariants OK";

    public Treap() {
        this(new Random(), DEFAULT_PRIORITY_BOUND);
    }

    public Treap(Random random) {
        this(random, DEFAULT_PRIORITY_BOUND);
    }

    public Treap(Random random, int priorityBound) {
        this.random = random;
        setPriorityBound(priorityBound);
    }

    public int priorityBound() {
        return priorityBound;
    }

    public void setPriorityBound(int priorityBound) {
        if (priorityBound < 2) {
            throw new IllegalArgumentException("priorityBound must be at least 2");
        }
        this.priorityBound = priorityBound;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean containsKey(K key) {
        requireKey(key);
        return find(key).isPresent();
    }

    public Optional<V> find(K key) {
        requireKey(key);
        Node<K, V> current = root;
        while (current != null) {
            int cmp = key.compareTo(current.key);
            if (cmp == 0) {
                return Optional.of(current.value);
            }
            current = cmp < 0 ? current.left : current.right;
        }
        return Optional.empty();
    }

    public boolean insert(K key, V value) {
        requireKey(key);
        // Standard treap insertion: BST by key, heap by random priority.
        return insertWithPriority(key, value, random.nextInt(priorityBound));
    }

    public boolean insertWithPriority(K key, V value, int priority) {
        requireKey(key);
        List<String> events = new ArrayList<>();
        InsertState state = new InsertState();
        root = insertRecursive(root, key, value, priority, state, events);
        if (state.inserted) {
            size++;
        }
        ValidationState validationState = validate();
        storeLastOperation(events, validationState);
        return state.inserted;
    }

    public boolean delete(K key) {
        requireKey(key);
        // Deletion keeps BST property and restores heap property via rotations.
        List<String> events = new ArrayList<>();
        DeleteState state = new DeleteState();
        root = deleteRecursive(root, key, state, events);
        if (state.deleted) {
            size--;
        }
        ValidationState validationState = validate();
        storeLastOperation(events, validationState);
        return state.deleted;
    }

    public List<Map.Entry<K, V>> inOrder() {
        // In-order traversal returns keys in ascending order.
        List<Map.Entry<K, V>> entries = new ArrayList<>();
        inOrderRecursive(root, entries);
        return entries;
    }

    public Optional<K> predecessor(K key) {
        requireKey(key);
        // Best candidate smaller than key found during BST walk.
        Node<K, V> current = root;
        Node<K, V> candidate = null;
        while (current != null) {
            if (key.compareTo(current.key) <= 0) {
                current = current.left;
            } else {
                candidate = current;
                current = current.right;
            }
        }
        return candidate == null
                ? Optional.empty()
                : Optional.of(candidate.key);
    }

    public Optional<K> successor(K key) {
        requireKey(key);
        // Best candidate greater than key found during BST walk.
        Node<K, V> current = root;
        Node<K, V> candidate = null;
        while (current != null) {
            if (key.compareTo(current.key) >= 0) {
                current = current.right;
            } else {
                candidate = current;
                current = current.left;
            }
        }
        return candidate == null
                ? Optional.empty()
                : Optional.of(candidate.key);
    }

    public Optional<Integer> priorityOf(K key) {
        requireKey(key);
        Node<K, V> current = root;
        while (current != null) {
            int cmp = key.compareTo(current.key);
            if (cmp == 0) {
                return Optional.of(current.priority);
            }
            current = cmp < 0 ? current.left : current.right;
        }
        return Optional.empty();
    }

    public List<String> lastOperationEvents() {
        return Collections.unmodifiableList(lastOperationEvents);
    }

    public boolean lastInvariantsValid() {
        return lastInvariantsValid;
    }

    public String lastInvariantMessage() {
        return lastInvariantMessage;
    }

    public String levelOrderSnapshot() {
        if (root == null) {
            return "(empty)";
        }
        StringBuilder builder = new StringBuilder();
        Deque<Node<K, V>> queue = new ArrayDeque<>();
        queue.add(root);
        int level = 0;
        while (!queue.isEmpty()) {
            int count = queue.size();
            builder.append("level ").append(level).append(": ");
            for (int i = 0; i < count; i++) {
                Node<K, V> node = queue.removeFirst();
                builder.append("[k=").append(node.key).append(", p=").append(node.priority).append("]");
                if (i < count - 1) {
                    builder.append(" ");
                }
                if (node.left != null) {
                    queue.addLast(node.left);
                }
                if (node.right != null) {
                    queue.addLast(node.right);
                }
            }
            if (!queue.isEmpty()) {
                builder.append(System.lineSeparator());
            }
            level++;
        }
        return builder.toString();
    }

    public List<List<Optional<Map.Entry<K, Integer>>>> levelLayout() {
        if (root == null) {
            return List.of();
        }

        List<List<Optional<Map.Entry<K, Integer>>>> levels = new ArrayList<>();
        List<Node<K, V>> current = new ArrayList<>();
        current.add(root);

        while (!current.isEmpty()) {
            List<Optional<Map.Entry<K, Integer>>> level = new ArrayList<>(current.size());
            List<Node<K, V>> next = new ArrayList<>(current.size() * 2);
            boolean hasRealNode = false;
            boolean hasNextRealNode = false;

            for (Node<K, V> node : current) {
                if (node == null) {
                    level.add(Optional.empty());
                    next.add(null);
                    next.add(null);
                    continue;
                }

                hasRealNode = true;
                level.add(Optional.of(new AbstractMap.SimpleImmutableEntry<>(node.key, node.priority)));
                next.add(node.left);
                next.add(node.right);
                if (node.left != null || node.right != null) {
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

        return levels;
    }

    private Node<K, V> insertRecursive(
            Node<K, V> node,
            K key,
            V value,
            int priority,
            InsertState state,
            List<String> events
    ) {
        if (node == null) {
            state.inserted = true;
            events.add("Inserted key=" + key + " with priority=" + priority);
            return new Node<>(key, value, priority);
        }

        int cmp = key.compareTo(node.key);
        if (cmp == 0) {
            events.add("Insert blocked: key=" + key + " already exists");
            return node;
        }

        if (cmp < 0) {
            node.left = insertRecursive(node.left, key, value, priority, state, events);
            // If left child violates max-heap priority, rotate it up.
            if (state.inserted && node.left != null && node.left.priority > node.priority) {
                node = rotateRight(node, events);
            }
        } else {
            node.right = insertRecursive(node.right, key, value, priority, state, events);
            // If right child violates max-heap priority, rotate it up.
            if (state.inserted && node.right != null && node.right.priority > node.priority) {
                node = rotateLeft(node, events);
            }
        }
        return node;
    }

    private Node<K, V> deleteRecursive(Node<K, V> node, K key, DeleteState state, List<String> events) {
        if (node == null) {
            return null;
        }

        int cmp = key.compareTo(node.key);
        if (cmp < 0) {
            node.left = deleteRecursive(node.left, key, state, events);
            return node;
        }
        if (cmp > 0) {
            node.right = deleteRecursive(node.right, key, state, events);
            return node;
        }

        state.deleted = true;
        events.add("Deleting key=" + key);

        if (node.left == null && node.right == null) {
            events.add("Deleted leaf key=" + key);
            return null;
        }
        if (node.left == null) {
            events.add("Deleted node key=" + key + " replaced by right child");
            return node.right;
        }
        if (node.right == null) {
            events.add("Deleted node key=" + key + " replaced by left child");
            return node.left;
        }

        if (node.left.priority > node.right.priority) {
            // Bubble higher-priority child up, then continue deleting key below.
            node = rotateRight(node, events);
            node.right = deleteRecursive(node.right, key, state, events);
        } else {
            // Bubble higher-priority child up, then continue deleting key below.
            node = rotateLeft(node, events);
            node.left = deleteRecursive(node.left, key, state, events);
        }
        return node;
    }

    private Node<K, V> rotateLeft(Node<K, V> node, List<String> events) {
        // Left rotation around node: node.right becomes new subtree root.
        Node<K, V> pivot = node.right;
        node.right = pivot.left;
        pivot.left = node;
        events.add(
                "Rotate LEFT at key=" + node.key
                        + " (pivot key=" + pivot.key + ")"
        );
        return pivot;
    }

    private Node<K, V> rotateRight(Node<K, V> node, List<String> events) {
        // Right rotation around node: node.left becomes new subtree root.
        Node<K, V> pivot = node.left;
        node.left = pivot.right;
        pivot.right = node;
        events.add(
                "Rotate RIGHT at key=" + node.key
                        + " (pivot key=" + pivot.key + ")"
        );
        return pivot;
    }

    private void inOrderRecursive(Node<K, V> node, List<Map.Entry<K, V>> entries) {
        if (node == null) {
            return;
        }
        inOrderRecursive(node.left, entries);
        entries.add(new AbstractMap.SimpleImmutableEntry<>(node.key, node.value));
        inOrderRecursive(node.right, entries);
    }

    private void requireKey(K key) {
        if (key == null) {
            throw new IllegalArgumentException("Key must not be null");
        }
    }

    private void storeLastOperation(List<String> events, ValidationState validationState) {
        lastOperationEvents = List.copyOf(events);
        lastInvariantsValid = validationState.valid;
        lastInvariantMessage = validationState.message;
    }

    private ValidationState validate() {
        ValidationState state = new ValidationState();
        validateRecursive(root, null, null, null, state);
        return state;
    }

    private void validateRecursive(
            Node<K, V> node,
            K minExclusive,
            K maxExclusive,
            Integer parentPriority,
            ValidationState state
    ) {
        // Checks both invariants: strict BST ordering and max-heap priorities.
        if (node == null || !state.valid) {
            return;
        }

        if (minExclusive != null && node.key.compareTo(minExclusive) <= 0) {
            state.valid = false;
            state.message = "BST violation: key " + node.key + " <= min boundary " + minExclusive;
            return;
        }
        if (maxExclusive != null && node.key.compareTo(maxExclusive) >= 0) {
            state.valid = false;
            state.message = "BST violation: key " + node.key + " >= max boundary " + maxExclusive;
            return;
        }
        if (parentPriority != null && node.priority > parentPriority) {
            state.valid = false;
            state.message = "Heap violation: child priority " + node.priority + " > parent priority " + parentPriority;
            return;
        }

        validateRecursive(node.left, minExclusive, node.key, node.priority, state);
        validateRecursive(node.right, node.key, maxExclusive, node.priority, state);
    }
}
