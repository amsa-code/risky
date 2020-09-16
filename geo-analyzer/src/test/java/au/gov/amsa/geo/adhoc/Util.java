package au.gov.amsa.geo.adhoc;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class Util {
    
    public static long getStartTime(File file) {
        String date = file.getName().substring(0, 10);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        long startTime;
        try {
            startTime = sdf.parse(date).getTime();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return startTime;
    }

}
