package au.gov.amsa.ihs.reader;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.joda.time.DateTime;

import com.google.common.base.Optional;

import au.gov.amsa.ihs.model.Ship;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;

public class IhsReader {

    public Observable<Map<String, String>> from(InputStream is) {
        return Observable.just(is).lift(new OperatorIhsReader());
    }

    public static Observable<Map<String, String>> fromZip(File file) {
        return shipDataFilesAsInputStreamFromZip(file).lift(new OperatorIhsReader());
    }

    public static Observable<Map<String, Map<String, String>>> fromZipAsMapByImo(File file) {
        return fromZip(file).toMap(map -> map.get(Key.LRIMOShipNo.name()));
    }

    public static Observable<Map<String, Map<String, String>>> fromZipAsMapByMmsi(File file) {
        return fromZip(file)
                // only ships with an mmsi
                .filter(map -> map.get(Key.MaritimeMobileServiceIdentityMMSINumber.name()) != null)
                // as map
                .toMap(map -> map.get(Key.MaritimeMobileServiceIdentityMMSINumber.name()));
    }

    private static Observable<InputStream> shipDataFilesAsInputStreamFromZip(final File file) {
        return Observable.create(new OnSubscribe<InputStream>() {

            @Override
            public void call(Subscriber<? super InputStream> subscriber) {

                ZipFile zip = null;
                try {
                    zip = new ZipFile(file);
                    Enumeration<? extends ZipEntry> en = zip.entries();
                    while (en.hasMoreElements() && !subscriber.isUnsubscribed()) {
                        ZipEntry entry = en.nextElement();
                        if (entry.getName().startsWith("ShipData")
                                && entry.getName().endsWith(".xml")) {
                            InputStream is = zip.getInputStream(entry);
                            System.out.println(entry.getName());
                            subscriber.onNext(is);
                        }
                    }
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                } finally {
                    try {
                        if (zip != null)
                            zip.close();
                    } catch (IOException e) {
                        // don't care
                    }
                }
            }

        });
    }

    public static Ship toShip(Map<String, String> values) {
        return new ShipCreator(values).buildShip();
    }

    private static class ShipCreator {
        private final Map<String, String> values;

        ShipCreator(Map<String, String> values) {
            this.values = values;
        }

        Ship buildShip() {
            return Ship.builder().classificationSocietyCode(value("ClassificationSocietyCode"))
                    .countryOfBuildCode(value("CountryOfBuildCode"))
                    .yearOfBuild(toYearOfBuild(value("DateOfBuild")))
                    .monthOfBuild(toMonthOfBuild(value("DateOfBuild"))).flagCode(value("FlagCode"))
                    .grossTonnage(toLong(value("GrossTonnage")))
                    .groupBeneficialOwnerCompanyCode(value("GroupBeneficialOwnerCompanyCode"))
                    .groupBeneficialOwnerCountryOfDomicileCode(
                            value("GroupBeneficialOwnerCountryofDomicileCode"))
                    .imo(value("LRIMOShipNo").get())
                    .mmsi(value("MaritimeMobileServiceIdentityMMSINumber"))
                    .type2(value("ShiptypeLevel2")).type3(value("ShiptypeLevel3"))
                    .type4(value("ShiptypeLevel4")).type5(value("ShiptypeLevel5"))
                    .deadweightTonnage(toFloat(value("FormulaDWT"))).statCode5(value("StatCode5"))
                    .lengthOverallMetres(toFloat(value("LengthOverallLOA")))
                    .breadthMetres(toFloat(value("Breadth")))
                    .displacementTonnage(toFloat(value("Displacement")))
                    .draughtMetres(toFloat(value("Draught"))).speedKnots(toFloat(value("Speed")))
                    .lastUpdateTime(toDateTime(value("LastUpdateDate"))).name(value("ShipName"))
                    .shipBuilderCompanyCode(value("ShipbuilderCompanyCode"))
                    // build it
                    .build();
        }

        private Optional<String> value(String name) {
            String val = values.get(name);
            if (val == null)
                return absent();
            else if (val.trim().length() == 0)
                return absent();
            else
                return Optional.fromNullable(val.trim());
        }

    }

    private static Optional<DateTime> toDateTime(Optional<String> value) {
        if (!value.isPresent())
            return absent();
        else {
            try {
                return of(DateTime.parse(value.get()));
            } catch (RuntimeException e) {
                return absent();
            }
        }
    }

    private static Optional<Integer> toYearOfBuild(Optional<String> value) {
        if (!value.isPresent())
            return absent();
        else {
            try {
                return of(Integer.parseInt(value.get().substring(0, 4)));
            } catch (RuntimeException e) {
                return absent();
            }
        }
    }

    private static Optional<Integer> toMonthOfBuild(Optional<String> value) {
        if (!value.isPresent())
            return absent();
        else {
            int month = Integer.parseInt(value.get().substring(4, 6));
            if (month > 0)
                return of(month);
            else
                return absent();
        }
    }

    private static Optional<Long> toLong(Optional<String> value) {
        if (!value.isPresent())
            return Optional.absent();
        else {
            return Optional.of(Long.parseLong(value.get()));
        }
    }

    private static Optional<Float> toFloat(Optional<String> value) {
        if (!value.isPresent())
            return Optional.absent();
        else {
            return Optional.of(Float.parseFloat(value.get()));
        }
    }

}
