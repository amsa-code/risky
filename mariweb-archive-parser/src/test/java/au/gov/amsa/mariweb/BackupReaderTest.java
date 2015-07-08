package au.gov.amsa.mariweb;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

import org.junit.Test;

import rx.functions.Action1;
import au.gov.amsa.ais.AisMessage;
import au.gov.amsa.ais.AisNmeaMessage;
import au.gov.amsa.util.nmea.NmeaMessage;
import au.gov.amsa.util.nmea.NmeaMessageParser;
import au.gov.amsa.util.nmea.NmeaUtil;

public class BackupReaderTest {

    @Test
    public void testRowsInSampleParseOk() {
        InputStream is = OperatorExtractValuesFromInsertStatementTest.class
                .getResourceAsStream("/mariweb-backup-sample.txt");
        BackupReader.getNmea(is).subscribe(new Action1<String>() {
            @Override
            public void call(String line) {
                System.out.println(line);
                NmeaUtil.parseNmea(line);
            }
        });
    }

    @Test
    public void testFirstRowFromActualSample() {
        InputStream is = OperatorExtractValuesFromInsertStatementTest.class
                .getResourceAsStream("/mariweb-backup-sample.txt");
        String nmea = BackupReader.getNmea(is).toBlocking().first();
        System.out.println(nmea);
        assertEquals(
                "\\s:Lockhart River,c:1325980876,at:1383734682*69\\!AIVDM,1,1,,B,1E@LR2200Rbrdgqe@Pl68S4H0000,0*07",
                nmea);
        NmeaMessage n = new NmeaMessageParser().parse(nmea);
        assertEquals("1383734682", n.getTags().get("at"));
        assertEquals("1325980876", n.getTags().get("c"));
        AisMessage m = new AisNmeaMessage(nmea).getMessage();
        assertEquals("Lockhart River", m.getSource());
        assertEquals(1, m.getMessageId());
        assertEquals("69", NmeaUtil.getChecksum("s:Lockhart River,c:1325980876,at:1383734682"));

    }

    @Test
    public void testExtractValuesForMultilineMessage() {
        List<String> list = BackupReader
                .getNmea(
                        new ByteArrayInputStream(
                                "INSERT INTO `ITU411_data` VALUES (11861,'2013-04-16 01:35:43','2013-04-16 01:35:43','203.9.160.33:4001','IN','!AIVDM,2,1,3,B,58Hu?002;ASoUH4OF20P4V1<PV22222222222216O@W>O4E6NEB0FH43,0*6C|!AIVDM,2,2,3,B,jCU888888888880,2*60','\\\\i:|D=0|T=41380.0664713773|P=203.9.160.33:4001|R=IN|*36\\\\',5,0,'563040000',0,9127485,'9VAG5  ','HAI SHI             ',70,250,39,14,31,1,'01100630',8.5,'HAY POINT           ',0)"
                                        .getBytes())).toList().toBlocking().single();
        System.out.println(list);
        assertEquals(2, list.size());
    }

    public static void main(String[] args) throws FileNotFoundException {
        System.out.println(BackupReader.getNmea(new FileInputStream("/home/dxm/temp.txt")).count()
                .toBlocking().single());

    }

}
