package au.gov.amsa.risky.format;

import java.io.IOException;
import java.io.Writer;

public class LibSvm {

    public static void write(Writer writer, int classification, double... values) {
        try {
            writer.write(Integer.toString(classification));

            for (int i = 0; i < values.length; i++) {
                double value = values[i];
                if (value != 0) {
                    writer.write(' ');
                    writer.write(Integer.toString(i + 1));
                    writer.write(':');
                    writer.write(Double.toString(value));
                }
            }
            writer.write('\n');
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
