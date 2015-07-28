package au.gov.amsa.util.netcdf;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.NetcdfFileWriter.Version;
import ucar.nc2.Variable;

import com.google.common.base.Optional;

public class NetCdfWriter {

    private final NetcdfFileWriter f;
    private final Map<String, Var<?, ?>> variables = new HashMap<>();
    private final int numRecords;

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

    public <T, R> Var<T, R> addVariable(String standardName, Optional<String> longName,
            Optional<String> units, Optional<String> encoding, Class<R> cls,
            Function<? super T, R> function) {
        Dimension dimension = f.addDimension(null, standardName, numRecords);
        Variable variable = f.addVariable(null, standardName, toDataType(cls),
                Arrays.asList(dimension));
        if (units.isPresent())
            variable.addAttribute(new Attribute("units", units.get()));
        if (units.isPresent())
            variable.addAttribute(new Attribute("encoding", encoding.get()));
        return new Var<T, R>(variable, function);
    }

    public <T, R> void write(Var<T, R> variable, T record) {

    }

    private static DataType toDataType(Class<?> cls) {
        return DataType.DOUBLE;
    }

    public static class Var<T, R> {

        private final Variable variable;
        private final Function<? super T, R> function;

        public Var(Variable variable, Function<? super T, R> function) {
            this.variable = variable;
            this.function = function;
        }

        public Variable variable() {
            return variable;
        }

        public Function<? super T, R> function() {
            return function;
        }

    }

}
