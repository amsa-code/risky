package au.gov.amsa.util.netcdf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Test;

import com.google.common.base.Optional;

import au.gov.amsa.util.netcdf.NetCdfWriter.Var;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;

public class NetCdfWriterTest {

    @Test
    public void test() throws IOException, InvalidRangeException {
        File file = new File("target/temp.nc");
        file.delete();

        NetCdfWriter n = new NetCdfWriter(file, 2);
        Var<Long> v = n.addVariable("time", Optional.of("time in epoch milliseconds"),
                Optional.of("epoch milliseconds"), Optional.<String> absent(), Long.class);
        v.add(100L);
        v.add(200L);
        n.close();

        // now read the file just written and assert
        NetcdfFile nc = NetcdfFile.open(file.getCanonicalPath());
        List<Attribute> attributes = nc.findGroup(null).getAttributes();
        System.out.println(attributes);
        assertFalse(attributes.isEmpty());
        System.out.println(nc.getDimensions());

        {
            Array array = nc.readSection("time");
            assertEquals(100, array.getLong(0));
            assertEquals(200, array.getLong(1));
            assertEquals(2, array.getSize());
        }
    }
}
