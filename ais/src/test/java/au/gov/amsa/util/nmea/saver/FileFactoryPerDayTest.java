package au.gov.amsa.util.nmea.saver;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

public class FileFactoryPerDayTest {
    
    @Test
    public void test() {
        FileFactoryPerDay f = new FileFactoryPerDay(new File("target/temp"));
        long t = 1656565846973L;
        File a = f.file("blah", t);
        assertEquals("2022-06-30.txt", a.getName());
    }

}
