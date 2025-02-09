package ru.vk.itmo.naumovivan;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;

public abstract class MergeIterator<T> implements Iterator<T> {

    private final PriorityQueue<MergeIterator.PeekIterator<T>> priorityQueue;
    private final Comparator<T> comparator;

    MergeIterator.PeekIterator<T> peek;

    private static class PeekIterator<T> implements Iterator<T> {

        public final int id;
        private final Iterator<T> delegate;
        private T peek;

        private PeekIterator(int id, Iterator<T> delegate) {
            this.id = id;
            this.delegate = delegate;
        }

        @Override
        public boolean hasNext() {
            if (peek == null) {
                return delegate.hasNext();
            }
            return true;
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            T ret = peek();
            this.peek = null;
            return ret;
        }

        private T peek() {
            if (peek == null) {
                if (!delegate.hasNext()) {
                    return null;
                }
                peek = delegate.next();
            }
            return peek;
        }
    }

    protected MergeIterator(Collection<Iterator<T>> iterators, Comparator<T> comparator) {
        this.comparator = comparator;
        Comparator<MergeIterator.PeekIterator<T>> peekComp = (o1, o2) -> comparator.compare(o1.peek(), o2.peek());
        priorityQueue = new PriorityQueue<>(
                iterators.size(),
                peekComp.thenComparing(o -> -o.id)
        );

        int id = 0;
        for (Iterator<T> iterator : iterators) {
            if (iterator.hasNext()) {
                priorityQueue.add(new MergeIterator.PeekIterator<>(id++, iterator));
            }
        }
    }

    private MergeIterator.PeekIterator<T> peek() {
        while (peek == null) {
            peek = priorityQueue.poll();
            if (peek == null) {
                return null;
            }

            mergeWithPeekIterator();

            if (peek.peek() == null) {
                peek = null;
                continue;
            }

            if (skip(peek.peek())) {
                peek.next();
                if (peek.hasNext()) {
                    priorityQueue.add(peek);
                }
                peek = null;
            }
        }

        return peek;
    }

    private void mergeWithPeekIterator() {
        while (true) {
            MergeIterator.PeekIterator<T> next = priorityQueue.peek();
            if (next == null) {
                break;
            }

            int compare = comparator.compare(peek.peek(), next.peek());
            if (compare == 0) {
                MergeIterator.PeekIterator<T> poll = priorityQueue.poll();
                if (poll == null) {
                    continue;
                }
                poll.next();
                if (poll.hasNext()) {
                    priorityQueue.add(poll);
                }
            } else {
                break;
            }
        }
    }

    protected abstract boolean skip(T value);

    @Override
    public boolean hasNext() {
        return peek() != null;
    }

    @Override
    public T next() {
        MergeIterator.PeekIterator<T> peekIterator = peek();
        if (peekIterator == null) {
            throw new NoSuchElementException();
        }
        T next = peekIterator.next();
        this.peek = null;
        if (peekIterator.hasNext()) {
            priorityQueue.add(peekIterator);
        }
        return next;
    }
}
