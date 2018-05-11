package au.gov.amsa.risky.format;

import static com.google.common.base.Optional.of;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

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
    public void testWriteOneFixToFile() throws IOException {
        FixImpl f = new FixImpl(213456789, -10f, 135f, 1526002907873L, of(12), of((short) 1),
                of(NavigationalStatus.ENGAGED_IN_FISHING), of(7.5f), of(45f), of(46f), AisClass.B);
        File output = new File("target/test.parquet");
        output.delete();
        Iterable<Record> iterable = () -> Iterators.limit(Iterators.cycle(Parquet.toRecord(f)), 1000);
        Parquet.writeTo(iterable, new Path(output.getPath()));
        System.out.println(output.length());
    }

}
