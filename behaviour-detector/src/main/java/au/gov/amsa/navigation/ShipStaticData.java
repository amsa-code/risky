package au.gov.amsa.navigation;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import com.github.davidmoten.rx.Checked;

import au.gov.amsa.ais.message.AisShipStaticUtil;
import au.gov.amsa.risky.format.AisClass;
import au.gov.amsa.streams.Strings;
import rx.Observable;

public final class ShipStaticData {

    public static class Info {
        public final int mmsi;
        public final Optional<String> imo;
        public final AisClass cls;
        public final Optional<Integer> shipType;
        public final Optional<Float> maxDraftMetres;
        public final Optional<Integer> dimensionAMetres;
        public final Optional<Integer> dimensionBMetres;
        public final Optional<Integer> dimensionCMetres;
        public final Optional<Integer> dimensionDMetres;
        public final Optional<String> name;

        public Info(int mmsi, Optional<String> imo, AisClass cls, Optional<Integer> shipType,
                Optional<Float> maxDraftMetres, Optional<Integer> dimensionAMetres,
                Optional<Integer> dimensionBMetres, Optional<Integer> dimensionCMetres,
                Optional<Integer> dimensionDMetres, Optional<String> name) {
            this.mmsi = mmsi;
            this.imo = imo;
            this.cls = cls;
            this.shipType = shipType;
            this.maxDraftMetres = maxDraftMetres;
            this.dimensionAMetres = dimensionAMetres;
            this.dimensionBMetres = dimensionBMetres;
            this.dimensionCMetres = dimensionCMetres;
            this.dimensionDMetres = dimensionDMetres;
            this.name = name;
        }

        public Optional<Integer> lengthMetres() {
            return AisShipStaticUtil.lengthMetres(dimensionAMetres, dimensionBMetres,
                    dimensionCMetres, dimensionDMetres);
        }

        public Optional<Integer> widthMetres() {
            return AisShipStaticUtil.widthMetres(dimensionAMetres, dimensionBMetres,
                    dimensionCMetres, dimensionDMetres);
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
            builder.append(dimensionAMetres.orElse(-1));
            builder.append(", dimensionBMetres=");
            builder.append(dimensionBMetres.orElse(-1));
            builder.append(", dimensionCMetres=");
            builder.append(dimensionCMetres.orElse(-1));
            builder.append(", dimensionDMetres=");
            builder.append(dimensionDMetres.orElse(-1));
            builder.append(", name=");
            builder.append(name.orElse(""));
            builder.append("]");
            return builder.toString();
        }

    }

    public static Map<Integer, Info> getMapFromResource(String resource) {
        return getMapFromReader(new InputStreamReader(
                ShipStaticData.class.getResourceAsStream(resource), StandardCharsets.UTF_8));
    }

    public static Map<Integer, Info> getMapFromReader(Reader reader) {
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

    public static Observable<Info> from(String resource) {
        return Observable.using(
                () -> new InputStreamReader(ShipStaticData.class.getResourceAsStream(resource)),
                r -> from(r), Checked.a1(r -> r.close()));
    }

    public static Observable<Info> from(Reader reader) {
        return Strings.lines(reader)
                // ignore comments
                .filter(line -> !line.startsWith("#"))
                //
                .map(line -> line.trim())
                //
                .filter(line -> line.length() > 0)
                //
                .map(line -> line.split("\t"))
                //
                .map(items -> {
                    int i = 0;
                    int mmsi = Integer.parseInt(items[i++]);
                    String imoTemp = items[i++];
                    Optional<String> imo;
                    if (imoTemp.trim().length() == 0 || Integer.parseInt(imoTemp.trim()) == -1)
                        imo = Optional.empty();
                    else
                        imo = Optional.of(imoTemp);
                    AisClass cls = items[i++].equals("A") ? AisClass.A : AisClass.B;
                    int type = Integer.parseInt(items[i++]);
                    Optional<Integer> shipType = type == -1 ? empty() : of(type);
                    float draft = Float.parseFloat(items[i++]);
                    Optional<Float> maxDraftMetres = draft == -1 ? empty() : of(draft);
                    int a = Integer.parseInt(items[i++]);
                    int b = Integer.parseInt(items[i++]);
                    int c = Integer.parseInt(items[i++]);
                    int d = Integer.parseInt(items[i++]);
                    Optional<Integer> dimensionAMetres = a == -1 ? empty() : of(a);
                    Optional<Integer> dimensionBMetres = b == -1 ? empty() : of(b);
                    Optional<Integer> dimensionCMetres = c == -1 ? empty() : of(c);
                    Optional<Integer> dimensionDMetres = d == -1 ? empty() : of(d);
                    // skip length and width
                    i += 2;
                    Optional<String> name;
                    if (i >= items.length || items[i].trim().length() == 0)
                        name = Optional.empty();
                    else
                        name = Optional.of(items[i].trim());
                    return new Info(mmsi, imo, cls, shipType, maxDraftMetres, dimensionAMetres,
                            dimensionBMetres, dimensionCMetres, dimensionDMetres, name);
                });
    }
}
