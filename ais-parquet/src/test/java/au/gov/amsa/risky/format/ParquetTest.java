package au.gov.amsa.risky.format;

import static com.google.common.base.Optional.of;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

import org.apache.avro.generic.GenericData.Record;
import org.apache.hadoop.fs.Path;
import org.junit.Test;

import com.google.common.collect.Iterators;

public final class ParquetTest {

    @Test
    public void testLoadSchema() {
        Parquet.loadSchema();
    }

    @Test
    public void testRoundTrip() throws IOException {
        int n = 10000;
        FixImpl f = new FixImpl(213456789, -10f, 135f, 1526002907873L, of(12), of((short) 1),
                of(NavigationalStatus.ENGAGED_IN_FISHING), of(7.5f), of(45f), of(46f), AisClass.B);
        File output = new File("target/test.parquet");
        output.delete();
        Iterable<Record> iterable = () -> Iterators.limit(Iterators.cycle(Parquet.toRecord(f)), n);
        Parquet.writeTo(iterable, new Path(output.getPath()));
        System.out.println("parquet file size = " + output.length() + "B");
        final Stream<Record> stream = Parquet.read(new Path(output.getPath()));
        assertEquals(n, stream.peek(r -> {
            if (r == null) {
                stream.close();
            }
        }) //
                .filter(x -> x != null) //
                .count());
    }

}
