package au.gov.amsa.geo.adhoc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipShapefilesMain {

    public static void main(String[] args) throws IOException {
        File directory = new File("/home/dave/Downloads/port_area_shapefiles");
        String[] list = directory.list();
        Arrays.sort(list);
        String lastKey = null;
        List<File> files = new ArrayList<>();
        for (int i = 0; i < list.length; i++) {
            String name = list[i];

            String key = name.substring(4, name.indexOf("_port_pl."));
            // System.out.println(key);
            if (lastKey != null && !key.equals(lastKey)) {
                zip(lastKey, files);
                files.clear();
            }
            files.add(new File(directory, name));
            lastKey = key;
        }
        zip(lastKey, files);
    }

    private static void zip(String key, List<File> files) throws IOException {
        System.out.println(key);
        for (File file : files) {
            System.out.println("    " + file);
        }
        File out = new File("target/" + key + "_visit_pl.zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(out))) {
            for (File file : files) {
                byte[] buffer = new byte[8192];
                ZipEntry ze = new ZipEntry(file.getName());
                zos.putNextEntry(ze);
                try (FileInputStream fis = new FileInputStream(file)) {
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                }
                zos.closeEntry();
            }
        }
    }

}
