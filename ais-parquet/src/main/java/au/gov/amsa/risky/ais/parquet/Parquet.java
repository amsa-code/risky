package au.gov.amsa.risky.parquet;

import java.io.IOException;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;

import com.github.davidmoten.guavamini.annotations.VisibleForTesting;

import au.gov.amsa.risky.format.AisClass;
import au.gov.amsa.risky.format.Fix;

//PRE-ALPHA! IN DEVELOPMENT
public class Parquet {

    public static void writeTo(Iterable<GenericData.Record> recordsToWrite, Path path)
            throws IOException {
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

    public static final Schema FIX_WITH_MMSI_SCHEMA = loadSchema();

    public static GenericData.Record toRecord(Fix fix) {
        GenericData.Record record = new GenericData.Record(FIX_WITH_MMSI_SCHEMA);
        record.put("mmsi", fix.mmsi());
        record.put("lat", fix.lat());
        record.put("lon", fix.lon());
        record.put("timeEpochMs", fix.time());
        record.put("latencySeconds", fix.latencySeconds().orElse(-1));
        record.put("source", (int) fix.source().orElse((short) 0));
        record.put("navigationalStatus", fix.navigationalStatus().map(n -> n.ordinal()).orElse(127));
        record.put("rateOfTurn", fix.rateOfTurn().orElse((byte) -128));
        record.put("speedOverGroundKnots", fix.speedOverGroundKnots().orElse(-1f));
        record.put("courseOverGroundDegrees", fix.courseOverGroundDegrees().orElse(-1f));
        record.put("headingDegrees", fix.headingDegrees().orElse(-1f));
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
