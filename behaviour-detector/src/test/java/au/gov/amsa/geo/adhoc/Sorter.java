package au.gov.amsa.geo.adhoc;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Function;

import com.github.davidmoten.rtree.internal.util.PriorityQueue;

import net.jcip.annotations.NotThreadSafe;

public class Sorter {

    public <T> File sort(Enumeration<byte[]> records, Function<InputStream, Enumeration<byte[]>> read,
            Function<byte[], T> decode, Comparator<T> comparator, File output, int maxFilesPerMerge, int recordSize)
            throws IOException {
        int MB = 1024 * 1024;
        int K = 1024;
        int maxFileSize = 320 * K;
        int index = 0;
        List<File> tempFiles = new ArrayList<>();
        tempFiles.add(Files.createTempFile("sorter", ".bin").toFile());
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(last(tempFiles)));
        List<byte[]> list = new ArrayList<>();
        try {
            while (records.hasMoreElements()) {
                byte[] b = records.nextElement();
                if (index + b.length > maxFileSize || !records.hasMoreElements()) {
                    BufferedOutputStream mOut = out;
                    System.out.println("sorting and outputing " + last(tempFiles));
                    list //
                            .stream() //
                            .map(x -> new Pair<T>(x, decode.apply(x))) //
                            .parallel() //
                            .sorted((x, y) -> comparator.compare(x.value, y.value)) //
                            .forEach(x -> write(mOut, x));

                    if (index == 0 && records.hasMoreElements()) {
                        throw new RuntimeException("maxFileSize too small, cannot contain 1 record!");
                    }
                    out.close();
                    System.out.println("file written");
                    tempFiles.add(Files.createTempFile("sorter", ".bin").toFile());
                    out = new BufferedOutputStream(new FileOutputStream(last(tempFiles)));
                    index = 0;
                    list.clear();
                }
                list.add(b);
                index += b.length;
            }
            return merge(tempFiles, read, decode, comparator, output, maxFilesPerMerge, recordSize);
        } catch (IOException e) {
            out.close();
            throw e;
        }

    }

    private <T> File merge(List<File> list, Function<InputStream, Enumeration<byte[]>> read, Function<byte[], T> decode,
            Comparator<T> comparator, File output, int maxFilesPerMerge, int recordSize) throws IOException {

        List<File> toFiles = new ArrayList<>();
        while (list.size() > 1) {
            List<File> group = new ArrayList<>(list.subList(0, Math.min(maxFilesPerMerge, list.size())));
            File toFile = Files.createTempFile("merge", ".bin").toFile();
            toFiles.add(toFile);
            // merge group
            try (OutputStream out = new BufferedOutputStream(new FileOutputStream(toFile))) {
                List<InputStreamFixedLengthRecordEnumeration> enumerations = new ArrayList<>();
                for (File file : group) {
                    InputStreamFixedLengthRecordEnumeration en = new InputStreamFixedLengthRecordEnumeration(
                            new BufferedInputStream(new FileInputStream(file)), recordSize);
                    enumerations.add(en);
                }
                PriorityQueue<Integer> queue = new PriorityQueue<Integer>((x, y) -> {
                    T a = decode.apply(enumerations.get(x).lastElement());
                    T b = decode.apply(enumerations.get(y).lastElement());
                    return comparator.compare(a, b);
                });
                for (int i = 0; i < enumerations.size(); i++) {
                    InputStreamFixedLengthRecordEnumeration en = enumerations.get(i);
                    if (en.hasMoreElements()) {
                        // sets last element
                        en.nextElement();
                        queue.offer(i);
                    }
                }
                while (true) {
                    Integer i = queue.poll();
                    if (i == null) {
                        break;
                    } else {
                        byte[] bytes = enumerations.get(i).lastElement();
                        out.write(bytes);
                    }
                }
                for (InputStreamFixedLengthRecordEnumeration en : enumerations) {
                    try {
                        en.close();
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                }
            }

            list = new ArrayList<>(list.subList(group.size(), list.size()));
            if (list.isEmpty()) {
                list = toFiles;
            }
        }
        return list.get(0);
    }

    private File last(List<File> tempFiles) {
        return tempFiles.get(tempFiles.size() - 1);
    }

    private <T> void write(BufferedOutputStream mOut, Pair<T> x) {
        try {
            mOut.write(x.bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class Pair<T> {
        final byte[] bytes;
        final T value;

        Pair(byte[] bytes, T value) {
            this.bytes = bytes;
            this.value = value;
        }
    }

    public static void main(String[] args) throws IOException {

        final byte[] BYTES = "hello".getBytes(StandardCharsets.UTF_8);

        Enumeration<byte[]> e = new Enumeration<byte[]>() {

            int count = 1000000;
            int i = 1;

            @Override
            public boolean hasMoreElements() {
                return i < count;
            }

            @Override
            public byte[] nextElement() {
                i++;
                return BYTES;
            }
        };
        Function<InputStream, Enumeration<byte[]>> read = is -> new InputStreamFixedLengthRecordEnumeration(is,
                BYTES.length);
        new Sorter().sort(e, read, b -> new String(b), Comparator.<String>naturalOrder(), new File("target/sorted"), 10,
                BYTES.length);
    }

    @NotThreadSafe
    static final class InputStreamFixedLengthRecordEnumeration implements Enumeration<byte[]> {
        boolean hasMoreElements = true;
        final int recordLength;
        final DataInputStream dis;
        byte[] bytes = null;
        private byte[] lastElement;

        InputStreamFixedLengthRecordEnumeration(InputStream is, int recordLength) {
            this.dis = new DataInputStream(is);
            this.recordLength = recordLength;
        }

        @Override
        public boolean hasMoreElements() {
            if (!hasMoreElements)
                return false;
            check();
            return hasMoreElements;
        }

        private void check() {
            if (bytes == null) {
                read();
            }
        }

        private void read() {
            try {
                bytes = new byte[recordLength];
                dis.readFully(bytes);
            } catch (EOFException e) {
                hasMoreElements = false;
                return;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public byte[] lastElement() {
            return lastElement;
        }

        @Override
        public byte[] nextElement() {
            check();
            if (bytes == null) {
                throw new RuntimeException("no more elements, you should check hasMoreElements");
            }
            byte[] result = Arrays.copyOf(bytes, bytes.length);
            if (hasMoreElements) {
                read();
            }
            lastElement = result;
            return result;
        }

        public void close() throws IOException {
            dis.close();
        }
    }

}
