package au.gov.amsa.util.netcdf;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.NetcdfFileWriter.Version;
import ucar.nc2.Variable;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

public class NetCdfWriter {

    private final NetcdfFileWriter f;
    private final int numRecords;
    private final Map<Var<?>, List<?>> map = new HashMap<>();

    public NetCdfWriter(File file, int numRecords) {
        this.numRecords = numRecords;
        try {
            f = NetcdfFileWriter.createNew(Version.netcdf3, file.getPath());
            // add version attribute
            f.addGroupAttribute(null, new Attribute("version", "0.1"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> Var<T> addVariable(String standardName, Optional<String> longName,
            Optional<String> units, Optional<String> encoding, Class<T> cls) {
        Dimension dimension = f.addDimension(null, standardName, numRecords);
        Variable variable = f.addVariable(null, standardName, toDataType(cls),
                Arrays.asList(dimension));
        if (units.isPresent())
            variable.addAttribute(new Attribute("units", units.get()));
        if (encoding.isPresent())
            variable.addAttribute(new Attribute("encoding", encoding.get()));
        return new Var<T>(variable, cls);
    }

    public <T> void add(Var<T> variable, T value) {
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) map.get(variable);
        if (list == null) {
            list = Lists.newArrayList();
            map.put(variable, list);
        }
        list.add(value);
    }

    public void close() {

        try {
            f.create();
            for (Var<?> var : map.keySet()) {
                List<?> list = map.get(var);
                int[] shape = new int[] { list.size() };
                Array data = Array.factory(DataType.getType(var.cls()), shape);
                for (int i = 0; i < list.size(); i++) {
                    data.setObject(i, list.get(i));
                }
                f.write(var.variable(), data);
            }
            f.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InvalidRangeException e) {
            throw new RuntimeException(e);
        }
    }

    private static DataType toDataType(Class<?> cls) {
        return DataType.DOUBLE;
    }

    public static class Var<T> {

        private final Variable variable;
        private final Class<T> cls;

        public Var(Variable variable, Class<T> cls) {
            this.variable = variable;
            this.cls = cls;
        }

        public Variable variable() {
            return variable;
        }

        public Class<T> cls() {
            return cls;
        }

    }

}
