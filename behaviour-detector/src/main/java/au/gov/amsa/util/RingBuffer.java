package au.gov.amsa.util;

import java.util.Enumeration;

/**
 * Non-threadsafe implementation of a Ring Buffer.
 * 
 * @param <T>
 */
public class RingBuffer<T> {

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

    public RingBuffer<T> push(T t) {
        int currentFinish = finish;
        finish = (finish + 1) % list.length;
        if (finish == start) {
            throw new RuntimeException("overflow of ring buffer");
        } else {
            list[currentFinish] = t;
        }
        return this;
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

    public T pop() {
        if (start == finish) {
            throw new RuntimeException("no element present");
        } else {
            T value = list[start];
            start = (start + 1) % list.length;
            return value;
        }
    }

    public T peek() {
        if (start == finish)
            throw new RuntimeException("no element present");
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
}
