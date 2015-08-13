package au.gov.amsa.ihs.reader.adhoc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import org.joda.time.DateTime;

import com.google.common.base.Optional;

import au.gov.amsa.ihs.model.Ship;
import au.gov.amsa.ihs.reader.IhsReader;
import rx.functions.Action1;

public class ExtractCsvMain {

    public static void main(String[] args) throws FileNotFoundException {
        File file = new File("/media/analysis/ship-data/ihs/608750.zip");
        final PrintStream out = new PrintStream("target/ships.csv");
        out.println(
                "IMO, MMSI, DWT, GT, Type2, Type3, Type4, Type5, StatCode5, LOAMetres, BreadthMetres, BuildYear, BuildMonth, DisplacementTonnage, DraughtMetres, SpeedKnots, LastUpdateTime");
        IhsReader.fromZip(file).map(IhsReader::toShip).forEach(new Action1<Ship>() {

            @Override
            public void call(Ship s) {
                out.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n", s.getImo(),
                        st(s.getMmsi()), str(s.getDeadweightTonnage()), str(s.getGrossTonnage()),
                        st(s.getType2()), st(s.getType3()), st(s.getType4()), st(s.getType5()),
                        st(s.getStatCode5()), str(s.getLengthOverallMetres()),
                        str(s.getBreadthMetres()), str(s.getYearOfBuild()),
                        str(s.getMonthOfBuild()), str(s.getDisplacementTonnage()),
                        str(s.getDraughtMetres()), str(s.getSpeedKnots()),
                        dst(s.getLastUpdateTime()));
            }
        });
        out.close();
    }

    private static String dst(Optional<DateTime> d) {

        if (d.isPresent())
            return d.get().toString();
        else
            return "";
    }

    private static String str(Optional<? extends Number> value) {
        if (value.isPresent())
            return value.get().toString();
        else
            return "";
    }

    private static String st(Optional<String> value) {
        return value.or("");
    }
}
