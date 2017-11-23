package au.gov.amsa.geo.adhoc;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Function;

public class Sorter {

    public <T> File sort(Enumeration<byte[]> records, Function<InputStream, Enumeration<byte[]>> read,
            Function<byte[], T> decode, Comparator<T> comparator, File output) throws IOException {
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
            return merge(list, read, decode, comparator, output);
        } catch (IOException e) {
            out.close();
            throw e;
        }

    }

    private <T> File merge(List<byte[]> list, Function<InputStream, Enumeration<byte[]>> read,
            Function<byte[], T> decode, Comparator<T> comparator, File output) {
        return null;
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
        new Sorter().sort(e, read, b -> new String(b), Comparator.<String>naturalOrder(), new File("target/sorted"));
    }

    private static final class InputStreamFixedLengthRecordEnumeration implements Enumeration<byte[]> {
        boolean hasMoreElements = true;
        final int recordLength;
        final DataInputStream dis;
        byte[] bytes = null;

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
            return result;
        }
    }

}
