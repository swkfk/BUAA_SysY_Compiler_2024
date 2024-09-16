package top.swkfk.compiler.utils;

import java.util.Iterator;
import java.util.Optional;

final public class DualLinkedList<T> implements Iterable<DualLinkedList.Node<T>> {

    private Node<T> head;
    private Node<T> tail;
    private int length;

    @Override
    public Iterator<Node<T>> iterator() {
        return new Iter<>(head);
    }

    public int size() {
        return length;
    }

    public boolean isEmpty() {
        return length == 0;
    }

    public Node<T> getHead() {
        return head;
    }

    public Node<T> getTail() {
        return tail;
    }

    public static class Node<T> {
        private final T data;
        private Node<T> prev;
        private Node<T> next;
        private DualLinkedList<T> parent;

        public Node(T data) {
            this.data = data;
        }

        public T getData() {
            return data;
        }

        public Node<T> getPrev() {
            return prev;
        }

        public Node<T> getNext() {
            return next;
        }

        public void insertIntoHead(DualLinkedList<T> parent) {
            this.parent = parent;
            if (parent.isEmpty()) {
                parent.tail = this;
            } else {
                parent.head.prev = this;
                this.next = parent.head;
            }
            parent.head = this;
            parent.length++;
        }

        public void insertIntoTail(DualLinkedList<T> parent) {
            this.parent = parent;
            if (parent.isEmpty()) {
                parent.head = this;
            } else {
                parent.tail.next = this;
                this.prev = parent.tail;
            }
            parent.tail = this;
            parent.length++;
        }

        public void insertAfter(Node<T> node) {
            this.next = node.next;
            this.prev = node;
            node.next = this;

            this.parent = node.parent;
            this.parent.length++;

            Optional.ofNullable(this.next).ifPresent(after -> after.prev = this);
            if (parent.tail == node) {
                parent.tail = this;
            }
        }

        public void insertBefore(Node<T> node) {
            this.next = node;
            this.prev = node.prev;
            node.prev = this;

            this.parent = node.parent;
            this.parent.length++;

            Optional.ofNullable(this.prev).ifPresent(before -> before.next = this);
            if (parent.head == node) {
                parent.head = this;
            }
        }

        public void drop() {
            if (prev != null) {
                prev.next = next;
            } else {
                parent.head = next;
            }

            if (next != null) {
                next.prev = prev;
            } else {
                parent.tail = prev;
            }

            parent.length--;
        }
    }

    static class Iter<T> implements Iterator<Node<T>> {

        private final Node<T> start;
        private Node<T> now;

        Iter(Node<T> head) {
            start = new Node<>(null);
            this.now = start;
            this.now.next = head;
        }

        @Override
        public boolean hasNext() {
            return now != null && now.next != null;
        }

        @Override
        public Node<T> next() {
            now = now.next;
            return now;
        }

        @Override
        public void remove() {
            if (now == start) {
                return;
            }
            Node<T> prev = now.prev;
            now.drop();
            now = prev;
        }
    }
}
