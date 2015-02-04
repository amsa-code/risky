package au.gov.amsa.ais.rx;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observables.GroupedObservable;
import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.Fix;
import au.gov.amsa.util.Files;

import com.github.davidmoten.rx.Functions;
import com.github.davidmoten.rx.operators.OperatorUnsubscribeEagerly;
import com.github.davidmoten.rx.slf4j.Logging;

public final class BinaryFixesWriter {

	public static Observable<List<Fix>> writeFixes(
			final Func1<Fix, String> fileMapper, Observable<Fix> fixes,
			int bufferSize) {
		return fixes
		// group by filename
				.groupBy(fileMapper)
				// buffer fixes by filename
				.flatMap(buffer(bufferSize))
				// write each list to a file
				.doOnNext(writeFixList(fileMapper));
	}

	private static Func1<GroupedObservable<String, Fix>, Observable<List<Fix>>> buffer(
			final int bufferSize) {
		return new Func1<GroupedObservable<String, Fix>, Observable<List<Fix>>>() {
			@Override
			public Observable<List<Fix>> call(
					GroupedObservable<String, Fix> fileFixes) {
				return fileFixes.buffer(bufferSize);
			}
		};
	}

	private static Action1<List<Fix>> writeFixList(
			final Func1<Fix, String> fileMapper) {
		return new Action1<List<Fix>>() {

			@Override
			public void call(List<Fix> fixes) {
				if (fixes.size() == 0)
					return;
				String filename = fileMapper.call(fixes.get(0));
				writeFixes(fixes, new File(filename), true);
			}

		};
	}

	private static void writeFixes(List<Fix> fixes, File file, boolean append) {
		OutputStream os = null;
		try {
			file.getParentFile().mkdirs();
			os = new BufferedOutputStream(new FileOutputStream(file, append));
			ByteBuffer bb = BinaryFixes.createFixByteBuffer();
			for (Fix fix : fixes) {
				bb.rewind();
				BinaryFixes.write(fix, bb);
				os.write(bb.array());
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} finally {
			if (os != null)
				try {
					os.close();
				} catch (IOException e) {
					// we care because we are writing
					throw new RuntimeException(e);
				}
		}
	}

	public static class ByMonth implements Func1<Fix, String> {

		private final String base;

		public ByMonth(File directory) {
			base = directory.getAbsolutePath();
		}

		@Override
		public String call(Fix fix) {
			DateTime d = new DateTime(fix.getTime(), DateTimeZone.UTC);
			int month = d.getMonthOfYear();
			int year = d.getYear();
			StringBuilder s = new StringBuilder();
			s.append(base);
			s.append(File.separator);
			s.append(year);
			s.append(File.separator);
			s.append(month);
			s.append(File.separator);
			s.append(fix.getMmsi());
			s.append(".track");
			return s.toString();
		}

	}

	public static Observable<Integer> writeFixes(File input,
			Pattern inputPattern, File output, int logEvery,
			int writeBufferSize, Scheduler scheduler, int linesPerProcessor) {
		Observable<File> files = Observable.from(Files
				.find(input, inputPattern));

		deleteDirectory(output);

		Observable<Fix> fixes = files
		// log the filename
				.lift(Logging.<File> log())
				// extract fixes
				.flatMap(extractFixesFromNmeaGz(linesPerProcessor, scheduler))
				// log
				.lift(Logging.<Fix> logger().showCount().showMemory()
						.showRateSince("rate", 5000).every(logEvery).log());

		ByMonth fileMapper = new BinaryFixesWriter.ByMonth(output);

		return BinaryFixesWriter.writeFixes(fileMapper, fixes, writeBufferSize)
		// count number of fixes
				.count()
				// on completion of writing fixes, sort the track files and emit
				// the count of files
				.concatWith(sortOutputFilesByTime(output));
	}

	private static void deleteDirectory(File output) {
		try {
			FileUtils.deleteDirectory(output);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static Func1<File, Observable<Fix>> extractFixesFromNmeaGz(
			final int linesPerProcessor, final Scheduler scheduler) {
		return new Func1<File, Observable<Fix>>() {
			@Override
			public Observable<Fix> call(File file) {
				return Streams.nmeaFromGzip(file.getAbsolutePath())
						.buffer(linesPerProcessor)
						// parse the messages asynchronously using computation
						// scheduler
						.flatMap(new Func1<List<String>, Observable<Fix>>() {
							@Override
							public Observable<Fix> call(List<String> list) {
								return Streams.extractFixes(
										Observable.from(list))
								// do async
										.subscribeOn(scheduler);
							}
						});
			}
		};
	}

	private static Observable<Integer> sortOutputFilesByTime(File output) {
		return Observable.just(1).onBackpressureBuffer()
		// use output lazily
				.map(Functions.constant(output))
				// find the track files
				.flatMap(findTrackFiles())
				// sort the fixes in each one and rewrite
				.flatMap(sortFileFixes())
				// return the count
				.count();
	}

	private static Func1<File, Observable<Integer>> sortFileFixes() {
		return new Func1<File, Observable<Integer>>() {
			@Override
			public Observable<Integer> call(final File file) {
				return BinaryFixes
						.from(file)
						// ensure file is closed in case we want to rewrite
						// downstream
						.lift(OperatorUnsubscribeEagerly.<Fix> instance())
						.toList()
						// sort each list
						.map(sortFixes())
						// replace the file with sorted fixes
						.doOnNext(writeFixes(file))
						// count the fixes
						.count();
			}

		};
	}

	private static Action1<List<Fix>> writeFixes(final File file) {
		return new Action1<List<Fix>>() {
			@Override
			public void call(List<Fix> list) {
				BinaryFixesWriter.writeFixes(list, file, false);
			}
		};
	}

	private static Func1<File, Observable<File>> findTrackFiles() {
		return new Func1<File, Observable<File>>() {
			@Override
			public Observable<File> call(File output) {
				return Observable.from(Files.find(output,
						Pattern.compile("\\d+\\.track")));
			}
		};
	}

	private static Func1<List<Fix>, List<Fix>> sortFixes() {
		return new Func1<List<Fix>, List<Fix>>() {

			@Override
			public List<Fix> call(List<Fix> list) {
				ArrayList<Fix> temp = new ArrayList<Fix>(list);
				Collections.sort(temp, FIX_ORDER_BY_TIME);
				return temp;
			}
		};
	}

	private static final Comparator<Fix> FIX_ORDER_BY_TIME = new Comparator<Fix>() {
		@Override
		public int compare(Fix a, Fix b) {
			return ((Long) a.getTime()).compareTo(b.getTime());
		}
	};

}
