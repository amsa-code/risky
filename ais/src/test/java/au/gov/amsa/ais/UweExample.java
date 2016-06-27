package au.gov.amsa.ais;

import java.io.File;
import java.nio.charset.StandardCharsets;

import au.gov.amsa.streams.StringSockets;
import au.gov.amsa.util.nmea.saver.FileFactoryPerDay;
import au.gov.amsa.util.nmea.saver.NmeaSaver;
import rx.Observable;
import rx.schedulers.Schedulers;

public class UweExample {

    public static void main(String[] args) {
        File directory = new File("target/days");
        directory.mkdir();
        int quietTimeoutMs = 10000;
        long reconnectDelayMs = 5000;
        Observable<String> nmea = StringSockets.from("mariweb.amsa.gov.au", 9010, quietTimeoutMs,
                reconnectDelayMs, StandardCharsets.UTF_8, Schedulers.trampoline());

        // Uwe you would use
        // StringSockets.from(socketCreator, quietTimeoutMs, reconnectDelayMs,
        // StandardCharsets.UTF_8, Schedulers.trampoline());
        // and the socketCreator lambda would set up an SSL socket

        NmeaSaver saver = new NmeaSaver(nmea, new FileFactoryPerDay(directory));
        saver.start(Schedulers.trampoline());
    }

}
