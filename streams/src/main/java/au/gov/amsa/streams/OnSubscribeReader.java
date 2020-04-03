package au.gov.amsa.streams;

import java.io.IOException;
import java.io.Reader;

import rx.Observable;
import rx.Observer;
import rx.observables.SyncOnSubscribe;

public final class OnSubscribeReader extends SyncOnSubscribe<Reader, String> {

    private final Reader reader;
    private final int size;
    
    private final char[] buffer;

    public OnSubscribeReader(Reader reader, int size) {
        this.reader = reader;
        this.size = size;
        this.buffer = new char[size];
    }

    @Override
    protected Reader generateState() {
        return reader;
    }

    @Override
    protected Reader next(Reader reader, Observer<? super String> observer) {
        try {
            int count = reader.read(buffer);
            if (count == -1)
                observer.onCompleted();
            else
                observer.onNext(String.valueOf(buffer, 0, count));
        } catch (IOException e) {
            observer.onError(e);
        }
        return reader;
    }

    public Observable<String> toObservable() {
        return Observable.create(this);
    }
}
