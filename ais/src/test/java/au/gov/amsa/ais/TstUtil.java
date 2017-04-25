package au.gov.amsa.ais;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import au.gov.amsa.util.nmea.NmeaMessage;
import au.gov.amsa.util.nmea.NmeaReader;
import au.gov.amsa.util.nmea.NmeaUtil;

public class TstUtil {

    static void handleAisStream(InputStream is, PrintStream messagesWithTimestamp, PrintStream out,
            PrintStream console) throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        DecimalFormat df = new DecimalFormat("00");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = br.readLine()) != null) {
            out.println(line);
            messagesWithTimestamp.println(line + "," + System.currentTimeMillis());
            try {
                AisNmeaMessage nmea = new AisNmeaMessage(line);
                Timestamped<AisMessage> message = nmea.getTimestampedMessage();
                out.println(message);
                out.println("talker=" + nmea.getTalker().getDescription());
                if (message.message().getMessageId() <= 3 && message.message().getMessageId() > 0) {
                    Communications comms = ((HasCommunications) message.message()).getCommunications();
                    if (comms.getMinutesUtc() != null) {
                        String commsTime = df.format(comms.getHourUtc()) + ":"
                                + df.format(comms.getMinuteUtc());
                        String arrivalTime = sdf.format(new Date(message.time()));
                        String output = "comms,arrival=" + commsTime + "," + arrivalTime;
                        if (!commsTime.equals(arrivalTime)) {
                            console.println(output + " " + line);
                        }
                        out.println(output + "\t" + line);
                    }
                }
            } catch (RuntimeException e) {
                out.println("-------------- ERROR: " + e.getMessage());
            }
        }
        br.close();
    }

    public static String insertNewLines(Object s) {
        return s.toString().replaceAll(",", ",\n");
    }

    static NmeaStreamProcessorListener createLoggingListener(final PrintStream out) {
        return new NmeaStreamProcessorListener() {

            int count = 0;

            @Override
            public void message(String line, long time) {
                count++;
                out.println(count + " " + line + " -> " + new Date(time));
            }

            @Override
            public void timestampNotFound(String line, Long arrivalTime) {
                String commsDate = "";
                String className = "";
                try {
                    AisNmeaMessage nmea = new AisNmeaMessage(line);
                    AisMessage message = nmea.getMessage();
                    className = message.getClass().getSimpleName();
                    if (message instanceof HasCommunications) {
                        Communications comms = ((HasCommunications) message).getCommunications();
                        if (comms.getMinutesUtc() != null) {
                            DecimalFormat df = new DecimalFormat("00");
                            commsDate = df.format(comms.getHourUtc()) + ":"
                                    + df.format(comms.getMinuteUtc());
                        }
                    }
                } catch (AisParseException e) {

                }
                out.println(count++ + " " + line + " -> " + new Date(arrivalTime) + " NOT FOUND "
                        + className + " " + commsDate);
            }

            @Override
            public void invalidNmea(String line, long arrivalTime, String message) {
                out.println("invalid nmea: " + line);
            }
        };
    }

    static void process(NmeaReader con, NmeaStreamProcessorListener listener, final PrintStream out) {

        NmeaStreamProcessor p = new NmeaStreamProcessor(listener, true);
        for (String line : con.read()) {
            // System.out.println(line);
            NmeaMessage nmea = NmeaUtil.parseNmea(line);
            long time = Long.parseLong(nmea.getItems().get(nmea.getItems().size() - 1));
            int i = line.lastIndexOf(",");
            p.line(line.substring(0, i), time);
        }
        out.println("buffer remaining=" + p.getBuffer().size());
        out.println("buffer=");
        for (LineAndTime s : p.getBuffer())
            out.println(s);

    }

}
