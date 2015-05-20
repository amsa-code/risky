package au.gov.amsa.gt;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtil {

    public static void unzip(InputStream is, File folder) {
        byte[] buffer = new byte[1024];
        try {
            // create output directory is not exists
            if (!folder.exists()) {
                folder.mkdir();
            }

            // get the zip file content
            try (ZipInputStream zis = new ZipInputStream(is)) {
                // get the zipped file list entry
                ZipEntry entry = zis.getNextEntry();

                while (entry != null) {

                    String fileName = entry.getName();
                    File newFile = new File(folder, fileName);
                    // create all non existent folders
                    // else you will hit FileNotFoundException for compressed
                    // folder
                    new File(newFile.getParent()).mkdirs();

                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                    entry = zis.getNextEntry();
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
