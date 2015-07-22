package au.gov.amsa.navigation;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;

import rx.Observable;
import au.gov.amsa.risky.format.AisClass;
import au.gov.amsa.streams.Strings;

import com.github.davidmoten.rx.Checked;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;

public final class ShipStaticData {

    public static class Info {
        public final long mmsi;
        public final Optional<String> imo;
        public final AisClass cls;
        public final Optional<Integer> shipType;
        public final Optional<Float> maxDraftMetres;
        public final Optional<Integer> dimensionAMetres;
        public final Optional<Integer> dimensionBMetres;
        public final Optional<Integer> dimensionCMetres;
        public final Optional<Integer> dimensionDMetres;

        public Info(long mmsi, Optional<String> imo, AisClass cls, Optional<Integer> shipType,
                Optional<Float> maxDraftMetres, Optional<Integer> dimensionAMetres,
                Optional<Integer> dimensionBMetres, Optional<Integer> dimensionCMetres,
                Optional<Integer> dimensionDMetres) {
            this.mmsi = mmsi;
            this.imo = imo;
            this.cls = cls;
            this.shipType = shipType;
            this.maxDraftMetres = maxDraftMetres;
            this.dimensionAMetres = dimensionAMetres;
            this.dimensionBMetres = dimensionBMetres;
            this.dimensionCMetres = dimensionCMetres;
            this.dimensionDMetres = dimensionDMetres;
        }

        public Optional<Integer> lengthMetres() {
            Optional<Integer> a = dimensionAMetres;
            Optional<Integer> b = dimensionBMetres;
            if (a.isPresent() && b.isPresent())
                return Optional.of(a.get() + b.get());
            else {
                Optional<Integer> c = dimensionCMetres;
                Optional<Integer> d = dimensionDMetres;
                if (!a.isPresent() && !c.isPresent() && b.isPresent() && d.isPresent())
                    return b;
                else
                    return Optional.absent();
            }
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Info [mmsi=");
            builder.append(mmsi);
            builder.append(", imo=");
            builder.append(imo);
            builder.append(", cls=");
            builder.append(cls);
            builder.append(", shipType=");
            builder.append(shipType);
            builder.append(", maxDraftMetres=");
            builder.append(maxDraftMetres);
            builder.append(", dimensionAMetres=");
            builder.append(dimensionAMetres);
            builder.append(", dimensionBMetres=");
            builder.append(dimensionBMetres);
            builder.append(", dimensionCMetres=");
            builder.append(dimensionCMetres);
            builder.append(", dimensionDMetres=");
            builder.append(dimensionDMetres);
            builder.append("]");
            return builder.toString();
        }

    }

    public static Map<Long, Info> getMapFromResource(String resource) {
        return getMapFromReader(new InputStreamReader(
                ShipStaticData.class.getResourceAsStream(resource), Charsets.UTF_8));
    }

    public static Map<Long, Info> getMapFromReader(Reader reader) {
        return ShipStaticData
        // read ship static data from classpath
                .from(reader)
                // make a map
                .toMap(info -> info.mmsi)
                // go
                .toBlocking().single();
    }

    public static Observable<Info> fromAndClose(Reader reader) {
        return Observable.using(() -> reader, r -> from(r), Checked.a1(r -> r.close()));
    }

    public static Observable<Info> from(Reader reader) {
        return Strings
                .lines(reader)
                // ignore comments
                .filter(line -> !line.startsWith("#"))
                .map(line -> line.trim())
                .filter(line -> line.length() > 0)
                .map(line -> line.split(","))
                .map(items -> {
                    int i = 0;
                    long mmsi = Long.parseLong(items[i++]);
                    String imoTemp = items[i++];
                    Optional<String> imo;
                    if (imoTemp.trim().length() == 0)
                        imo = Optional.absent();
                    else
                        imo = Optional.of(imoTemp);
                    AisClass cls = items[i++].equals("A") ? AisClass.A : AisClass.B;
                    int type = Integer.parseInt(items[i++]);
                    Optional<Integer> shipType = type == -1 ? absent() : of(type);
                    float draft = Float.parseFloat(items[i++]);
                    Optional<Float> maxDraftMetres = draft == -1 ? absent() : of(draft);
                    int a = Integer.parseInt(items[i++]);
                    int b = Integer.parseInt(items[i++]);
                    int c = Integer.parseInt(items[i++]);
                    int d = Integer.parseInt(items[i++]);
                    Optional<Integer> dimensionAMetres = a == -1 ? absent() : of(a);
                    Optional<Integer> dimensionBMetres = b == -1 ? absent() : of(b);
                    Optional<Integer> dimensionCMetres = c == -1 ? absent() : of(c);
                    Optional<Integer> dimensionDMetres = d == -1 ? absent() : of(d);

                    return new Info(mmsi, imo, cls, shipType, maxDraftMetres, dimensionAMetres,
                            dimensionBMetres, dimensionCMetres, dimensionDMetres);
                });
    }
}
