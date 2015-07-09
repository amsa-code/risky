package au.gov.amsa.mariweb;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Func1;
import au.gov.amsa.streams.Strings;
import au.gov.amsa.util.nmea.NmeaMessageParser;
import au.gov.amsa.util.nmea.NmeaUtil;

import com.google.common.base.Preconditions;

public class BackupReader {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final Set<String> TABLES = new HashSet<String>(Arrays.asList("ITU21_data",
            "ITU411_data", "ITU123_data", "ITU5_data", "ITU18_data", "ITU19_data"));

    static Observable<String> getNmea(InputStream is) {
        final AtomicInteger lineNo = new AtomicInteger(0);
        return Strings.split(Strings.from(new InputStreamReader(is)), "\n")
        //
                .doOnNext(line -> {
                    lineNo.incrementAndGet();
                }).filter(line -> {
                    for (String table : TABLES)
                        if (line.startsWith("INSERT INTO `" + table + "`"))
                            return true;
                    return false;
                })
                // parse the insert statements
                .lift(new OperatorExtractValuesFromInsertStatement())
                // buffer on backpressure because above operator is not
                // backpressure aware and was experiencing a hang here when
                // expected a MissingBackpressureException.
                .onBackpressureBuffer()
                // use the bits from the row
                .flatMap(toNmea());

    }

    private static Func1<List<String>, Observable<String>> toNmea() {
        return row -> {
            String aisMessage = row.get(5);
            String[] items = aisMessage.split("\\|");
            List<String> list = new ArrayList<String>();
            String positionTime = row.get(1);
            String arrivalTime = row.get(2);
            String tagBlock = row.get(6);
            final String tagBlockAmended;
            if (tagBlock.length() == 0)
                throw new RuntimeException("tag block is empty!");
            else {
                // now insert arrival time into the tag block with a tag of `at`
                if (tagBlock.charAt(0) != '\\')
                    throw new RuntimeException("tag block should start with \\:" + tagBlock);
                if (tagBlock.length() < 5)
                    throw new RuntimeException("tag block should be at least 5 characters:"
                            + tagBlock);
                LinkedHashMap<String, String> tags = NmeaMessageParser.extractTags(tagBlock
                        .substring(1, tagBlock.length() - 1));
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                sdf.setTimeZone(UTC);
                try {
                    long unixSeconds = sdf.parse(arrivalTime).getTime() / 1000;
                    StringBuilder s = new StringBuilder();
                    String source = tags.get("s");
                    if (source != null) {
                        source = source.trim();
                        s.append("s:");
                        s.append(source);
                    }
                    long positionTimeUnixSeconds = sdf.parse(positionTime).getTime() / 1000;
                    if (s.length() > 0)
                        s.append(',');
                    s.append("c:");
                    s.append(positionTimeUnixSeconds);
                    if (s.length() > 0)
                        s.append(',');
                    s.append("at:");// custom tag for arrival time
                    s.append(unixSeconds);
                    String checksum = NmeaUtil.getChecksum(s.toString());
                    s.append('*');
                    s.append(checksum);
                    s.append('\\');
                    s.insert(0, '\\');
                    tagBlockAmended = s.toString();
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }
            for (String item : items) {
                // list.add(toTagBlock(positionTime) + item);
                list.add(tagBlockAmended + item);
            }
            return Observable.from(list);
        };
    }

    public Observable<String> getNmea(String s) {
        return getNmea(new ByteArrayInputStream(s.getBytes()));
    }

    public void extractNmea(File file, File output) {
        GZIPInputStream is = null;
        GZIPOutputStream fos = null;
        try {
            is = new GZIPInputStream(new FileInputStream(file));
            fos = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(output)));
            extractNmea(is, fos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (is != null)
                try {
                    is.close();
                } catch (IOException e) {
                    // do nothing
                }
            if (fos != null)
                try {
                    fos.close();
                } catch (IOException e) {
                    // do nothing
                }
        }
    }

    public void extractNmea(InputStream is, OutputStream os) {
        final PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os,
                Charset.forName("UTF-8"))));
        try {
            final AtomicLong count = new AtomicLong(0);
            final AtomicReference<Throwable> exception = new AtomicReference<Throwable>();
            getNmea(is)
            // subscribe
                    .subscribe(new Observer<String>() {

                        Long time = System.currentTimeMillis();
                        final int rateEvery = 100000;

                        @Override
                        public void onCompleted() {
                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                            exception.set(e);
                        }

                        @Override
                        public void onNext(String line) {
                            writer.println(line);
                            incrementCount(count);
                        }

                        private void incrementCount(final AtomicLong count) {
                            long n = count.incrementAndGet();
                            if (n % rateEvery == 0) {
                                long t = System.currentTimeMillis();
                                double rate = rateEvery * 1000.0 / (t - time);
                                System.out.println(new Date() + ":" + n + " msgsPerSecond = "
                                        + rate);
                                time = t;
                            }
                        }
                    });
            if (exception.get() != null)
                throw new RuntimeException(exception.get());
        } finally {
            writer.close();
        }
    }

    public static void convertDirectoryToNmea(File directory, final Scheduler scheduler,
            boolean recurse) {

        int count = Observable.from(getFilesToProcess(directory, recurse))
                .flatMap(new Func1<File, Observable<File>>() {

                    @Override
                    public Observable<File> call(final File file) {
                        return Observable.create(new OnSubscribe<File>() {

                            @Override
                            public void call(Subscriber<? super File> subscriber) {
                                convertFileToNmea(file);
                                subscriber.onNext(file);
                                subscriber.onCompleted();
                            }
                        }).subscribeOn(scheduler);
                    }
                }).count().toBlocking().single();
        System.out.println(count + "files converted");
    }

    private static List<File> getFilesToProcess(File directory, boolean recurse) {
        Preconditions.checkArgument(directory.exists(), "directory does not exist: " + directory);
        Preconditions.checkArgument(directory.isDirectory(), "file is not a directory: "
                + directory);
        File[] files = directory.listFiles((dir, name) -> {
            return (name.startsWith("LSS_20") || name.startsWith("ITU_20"))
                    && name.endsWith(".bu.gz");
        });
        // sort by ascending filename
        Arrays.sort(files, (a, b) -> {
            return a.getName().compareTo(b.getName());
        });
        List<File> list = new ArrayList<File>(Arrays.asList(files));
        if (recurse) {
            for (File d : directory.listFiles())
                if (d.isDirectory() && !d.getName().startsWith("."))
                    list.addAll(getFilesToProcess(d, recurse));
        }
        return list;
    }

    static void convertFileToNmea(File file) {
        String baseName = "NMEA_" + file.getName().replace(".bu", "");
        File output = new File(file.getParentFile(), baseName + ".tmp");
        File finalOutput = new File(file.getParentFile(), baseName);
        if (!finalOutput.exists()) {
            System.out.println("converting " + file);
            try {
                BackupReader b = new BackupReader();
                b.extractNmea(file, output);
                output.renameTo(finalOutput);
                if (finalOutput.length() < 1000000)
                    throw new RuntimeException("file less than 1MB, deleting " + finalOutput);
                System.out.println("converted " + file);
            } catch (RuntimeException e) {
                System.out.println("problem processing " + file);
                e.printStackTrace(System.out);
                finalOutput.delete();
            }
        } else
            System.out.println("output exists: " + finalOutput);
    }
}
