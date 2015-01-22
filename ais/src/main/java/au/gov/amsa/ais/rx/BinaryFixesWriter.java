package au.gov.amsa.ais.rx;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observables.GroupedObservable;
import au.gov.amsa.risky.format.BinaryFixes;
import au.gov.amsa.risky.format.Fix;

public class BinaryFixesWriter {

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
				OutputStream os = null;
				try {
					File file = new File(filename);
					file.getParentFile().mkdirs();
					os = new BufferedOutputStream(new FileOutputStream(file,
							true));
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
		};
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
			s.append(".trace");
			return s.toString();
		}

	}

}
