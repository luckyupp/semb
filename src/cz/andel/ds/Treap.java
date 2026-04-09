package cz.andel.ds;

import java.util.AbstractMap;
import java.util.ArrayList;
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
        InsertState state = new InsertState();
        root = insertRecursive(root, key, value, priority, state);
        if (state.inserted) {
            size++;
        }
        validateOrThrow();
        return state.inserted;
    }

    public boolean delete(K key) {
        requireKey(key);
        DeleteState state = new DeleteState();
        root = deleteRecursive(root, key, state);
        if (state.deleted) {
            size--;
        }
        validateOrThrow();
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

    private Node<K, V> insertRecursive(
            Node<K, V> node,
            K key,
            V value,
            int priority,
            InsertState state
    ) {
        if (node == null) {
            state.inserted = true;
            return new Node<>(key, value, priority);
        }

        int cmp = key.compareTo(node.key);
        if (cmp == 0) {
            return node;
        }

        if (cmp < 0) {
            node.left = insertRecursive(node.left, key, value, priority, state);
            // If left child violates max-heap priority, rotate it up.
            if (state.inserted && node.left != null && node.left.priority > node.priority) {
                node = rotateRight(node);
            }
        } else {
            node.right = insertRecursive(node.right, key, value, priority, state);
            // If right child violates max-heap priority, rotate it up.
            if (state.inserted && node.right != null && node.right.priority > node.priority) {
                node = rotateLeft(node);
            }
        }
        return node;
    }

    private Node<K, V> deleteRecursive(Node<K, V> node, K key, DeleteState state) {
        if (node == null) {
            return null;
        }

        int cmp = key.compareTo(node.key);
        if (cmp < 0) {
            node.left = deleteRecursive(node.left, key, state);
            return node;
        }
        if (cmp > 0) {
            node.right = deleteRecursive(node.right, key, state);
            return node;
        }

        state.deleted = true;

        if (node.left == null && node.right == null) {
            return null;
        }
        if (node.left == null) {
            return node.right;
        }
        if (node.right == null) {
            return node.left;
        }

        if (node.left.priority > node.right.priority) {
            // Bubble higher-priority child up, then continue deleting key below.
            node = rotateRight(node);
            node.right = deleteRecursive(node.right, key, state);
        } else {
            // Bubble higher-priority child up, then continue deleting key below.
            node = rotateLeft(node);
            node.left = deleteRecursive(node.left, key, state);
        }
        return node;
    }

    private Node<K, V> rotateLeft(Node<K, V> node) {
        // Left rotation around node: node.right becomes new subtree root.
        Node<K, V> pivot = node.right;
        node.right = pivot.left;
        pivot.left = node;
        return pivot;
    }

    private Node<K, V> rotateRight(Node<K, V> node) {
        // Right rotation around node: node.left becomes new subtree root.
        Node<K, V> pivot = node.left;
        node.left = pivot.right;
        pivot.right = node;
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

    private void validateOrThrow() {
        if (!validateRecursive(root, null, null, null)) {
            throw new IllegalStateException("Treap invariants violated");
        }
    }

    private boolean validateRecursive(Node<K, V> node, K minExclusive, K maxExclusive, Integer parentPriority) {
        // Checks both invariants: strict BST ordering and max-heap priorities.
        if (node == null) {
            return true;
        }

        if (minExclusive != null && node.key.compareTo(minExclusive) <= 0) {
            return false;
        }
        if (maxExclusive != null && node.key.compareTo(maxExclusive) >= 0) {
            return false;
        }
        if (parentPriority != null && node.priority > parentPriority) {
            return false;
        }

        return validateRecursive(node.left, minExclusive, node.key, node.priority)
                && validateRecursive(node.right, node.key, maxExclusive, node.priority);
    }
}
