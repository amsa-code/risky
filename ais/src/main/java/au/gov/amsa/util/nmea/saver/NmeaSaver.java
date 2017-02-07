package au.gov.amsa.util.nmea.saver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.schedulers.Schedulers;
import au.gov.amsa.util.nmea.NmeaUtil;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;

public class NmeaSaver {

	private static Logger log = LoggerFactory.getLogger(NmeaSaver.class);

	private volatile Subscriber<String> subscriber;

	private final FileFactory factory;

	private final Observable<String> source;

	private final Clock clock;

	@VisibleForTesting
	NmeaSaver(Observable<String> nmea, FileFactory factory, Clock clock) {
		this.source = nmea;
		this.factory = factory;
		this.clock = clock;
	}

	public NmeaSaver(Observable<String> nmea, FileFactory factory) {
		this(nmea, factory, new SystemClock());
	}

	public void start() {
		start(Schedulers.io());
	}

	public void start(Scheduler scheduler) {
		subscriber = createSubscriber(factory, clock);
		source.subscribeOn(scheduler).subscribe(subscriber);
	}

	public void stop() {
		if (subscriber != null)
			subscriber.unsubscribe();
	}

	private static Subscriber<String> createSubscriber(
			final FileFactory factory, final Clock clock) {

		return new Subscriber<String>() {

			Optional<BufferedWriter> current = Optional.absent();
			Optional<String> currentKey = Optional.absent();
			boolean firstLineInFile = true;

			@Override
			public void onCompleted() {
				log.warn("should not complete");
				closeCurrentWriter();
			}

			@Override
			public void onError(Throwable e) {
				log.error(e.getMessage(), e);
				closeCurrentWriter();
			}

			private void closeCurrentWriter() {
				if (current.isPresent())
					try {
						current.get().close();
					} catch (IOException e1) {
						log.error(e1.getMessage(), e1);
					}
			}

			@Override
			public void onNext(String line) {
				try {
					long now = clock.getTimeMs();
					String amendedLine = NmeaUtil.supplementWithTime(line, now);
					String key = factory.key(amendedLine, now);
					if (!currentKey.isPresent()
							|| !key.equals(currentKey.get())) {
						if (current.isPresent())
							current.get().close();
						File file = factory.file(amendedLine, now);
						firstLineInFile = !file.exists();
						current = Optional.of((new BufferedWriter(
								new OutputStreamWriter(new FileOutputStream(
										file, true)))));
						currentKey = Optional.of(key);
					}
					if (!firstLineInFile)
						current.get().write('\n');
					firstLineInFile = false;
					current.get().write(amendedLine);
				} catch (IOException e) {
					log.error(e.getMessage(), e);
				} catch (RuntimeException e) {
					// could not parse the message, ignore
					// log.warn(e.getMessage() + ":" + line, e);
				}
			}
		};
	}

}
