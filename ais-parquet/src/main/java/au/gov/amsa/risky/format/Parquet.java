package au.gov.amsa.risky.format;

import java.io.IOException;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericData.Record;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;

import com.github.davidmoten.guavamini.annotations.VisibleForTesting;

//PRE-ALPHA! IN DEVELOPMENT
public class Parquet {

    public static void writeTo(Iterable<GenericData.Record> recordsToWrite, Path path) throws IOException {
        writeTo(recordsToWrite, path, FIX_WITH_MMSI_SCHEMA);
    }

    private static void writeTo(Iterable<GenericData.Record> recordsToWrite, Path path, Schema schema)
            throws IOException {
        try (ParquetWriter<GenericData.Record> writer = AvroParquetWriter.<GenericData.Record>builder(path)
                .withSchema(schema).withConf(new Configuration()).withCompressionCodec(CompressionCodecName.SNAPPY)
                .build()) {

            for (GenericData.Record record : recordsToWrite) {
                writer.write(record);
            }
        }
    }

    public static Stream<GenericData.Record> read(Path path) {
        return read(path, FIX_WITH_MMSI_SCHEMA);
    }

    public static Stream<GenericData.Record> read(Path path, Schema schema) {
        ParquetReader<GenericData.Record> reader;
        try {
            reader = AvroParquetReader.<GenericData.Record>builder(path).withConf(new Configuration()).build();
        } catch (IOException e1) {
            throw new RuntimeException(e1);
        }

        return Stream.generate(new Supplier<GenericData.Record>() {

            @Override
            public Record get() {
                GenericData.Record r;
                try {
                    r = reader.read();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                if (r != null) {
                    return r;
                } else {
                    return null;
                }
            }
        }) //
                .onClose(() -> {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

    }

    public static final Schema FIX_WITH_MMSI_SCHEMA = loadSchema();

    public static GenericData.Record toRecord(Fix fix) {
        GenericData.Record record = new GenericData.Record(FIX_WITH_MMSI_SCHEMA);
        record.put("mmsi", fix.mmsi());
        record.put("lat", fix.lat());
        record.put("lon", fix.lon());
        record.put("timeEpochMs", fix.time());
        record.put("latencySeconds", fix.latencySeconds().or(-1));
        record.put("source", (int) fix.source().or((short) 0));
        record.put("navigationalStatus", fix.navigationalStatus().transform(n -> n.ordinal()).or(127));
        record.put("rateOfTurn", fix.rateOfTurn().or((byte) -128));
        record.put("speedOverGroundKnots", fix.speedOverGroundKnots().or(-1f));
        record.put("courseOverGroundDegrees", fix.courseOverGroundDegrees().or(-1f));
        record.put("headingDegrees", fix.headingDegrees().or(-1f));
        record.put("aisClass", fix.aisClass() == AisClass.A);
        return record;
    }

    @VisibleForTesting
    static Schema loadSchema() {
        try {
            return new Schema.Parser().parse(Parquet.class.getResourceAsStream("/fixes.avsc"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
