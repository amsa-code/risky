package au.gov.amsa.ais;

import static au.gov.amsa.ais.TstUtil.handleAisStream;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.zip.GZIPInputStream;

import org.junit.Test;

public class NmeaStreamProcessorIntegrationTest {

    private static final PrintStream IGNORE = new PrintStream(new NullOutputStream());

    @Test
    public void test() {
        TstUtil.process(
                new NmeaReaderFromInputStream(NmeaStreamProcessorTest.class
                        .getResourceAsStream("/exact-earth-augmented-2.txt")), TstUtil
                        .createLoggingListener(IGNORE), IGNORE);
    }

    @Test
    public void testMany() throws IOException {
        handleAisStream(
                new GZIPInputStream(AisNmeaMessageTest.class.getResourceAsStream("/ais.txt.gz")),
                new PrintStream("target/raw-1.txt"), new PrintStream("target/processed-1.txt"),
                IGNORE);
    }

    private static class NullOutputStream extends OutputStream {

        @Override
        public void write(int b) throws IOException {
            // do nothing

        }

    }

}
