package au.gov.amsa.util.nmea;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class NmeaMessageParserTest {

    @Test
    public void testNmeaMessageParser() {
        String line = "\\g:3-3-1234*hh\\$ABVSI,r3669961,1,013536.96326433,1386,-98,,*hh";
        NmeaMessage n = NmeaUtil.parseNmea(line);
        assertEquals("3-3-1234", n.getSentenceGroupingFromTagBlock());
        assertNull(n.getSource());
        assertNull(n.getDestination());
        assertNull(n.getLineCount());
        assertNull(n.getRelativeTimeMillis());
        assertNull(n.getText());
        assertNull(n.getUnixTimeMillis());
        assertEquals("$ABVSI", n.getItems().get(0));
        assertEquals("r3669961", n.getItems().get(1));
        assertEquals(8, n.getItems().size());
    }

    @Test
    public void testParsingOfTimeAndSourceInTagBlock() {
        String line = "\\g:1-2-1234,s:r3669961,c:1120959341*hh\\!ABVDM,1,1,1,B,…..,0*hh";
        NmeaMessage n = NmeaUtil.parseNmea(line);
        assertEquals("1-2-1234", n.getSentenceGroupingFromTagBlock());
        assertEquals(1, (int) n.getSentenceNumber());
        assertEquals(2, (int) n.getSentenceCount());
        assertEquals("1234", n.getSentenceGroupId());
        assertEquals("r3669961", n.getSource());
        assertNull(n.getDestination());
        assertNull(n.getLineCount());
        assertNull(n.getRelativeTimeMillis());
        assertNull(n.getText());
        assertEquals(1120959341000L, (long) n.getUnixTimeMillis());
    }

    @Test
    public void testParsingWithoutTagBlock() {
        String line = "$ABVSI,r3669961,1,013536.96326433,1386,-98,,*hh";
        NmeaMessage n = NmeaUtil.parseNmea(line);
        assertEquals("$ABVSI", n.getItems().get(0));
        assertEquals("r3669961", n.getItems().get(1));
        assertEquals(8, n.getItems().size());
        assertNull(n.getSentenceGroupingFromTagBlock());
        assertNull(n.getSentenceCount());
    }

    @Test(expected = NmeaMessageParseException.class)
    public void testBadTagBlockParameterNotSplitWithColon() {
        String line = "\\g*hh\\!ABVDM,1,1,1,B,…..,0*hh";
        NmeaUtil.parseNmea(line);
    }

    @Test(expected = NmeaMessageParseException.class)
    public void testBadTagBlockTooManyColons() {
        String line = "\\g:1:2*hh\\!ABVDM,1,1,1,B,…..,0*hh";
        NmeaUtil.parseNmea(line);
    }

    @Test
    public void testNmeaMessageRelativeTimeAndLineCount() {
        String line = "\\g:1-2-1234,s:r3669961,n:4,r:1120959341*hh\\!ABVDM,1,1,1,B,…..,0*hh";
        NmeaMessage n = NmeaUtil.parseNmea(line);
        assertEquals(1120959341000L, (long) n.getRelativeTimeMillis());
        assertEquals(4, (int) n.getLineCount());
    }

    @Test(expected = NmeaMessageParseException.class)
    public void testNmeaMessageWhenTagBlockNotClosedProperly() {
        String line = "\\g:1-2-1234,s:r3669961,n:4,r:1120959341,some stuff here";
        NmeaUtil.parseNmea(line);
    }

    @Test
    public void testNmeaMessageParsingWhenChecksumNotPresent() {
        String line = "!AIVDM,1,1,,A,H5MfwBTU653hhhiG3Gookn1P=440,0*2F,1334365469";
        NmeaUtil.parseNmea(line);
    }

    @Test
    public void testParseOfNmeaLineWithOnlyTwoElements() {
        String line = "C9,0*5D";
        NmeaUtil.parseNmea(line);
    }

    @Test
    public void testTalkerPartTooShortDoesNotThrowException() {
        String msg = "DM,1,1,,B,37PACP001T8F=lil<<r7dV9R00sh,0*6B";
        new NmeaMessageParser().parse(msg);
    }

    @Test
    public void testMultiSentenceOnLineWithoutTagBlock() {
        String line1 = "!AIVDM,2,1,3,A,57P@t402AG69HPPr2218UHE9LTa>0l58T62222001p@654bd,0*59";
        NmeaMessage n = new NmeaMessageParser().parse(line1);
        assertEquals(1, n.getSentenceNumber().intValue());
        assertEquals(2, n.getSentenceCount().intValue());
        assertEquals("3", n.getSentenceGroupId());
    }

    @Test
    public void testMultiSentenceOnLineWithTagBlock() {
        String line1 = "\\g:1-2-1130*5E\\!BSVDM,2,1,0,A,00000000002,0*3D";
        NmeaMessage n = new NmeaMessageParser().parse(line1);
        assertEquals(1, n.getSentenceNumber().intValue());
        assertEquals(2, n.getSentenceCount().intValue());
        assertEquals("1130", n.getSentenceGroupId());
    }

    @Test
    public void testBadSentenceInfoDoesNotThrowException() {
        String msg = "!ABVDM,2,2,,,88888888800000000000000000000,4*50";
        new NmeaMessageParser().parse(msg);
    }

    @Test(expected = NmeaMessageParseException.class)
    public void testBadSententenceThrowsNmeaMessageParseException() {
        String msg = "\\s:Penrith Island*08\\!ABVDM,1,1\\s:Penrith Island*08\\!ABVDM,1,1,8,B,13bjW80000bcKtkkl=wEroaD28HF,0*12";
        new NmeaMessageParser().parse(msg);
    }

    @Test(expected = NmeaMessageParseException.class)
    public void testGTagDoesNotHaveSufficientPartsThrowsNmeaMessageParseException() {
        LinkedHashMap<String, String> map = Maps.newLinkedHashMap();
        map.put("g", "1-2");
        new NmeaMessage(map, Lists.<String> newArrayList("ABVDN", "2", "2"), "00");
    }

    @Test
    public void testSatelliteMessageFromEv05() {
        String msg = "\\s:rEV05,c:1399340268*3e\\!AIVDM,1,1,,B,19NWq7h02i9q0bGcNT05NDQN04:D,0*3F";
        NmeaMessage n = new NmeaMessageParser().parse(msg);
        assertEquals(1399340268000L, (long) n.getUnixTimeMillis());
    }

    @Test
    public void testParseOnly2ItemsInMessage() {
        String msg = "\\c:1388929778*00\\!ABF?:0000,0*5D";
        NmeaMessage n = new NmeaMessageParser().parse(msg);
        assertEquals(1388929778000L, (long) n.getUnixTimeMillis());
    }

    @Test
    public void testTagBlockThatIncludesAValueWithAColonIsParsedCorrectly() {
        String msg = "\\s:rEV06,c:1418371240,i:|X=1|D=1|T=41985.3864759144|P=10.225.253.129:25479|R=IN|*54\\!AIVDM,1,1,,A,13IMtL01BC4mJ7uurMNWP6;>08H:,0*03";
        NmeaMessage n = new NmeaMessageParser().parse(msg);
        LinkedHashMap<String, String> tags = n.getTags();
        assertEquals("rEV06", tags.get("s"));
        assertEquals("1418371240", tags.get("c"));
        assertEquals("|X=1|D=1|T=41985.3864759144|P=10.225.253.129:25479|R=IN|", tags.get("i"));
        assertEquals(3, tags.size());
    }

    @Test
    public void testNmeaWithBackSlashCorruptionInTagBlock() {
        String msg = "\\s:rEV61,c\\g:1-2-8541,s:rEV61,c:1427240143*20\\!AIVDM,2,1,3,A,53K=Fr42<hQKTP7?KKL<58pUH4j0hDLDp@00000t4T1DD4tj0DTnA3QF@00000,0*18";
        NmeaMessage n = new NmeaMessageParser().parse(msg);
        assertNotNull(n.getTags().get("c\\g"));
    }

    @Test
    public void testNmeaWithBadFormat() {
        String msg = "\\s:Pt Hedland NOMAD,c:14301062\\s:Penrith Island*08\\!ABVDM,1,1,2,B,404k0nQuu=SjC:inuikL5JQ00<09,0*12";
        NmeaMessage n = new NmeaMessageParser().parse(msg);
        assertNull(n.getUnixTimeMillis());
    }

    @Test
    public void testNmeaWithTagBlockOnly() {
        String msg = "\\1G4:53958,s:Gantheaume Pt,c:1481700261*7D\\";
        NmeaMessage m = NmeaUtil.parseNmea(msg);
        assertNotNull(m);
        String s = NmeaUtil.supplementWithTime(msg, 1000);
        assertEquals("\\1G4:53958,s:Gantheaume Pt,c:1481700261,a:1000*0B\\", s);
    }

    @Test
    public void testExtractTagsReturnsEmptyMapWhenDoesNotHaveChecksumDelimiter() {
        assertTrue(NmeaMessageParser.extractTags("c:12334,a:3456").isEmpty());
    }
    
}
