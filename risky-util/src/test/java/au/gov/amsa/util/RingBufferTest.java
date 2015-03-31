package au.gov.amsa.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Enumeration;

import org.junit.Test;

public class RingBufferTest {

    @Test
    public void testEmpty() {
        RingBuffer<Integer> q = RingBuffer.create(3);
        assertTrue(q.isEmpty());
        assertEquals(0, q.size());
    }

    @Test
    public void testPush() {
        RingBuffer<Integer> q = RingBuffer.create(3);
        q.push(1);
        assertFalse(q.isEmpty());
        assertEquals(1, q.size());
        assertEquals(1, (int) q.pop());
        assertTrue(q.isEmpty());
    }

    @Test
    public void testPushTwo() {
        RingBuffer<Integer> q = RingBuffer.create(3);
        q.push(1);
        q.push(2);
        assertFalse(q.isEmpty());
        assertEquals(2, q.size());
        assertEquals(1, (int) q.pop());
        assertEquals(2, (int) q.pop());
        assertTrue(q.isEmpty());
        q.push(3);
        q.push(4);
        assertEquals(3, (int) q.pop());
        assertEquals(4, (int) q.pop());
    }

    @Test(expected = RuntimeException.class)
    public void testPushThreeOverflows() {
        RingBuffer<Integer> q = RingBuffer.create(2);
        q.push(1);
        q.push(2);
        q.push(3);
        assertFalse(q.isEmpty());
        assertEquals(3, q.size());
        assertEquals(1, (int) q.pop());
        assertEquals(2, (int) q.pop());
        assertEquals(3, (int) q.pop());
    }

    public void testPushThreeInSizeThree() {
        RingBuffer<Integer> q = RingBuffer.create(3);
        q.push(1);
        q.push(2);
        q.push(3);
        assertFalse(q.isEmpty());
        assertEquals(3, q.size());
        assertEquals(1, (int) q.pop());
        assertEquals(2, (int) q.pop());
        assertEquals(3, (int) q.pop());
    }

    public void testPushThreeAndEnumerate() {
        RingBuffer<Integer> q = RingBuffer.create(3);
        q.push(1);
        q.push(2);
        q.push(3);
        Enumeration<Integer> en = q.values();
        assertEquals(1, (int) en.nextElement());
        assertEquals(1, (int) q.pop());
        assertEquals(2, (int) en.nextElement());
        assertEquals(2, (int) q.pop());
        assertEquals(3, (int) en.nextElement());
        assertEquals(3, (int) q.pop());
        assertFalse(en.hasMoreElements());
    }
}
