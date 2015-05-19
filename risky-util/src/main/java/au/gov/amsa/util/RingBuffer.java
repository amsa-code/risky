package au.gov.amsa.util;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

import net.jcip.annotations.NotThreadSafe;

/**
 * Non-threadsafe implementation of a Ring Buffer. Does not accept nulls.
 * 
 * @param <T>
 */
@NotThreadSafe
public class RingBuffer<T> implements Queue<T> {

    private final T[] list;
    private int start;
    private int finish;

    @SuppressWarnings("unchecked")
    private RingBuffer(int size) {
        list = (T[]) new Object[size + 1];
    }

    public static <T> RingBuffer<T> create(int size) {
        return new RingBuffer<T>(size);
    }

    @Override
    public void clear() {
        finish = start;
    }

    public Enumeration<T> values() {
        final int _start = start;
        final int _finish = finish;
        return new Enumeration<T>() {
            int i = _start;

            @Override
            public boolean hasMoreElements() {
                return i != _finish;
            }

            @Override
            public T nextElement() {
                T value = list[i];
                i = (i + 1) % list.length;
                return value;
            }
        };
    }

    @Override
    public T peek() {
        if (start == finish)
            return null;
        else
            return list[start];
    }

    public boolean isEmpty() {
        return start == finish;
    }

    public int size() {
        if (start <= finish)
            return finish - start;
        else
            return finish - start + list.length;
    }

    public int maxSize() {
        return list.length - 1;
    }

    @Override
    public boolean contains(Object o) {
        return notImplemented();
    }

    @Override
    public Iterator<T> iterator() {
        return notImplemented();
    }

    @Override
    public Object[] toArray() {
        return notImplemented();
    }

    @Override
    public <S> S[] toArray(S[] a) {
        return notImplemented();
    }

    @Override
    public boolean remove(Object o) {
        return notImplemented();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return notImplemented();
    }

    private static <T> T notImplemented() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        return notImplemented();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return notImplemented();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return notImplemented();
    }

    @Override
    public boolean add(T t) {
        if (offer(t))
            return true;
        else
            throw new IllegalStateException("Cannot add to queue because is full");
    }

    @Override
    public boolean offer(T t) {
        if (t == null)
            throw new NullPointerException();
        int currentFinish = finish;
        finish = (finish + 1) % list.length;
        if (finish == start) {
            return false;
        } else {
            list[currentFinish] = t;
            return true;
        }
    }

    @Override
    public T remove() {
        T t = poll();
        if (t == null)
            throw new NoSuchElementException();
        else
            return t;
    }

    @Override
    public T poll() {
        if (start == finish) {
            return null;
        } else {
            T value = list[start];
            // don't hold a reference to a popped value
            list[start] = null;
            start = (start + 1) % list.length;
            return value;
        }
    }

    @Override
    public T element() {
        return notImplemented();
    }
}
