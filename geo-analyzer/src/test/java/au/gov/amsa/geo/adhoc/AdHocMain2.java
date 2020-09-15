package au.gov.amsa.geo.adhoc;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import au.gov.amsa.ais.HasMmsi;
import au.gov.amsa.ais.message.AisBStaticDataReportPartA;
import au.gov.amsa.ais.message.AisBStaticDataReportPartB;
import au.gov.amsa.ais.message.AisPositionBExtended;
import au.gov.amsa.ais.message.AisShipStatic;
import au.gov.amsa.ais.message.AisShipStaticA;
import au.gov.amsa.ais.rx.Streams;
import au.gov.amsa.risky.format.AisClass;

public class AdHocMain2 {

    public static void extractStatic(File input, File output) throws IOException {
        output.mkdirs();
        try (PrintStream out = new PrintStream(output)) {
            Streams.nmeaFromGzip(new File("/home/dxm/AIS/2020-01-01.txt.gz")) //
                    .compose(o -> Streams.extractMessages(o)) //
                    .filter(x -> (x.message() instanceof AisShipStatic) //
                            || (x.message() instanceof AisBStaticDataReportPartA) //
                            || (x.message() instanceof AisBStaticDataReportPartB)) //
                    .groupBy(x -> ((HasMmsi) x.message()).getMmsi()) //
                    .flatMap(o -> o.scan(new Timed<State>(new State(o.getKey(), null, null, null, null, null), 0L), //
                            (timed, x) -> {
                                State state = timed.object;
                                String name = state.name;
                                String callsign = state.callsign;
                                Integer imo = state.imo;
                                Integer shipType = state.shipType;
                                AisClass aisClass = state.aisClass;
                                if (x.message() instanceof AisShipStaticA) {
                                    AisShipStaticA a = (AisShipStaticA) x.message();
                                    callsign = a.getCallsign();
                                    name = a.getName();
                                    imo = a.getImo().orNull();
                                    shipType = a.getShipType();
                                    aisClass = AisClass.A;
                                } else if (x.message() instanceof AisPositionBExtended) {
                                    AisPositionBExtended a = (AisPositionBExtended) x.message();
                                    name = a.getName();
                                    shipType = a.getShipType();
                                    aisClass = AisClass.B;
                                } else if (x.message() instanceof AisBStaticDataReportPartA) {
                                    AisBStaticDataReportPartA a = (AisBStaticDataReportPartA) x.message();
                                    if (a.getName().isPresent()) {
                                        name = a.getName().get();
                                    }
                                    aisClass = AisClass.B;
                                } else if (x.message() instanceof AisBStaticDataReportPartB) {
                                    aisClass = AisClass.B;
                                }
                                return new Timed<State>(new State(o.getKey(), aisClass, name, callsign, imo, shipType),
                                        x.time());
                            }) //
                            .skip(1) //
                            .distinctUntilChanged(ts -> ts.object))
                    .doOnNext(x -> {
                        State y = x.object;
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                        String line = y.mmsi + "," //
                                + sdf.format(new Date(x.time)) + "," //
                                + y.aisClass + "," //
                                + integer(y.imo) + "," //
                                + integer(y.shipType) + "," //
                                + string(y.name) + ",";
                        out.println(line);
                    }).subscribe();
        }
    }

    private static String string(String s) {
        if (s == null) {
            return "";
        } else {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
    }

    private static String integer(Integer s) {
        if (s == null) {
            return "";
        } else {
            return s.toString();
        }
    }

    public static final class Timed<T> {
        final T object;
        final long time;

        public Timed(T t, long time) {
            this.object = t;
            this.time = time;
        }

        @Override
        public String toString() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return "Timed [time=" + sdf.format(new Date(time)) + ", object=" + object + "]";
        }
    }

    public static final class State {
        final int mmsi;

        // nullable fields
        final AisClass aisClass;
        final String name;
        final String callsign;
        final Integer imo;
        final Integer shipType;

        public State(int mmsi, AisClass aisClass, String name, String callsign, Integer imo, Integer shipType) {
            this.mmsi = mmsi;
            this.aisClass = aisClass;
            this.name = name;
            this.callsign = callsign;
            this.imo = imo;
            this.shipType = shipType;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((aisClass == null) ? 0 : aisClass.hashCode());
            result = prime * result + ((callsign == null) ? 0 : callsign.hashCode());
            result = prime * result + ((imo == null) ? 0 : imo.hashCode());
            result = prime * result + mmsi;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + ((shipType == null) ? 0 : shipType.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            State other = (State) obj;
            if (aisClass != other.aisClass)
                return false;
            if (callsign == null) {
                if (other.callsign != null)
                    return false;
            } else if (!callsign.equals(other.callsign))
                return false;
            if (imo == null) {
                if (other.imo != null)
                    return false;
            } else if (!imo.equals(other.imo))
                return false;
            if (mmsi != other.mmsi)
                return false;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            if (shipType == null) {
                if (other.shipType != null)
                    return false;
            } else if (!shipType.equals(other.shipType))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "State [mmsi=" + mmsi + ", aisClass=" + aisClass + ", name=" + name + ", callsign=" + callsign
                    + ", imo=" + imo + ", shipType=" + shipType + "]";
        }
    }
    
    public static void main(String[] args) throws IOException {
        extractStatic(new File("/home/dxm/AIS/2020-01-01.txt.gz"), new File("target/static-data.txt"));
    }

}
