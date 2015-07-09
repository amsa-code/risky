package au.gov.amsa.mariweb;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class Benchmarks {

    final static byte[] bytes = readText();

    @Benchmark
    public void extractValuesFromInsertStatement() {
        InputStream is = new ByteArrayInputStream(bytes);
        BackupReader.getNmea(is).toBlocking().last();
    }

    private static byte[] readText() {
        try {
            return IOUtils.toByteArray(OperatorExtractValuesFromInsertStatementTest.class
                    .getResourceAsStream("/mariweb-backup-sample.txt"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
