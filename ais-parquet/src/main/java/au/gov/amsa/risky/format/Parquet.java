package au.gov.amsa.risky.format;

import java.io.IOException;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;

//PRE-ALPHA! IN DEVELOPMENT
public class Parquet {

    public static void writeTo(Iterable<GenericData.Record> recordsToWrite, Path path, Schema schema)
            throws IOException {
        try (ParquetWriter<GenericData.Record> writer = AvroParquetWriter.<GenericData.Record>builder(path)
                .withSchema(schema).withConf(new Configuration()).withCompressionCodec(CompressionCodecName.SNAPPY)
                .build()) {

            for (GenericData.Record record : recordsToWrite) {
                writer.write(record);
            }
        }
    }

    private static final Schema SCHEMA = loadSchema();

    public static GenericData.Record toRecord(Fix fix) {
        GenericData.Record record = new GenericData.Record(SCHEMA);
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
        record.put("aisClass", fix.aisClass());
        return record;
    }

    
    public static Schema loadSchema() {
        try {
            return new Schema.Parser().parse(Parquet.class.getResourceAsStream("/fixes.avsc"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
