package au.gov.amsa.util.netcdf;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.NetcdfFileWriter.Version;
import ucar.nc2.Variable;

public class NetCdfWriter implements AutoCloseable {

    private final NetcdfFileWriter f;
    private final Map<Var<?>, List<?>> map = new HashMap<>();

    public NetCdfWriter(File file, String version) {
        try {
            f = NetcdfFileWriter.createNew(Version.netcdf3, file.getPath());
            // add version attribute
            f.addGroupAttribute(null, new Attribute("version", version));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public NetcdfFileWriter writer() {
        return f;
    }

    public NetCdfWriter addAttribute(String name, String value) {
        f.addGroupAttribute(null, new Attribute(name, value));
        return this;
    }

    public <T> VarBuilder<T> addVariable(String shortName, Class<T> cls) {
        return new VarBuilder<T>(this, shortName, cls);
    }

    public <T> Var<T> addVariable(String shortName, Optional<String> longName,
            Optional<String> units, Optional<String> encoding, Class<T> cls, int numRecords) {
        Preconditions.checkNotNull(shortName);
        Preconditions.checkNotNull(longName);
        Preconditions.checkNotNull(units);
        Preconditions.checkNotNull(encoding);
        Preconditions.checkNotNull(cls);
        Dimension dimension = f.addDimension(null, shortName, numRecords);
        Variable variable = f.addVariable(null, shortName, toDataType(cls),
                Arrays.asList(dimension));
        if (longName.isPresent())
            variable.addAttribute(new Attribute("long_name", longName.get()));
        if (units.isPresent())
            variable.addAttribute(new Attribute("units", units.get()));
        if (encoding.isPresent())
            variable.addAttribute(new Attribute("encoding", encoding.get()));
        return new Var<T>(this, variable, cls);
    }

    public <T> NetCdfWriter add(Var<T> variable, T value) {
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) map.get(variable);
        if (list == null) {
            list = Lists.newArrayList();
            map.put(variable, list);
        }
        list.add(value);
        return this;
    }

    @Override
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

    public static class VarBuilder<T> {
        private final NetCdfWriter writer;
        final String shortName;
        final Class<T> cls;
        Optional<String> longName = Optional.absent();
        Optional<String> units = Optional.absent();
        Optional<String> encoding = Optional.absent();
        Optional<Integer> numRecords = Optional.absent();

        VarBuilder(NetCdfWriter writer, String shortName, Class<T> cls) {
            this.writer = writer;
            this.shortName = shortName;
            this.cls = cls;
        }

        public VarBuilder<T> longName(String s) {
            longName = Optional.of(s);
            return this;
        }

        public VarBuilder<T> units(String s) {
            longName = Optional.of(s);
            return this;
        }

        public VarBuilder<T> encoding(String s) {
            longName = Optional.of(s);
            return this;
        }

        public VarBuilder<T> numRecords(int n) {
            this.numRecords = Optional.of(n);
            return this;
        }

        public Var<T> build() {
            return writer.addVariable(shortName, longName, units, encoding, cls, numRecords.get());
        }

    }

    public static class Var<T> {

        private final Variable variable;
        private final Class<T> cls;
        private final NetCdfWriter writer;

        public Var(NetCdfWriter writer, Variable variable, Class<T> cls) {
            this.writer = writer;
            this.variable = variable;
            this.cls = cls;
        }

        public Variable variable() {
            return variable;
        }

        public Class<T> cls() {
            return cls;
        }

        public NetCdfWriter writer() {
            return writer;
        }

        public Var<T> add(T t) {
            writer.add(this, t);
            return this;
        }

    }

}
