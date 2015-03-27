package au.gov.amsa.risky.format;

import static org.junit.Assert.assertEquals;

import java.io.StringWriter;

import org.junit.Test;

public class LibSvmTest {

    @Test
    public void testLibSvm() {
        StringWriter w = new StringWriter();
        LibSvm.write(w, 1, 2.3);
        assertEquals("1 1:2.3\n", w.toString());
    }

    @Test
    public void testLibSvmOnlyZeroValues() {
        StringWriter w = new StringWriter();
        LibSvm.write(w, 1, 0);
        assertEquals("1\n", w.toString());
    }

    @Test
    public void testLibSvmSomeZeros() {
        StringWriter w = new StringWriter();
        LibSvm.write(w, 101, 0, 0, 2, 0, 3);
        assertEquals("101 3:2.0 5:3.0\n", w.toString());
    }

}
