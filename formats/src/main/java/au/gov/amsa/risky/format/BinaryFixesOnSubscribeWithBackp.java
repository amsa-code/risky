package au.gov.amsa.risky.format;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;
import java.util.zip.GZIPInputStream;

import com.github.davidmoten.util.Optional;

import au.gov.amsa.risky.format.BinaryFixesOnSubscribeWithBackp.State;
import rx.Observable;
import rx.Observer;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.observables.SyncOnSubscribe;

public final class BinaryFixesOnSubscribeWithBackp extends SyncOnSubscribe<State, Fix> {

    private final InputStream is;
    private final Optional<Integer> mmsi;
    private final BinaryFixesFormat format;

    public BinaryFixesOnSubscribeWithBackp(InputStream is, Optional<Integer> mmsi,
            BinaryFixesFormat format) {
        this.is = is;
        this.mmsi = mmsi;
        this.format = format;
    }

    public final static class State {
        final InputStream is;
        final Optional<Integer> mmsi;
        final Queue<Fix> queue;

        public State(InputStream is, Optional<Integer> mmsi, Queue<Fix> queue) {
            this.is = is;
            this.mmsi = mmsi;
            this.queue = queue;
        }

    }

    /**
     * Returns stream of fixes from the given file. If the file name ends in
     * '.gz' then the file is unzipped before being read.
     * 
     * @param file
     * @return fixes stream
     */
    public static Observable<Fix> from(final File file, BinaryFixesFormat format) {

        Func0<InputStream> resourceFactory = new Func0<InputStream>() {

            @Override
            public InputStream call() {
                try {
                    if (file.getName().endsWith(".gz"))
                        return new GZIPInputStream(new FileInputStream(file));
                    else
                        return new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        Func1<InputStream, Observable<Fix>> obsFactory = new Func1<InputStream, Observable<Fix>>() {

            @Override
            public Observable<Fix> call(InputStream is) {
                Optional<Integer> mmsi;
                if (format == BinaryFixesFormat.WITH_MMSI)
                    mmsi = Optional.absent();
                else
                    mmsi = Optional.of(BinaryFixesUtil.getMmsi(file));

                return Observable.create(new BinaryFixesOnSubscribeWithBackp(is, mmsi, format));
            }
        };
        Action1<InputStream> disposeAction = new Action1<InputStream>() {

            @Override
            public void call(InputStream is) {
                try {
                    is.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        return Observable.using(resourceFactory, obsFactory, disposeAction, true);

    }

    @Override
    protected State generateState() {
        return new State(is, mmsi, new LinkedList<Fix>());
    }

    @Override
    protected State next(State state, Observer<? super Fix> observer) {
        int recordSize = BinaryFixes.recordSize(format);
        Fix f = state.queue.poll();
        if (f != null)
            observer.onNext(f);
        else {
            byte[] bytes = new byte[4096 * BinaryFixes.recordSize(format)];
            int length;
            try {
                if ((length = state.is.read(bytes)) > 0) {
                    for (int i = 0; i < length; i += recordSize) {
                        ByteBuffer bb = ByteBuffer.wrap(bytes, i, recordSize);
                        final int mmsi;
                        if (state.mmsi.isPresent()) {
                            mmsi = state.mmsi.get();
                        } else {
                            mmsi = bb.getInt();
                        }
                        Fix fix = BinaryFixesUtil.toFix(mmsi, bb);
                        state.queue.add(fix);
                    }
                    observer.onNext(state.queue.remove());
                } else
                    observer.onCompleted();
            } catch (IOException e) {
                observer.onError(e);
            }
        }
        return state;
    }

}
