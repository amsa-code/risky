package au.gov.amsa.risky.format;

import static au.gov.amsa.risky.format.BinaryFixes.BINARY_FIX_BYTES;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.Subscription;

public class BinaryFixesOnSubscribeFastPath implements OnSubscribe<Fix> {

    private static final Logger log = LoggerFactory.getLogger(BinaryFixesOnSubscribeFastPath.class);

    private final File file;

    public BinaryFixesOnSubscribeFastPath(File file) {
        this.file = file;
    }

    public static Observable<Fix> from(File file) {
        return Observable.create(new BinaryFixesOnSubscribeFastPath(file));
    }

    @Override
    public void call(Subscriber<? super Fix> subscriber) {
        FileInputStream fis = null;
        AtomicBoolean closed = new AtomicBoolean(false);
        try {
            fis = new FileInputStream(file);
            subscriber.add(createSubscription(fis, closed));
            reportFixes(BinaryFixesUtil.getMmsi(file), subscriber, fis);
            if (!subscriber.isUnsubscribed()) {
                // eagerly close
                if (closed.compareAndSet(false, true))
                    fis.close();
                subscriber.onCompleted();
            }
        } catch (Exception e) {
            if (!subscriber.isUnsubscribed())
                subscriber.onError(e);
        }
    }

    private Subscription createSubscription(final FileInputStream fis, final AtomicBoolean closed) {
        return new Subscription() {

            volatile boolean subscribed = true;

            @Override
            public void unsubscribe() {
                subscribed = false;
                try {
                    if (closed.compareAndSet(false, true))
                        fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public boolean isUnsubscribed() {
                return !subscribed;
            }
        };
    }

    private static void reportFixes(long mmsi, Subscriber<? super Fix> subscriber, InputStream fis)
            throws IOException {
        byte[] bytes = new byte[4096 * BINARY_FIX_BYTES];
        int length = 0;
        if (subscriber.isUnsubscribed())
            return;
        while ((length = fis.read(bytes)) > 0) {
            for (int i = 0; i < length; i += BINARY_FIX_BYTES) {
                if (subscriber.isUnsubscribed())
                    return;
                ByteBuffer bb = ByteBuffer.wrap(bytes, i, BINARY_FIX_BYTES);
                try {
                    Fix fix = BinaryFixesUtil.toFix(mmsi, bb);
                    subscriber.onNext(fix);
                } catch (RuntimeException e) {
                    log.warn(e.getMessage(), e);
                }
            }
        }
    }

}
