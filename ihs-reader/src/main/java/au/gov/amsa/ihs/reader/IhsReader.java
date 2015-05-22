package au.gov.amsa.ihs.reader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import au.gov.amsa.ihs.model.Ship;

public class IhsReader {

    public Observable<Ship> from(InputStream is) {
        return Observable.just(is).lift(new OperatorIhsReader());
    }

    public static Observable<Ship> fromZip(File file) {
        return shipDataFilesAsInputStreamFromZip(file).lift(new OperatorIhsReader());
    }

    public static Observable<Map<String, Ship>> fromZipAsMapByImo(File file) {
        return fromZip(file).toMap(ship -> ship.getImo());
    }

    public static Observable<Map<String, Ship>> fromZipAsMapByMmsi(File file) {
        return fromZip(file)
        // only ships with an mmsi
                .filter(ship -> ship.getMmsi().isPresent())
                // as map
                .toMap(ship -> ship.getMmsi().get());
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

}
