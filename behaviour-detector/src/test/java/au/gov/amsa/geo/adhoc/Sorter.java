package au.gov.amsa.geo.adhoc;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Function;

public class Sorter {

    public <T> void sort(Enumeration<byte[]> records, Function<byte[], T> decode, Comparator<T> comparator, File output)
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
        } catch (IOException e) {
            out.close();
        }

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
                return "hello".getBytes(StandardCharsets.UTF_8);
            }
        };
        new Sorter().sort(e, b -> new String(b), Comparator.<String>naturalOrder(), new File("target/sorted"));
    }

}
