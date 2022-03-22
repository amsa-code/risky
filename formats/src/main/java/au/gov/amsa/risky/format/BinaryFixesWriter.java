package au.gov.amsa.risky.format;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.zip.GZIPOutputStream;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.github.davidmoten.util.Preconditions;
import com.google.common.util.concurrent.Striped;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observables.GroupedObservable;

public final class BinaryFixesWriter {

    public static Observable<List<Fix>> writeFixes(final Func1<Fix, String> fileMapper,
            Observable<Fix> fixes, int bufferSize, boolean zip, BinaryFixesFormat format) {
        return fixes
                // group by filename
                .groupBy(fileMapper)
                // buffer fixes by filename
                .flatMap(buffer(bufferSize))
                // write each list to a file
                .doOnNext(writeFixList(fileMapper, zip, format)) //
                .doOnNext(System.out::println);
    }

    private static Func1<GroupedObservable<String, Fix>, Observable<List<Fix>>> buffer(
            final int bufferSize) {
        return fileFixes -> fileFixes.buffer(bufferSize);
    }

    @SuppressWarnings("unchecked")
    private static Action1<List<Fix>> writeFixList(final Func1<Fix, String> fileMapper,
            final boolean zip, BinaryFixesFormat format) {
        return fixes -> {
            if (fixes.size() == 0)
                return;
            String filename = fileMapper.call(fixes.get(0));
            writeFixes((List<HasFix>) (List<?>) fixes, new File(filename), true, zip, format);
        };
    }

    private static final int NUMBER_FILE_LOCKS = 200;

    /**
     * A guava cache of locks so that file writes can happen simultaneously
     * (SSD) but not go too crazy (one lock per file). This maps files to a
     * limited set of locks using the file hash keys.
     */
    private static final Striped<Lock> fileLocks = Striped.lock(NUMBER_FILE_LOCKS);

    public static void writeFixes(List<HasFix> fixes, File file, boolean append, boolean zip,
            BinaryFixesFormat format) {
        Preconditions.checkArgument(!zip || !append, "cannot perform append and zip at same time");

        // get the lock for the file
        final Lock lock = fileLocks.get(file);

        OutputStream os = null;
        try {
            // open the lock for the file
            lock.lock();

            // open an output stream
            file.getParentFile().mkdirs();
            FileOutputStream fos = new FileOutputStream(file, append);
            OutputStream s;
            if (zip)
                s = new GZIPOutputStream(fos);
            else
                s = fos;
            os = new BufferedOutputStream(s);

            // write the fixes to the output stream
            ByteBuffer bb = BinaryFixes.createFixByteBuffer(format);
            for (HasFix fix : fixes) {
                bb.rewind();
                BinaryFixes.write(fix.fix(), bb, format);
                os.write(bb.array());
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            try {
                if (os != null)
                    try {
                        os.close();
                    } catch (IOException e) {
                        // we care because we are writing
                        throw new RuntimeException(e);
                    }
            } finally {
                // must unlock no matter what happens
                lock.unlock();
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
            DateTime d = new DateTime(fix.time(), DateTimeZone.UTC);
            int month = d.getMonthOfYear();
            int year = d.getYear();
            StringBuilder s = new StringBuilder();
            s.append(base);
            s.append(File.separator);
            s.append(year);
            s.append(File.separator);
            s.append(month);
            s.append(File.separator);
            s.append(fix.mmsi());
            s.append(".track");
            System.out.println(s);
            return s.toString();
        }

    }

    public static class ByYear implements Func1<Fix, String> {

        private final String base;

        public ByYear(File directory) {
            base = directory.getAbsolutePath();
        }

        @Override
        public String call(Fix fix) {
            DateTime d = new DateTime(fix.time(), DateTimeZone.UTC);
            int year = d.getYear();
            StringBuilder s = new StringBuilder();
            s.append(base);
            s.append(File.separator);
            s.append(year);
            s.append(File.separator);
            s.append(fix.mmsi());
            s.append(".track");
            return s.toString();
        }

    }

}
