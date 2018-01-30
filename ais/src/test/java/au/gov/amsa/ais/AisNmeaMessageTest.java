package au.gov.amsa.ais;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Date;
import java.util.List;

import org.junit.Test;

import au.gov.amsa.ais.message.AisAidToNavigation;
import au.gov.amsa.util.nmea.NmeaMessage;
import au.gov.amsa.util.nmea.NmeaUtil;
import au.gov.amsa.util.nmea.Talker;

public class AisNmeaMessageTest {

    @Test
    public void testAisNmeaMessage() {
        String m = "$BSVDM,2,2,5,B,00000000008,2*33";
        AisNmeaMessage a = new AisNmeaMessage(m);
        assertEquals("B", a.getChannel());
        assertEquals("$BSVDM", a.getFormat());
        assertEquals(2, a.getFragmentCount());
        assertEquals(2, a.getFragmentNumber());
        assertEquals("5", a.getSequentialMessageId());
        assertEquals("33", a.getChecksum());
        assertEquals(Talker.UNKNOWN, a.getTalker());
        assertNotNull(a.getNmea());
        a.getMessage();

    }

    @Test(expected = AisParseException.class)
    public void testInvalidLine() {
        new AisNmeaMessage("");
    }

    @Test(expected = AisParseException.class)
    public void testInvalidLineLessThan7Columns() {
        new AisNmeaMessage("$BSVDM,2,2*4E");
    }

    @Test(expected = AisParseException.class)
    public void testHasNoChecksum() {
        new AisNmeaMessage("$BSVDM,2,2,5,B,00000000008,2");
    }

    @Test(expected = AisParseException.class)
    public void testChecksumDoesNotMatch() {
        new AisNmeaMessage("$BSVDM,2,2,5,B,00000000008,2*34");
    }

    @Test(expected = AisParseException.class)
    public void testRuntimeExceptionBecomesAisParseException() {
        new AisNmeaMessage(
                "$GPGGA,024654.00,3351.0141,S,15117.2167,E,1,06,1.06,00105,M,021,M,,*77");
    }

    @Test
    public void testGetTimeObtainsTimeFromTagBlock() {
        String line = "\\g:1-2-1234,s:r3669961,c:1120959341*51\\$BSVDM,2,2,5,B,00000000008,2*33";
        AisNmeaMessage m = new AisNmeaMessage(line);
        assertEquals(1120959341000L, (long) m.getTime());
    }

    @Test
    public void testGetTimeObtainsTimeFromTagBlock2() {
        String line = "\\c:1357563697*00\\!AIVDM,1,1,,B,13b2AH8000bkvNajJ=1ov2C>25`4,0*0F";
        AisNmeaMessage m = new AisNmeaMessage(line);
        assertEquals(1357563697000L, (long) m.getTime());
        assertEquals(1357563697000L, m.getTimestampedMessage().time());
    }

    @Test
    public void testFragmentCount() {
        String line = "\\g:1-2-1536,c:1334258609*2F\\!BSVDM,2,1,0,A,577V7s02?k61I8Lg<00Dq@E918U<F1=@58000016Op`BL5D8tIm5@PDPCp0T,0*5B";
        NmeaMessage m = NmeaUtil.parseNmea(line);
        List<String> items = m.getItems();
        assertEquals("!BSVDM", items.get(0));
        assertEquals("2", items.get(1));
        assertEquals("1", items.get(2));
    }

    @Test
    public void testOrbcom() {
        String line = "\\s:Orbcom,q:v,c:1492562037,i:|X=1|D=1|T=42844.0236241782|P=127.0.0.1:12086|R=IN|,a:1492562041258*64\\!AIVDM,1,1,,A,13@e?B01BVVCD`L3Flm3IRoj0<0A,0*30";
        AisNmeaMessage m = AisNmeaMessage.from(line);
        assertEquals(1492562037000L, (long) m.getTime());
        assertEquals(1492562037000L, m.getTimestampedMessage().time());
        System.out.println(m.getTimestampedMessage());
        System.out.println(new Date(m.getTimestampedMessage().time()));
    }

    @Test
    public void testMissingMessageProblemFromMariwebExtract() {
        String line = "\\s:AISSat_2,c:1495765889,T:2017-05-26 02.31.29*24\\!AIVDM,1,1,,A,15Qp0b0029:obOH0wMO6k5Dp00S7,0*17";
        AisNmeaMessage m = AisNmeaMessage.from(line);
        assertEquals(1495765889000L, (long) m.getTime());
        assertEquals(1495765889000L, m.getTimestampedMessage().time());
        System.out.println(m.getTimestampedMessage());
        System.out.println(new Date(m.getTimestampedMessage().time()));
    }

    @Test
    public void testShipStaticDataFail() {
        String line = "\\s:DAMPIER,c:1495795358,T:2017-05-26 10.42.38*1D\\!ABVDM,2,1,3,B,57PK@>423P<iHP<F220=<j1LQT4hh62222222216>0M<<4t20>@hD1H4,0*13";
        AisNmeaMessage m = AisNmeaMessage.from(line);
    }
    
    @Test(expected=AisParseException.class)
    public void testParseCorruptAisAidToNavigationThrows() {
        String line = "\\s:Kordia Terrestrial,c:1517229997,seq:11725574*18\\!AIVDM,2,1,8,B,E>m1cBFch80W4PP000000000000FDvc;lecp@00003j,0*11";
        AisNmeaMessage m = AisNmeaMessage.from(line);
        AisAidToNavigation aid =  (AisAidToNavigation) m.getMessage();
        aid.getTimeSecondsOnly();
    }

    public static void main(String[] args) {
        System.out.println(AisNmeaMessage
                .from("\\s:Orbcom,q:v,c:1492561005*1A\\!AIVDM,1,1,,A,18JN>2000r`FN@SlS4iTc3mJ0@J2,0*57")
                .getTimestampedMessage());
        ;
    }
}
