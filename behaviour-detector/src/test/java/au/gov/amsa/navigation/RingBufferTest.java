package au.gov.amsa.navigation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
    public void testPushThree() {
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

    public void testPushThreeInSizeFour() {
        RingBuffer<Integer> q = RingBuffer.create(4);
        q.push(1);
        q.push(2);
        q.push(3);
        assertFalse(q.isEmpty());
        assertEquals(3, q.size());
        assertEquals(1, (int) q.pop());
        assertEquals(2, (int) q.pop());
        assertEquals(3, (int) q.pop());
    }
}
