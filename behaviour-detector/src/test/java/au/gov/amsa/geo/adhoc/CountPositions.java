package au.gov.amsa.geo.adhoc;

import java.io.File;

import com.github.davidmoten.rx.Actions;

import au.gov.amsa.ais.rx.Streams;

public class CountPositions {

    public static void main(String[] args) {
        Streams.extract(Streams.nmeaFrom(new File("/media/an/temp/2018-01-29.txt")))// //
                .filter(m -> m.getMessage().isPresent()) //
                .map(m -> m.getMessage().get().message().getMessageId()) //
                .filter(id -> id >= 1 && id <= 3) //
                .count() //
                .doOnNext(Actions.println()) //
                .subscribe();
    }

}
