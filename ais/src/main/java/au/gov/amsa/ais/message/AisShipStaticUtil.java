package au.gov.amsa.ais.message;

import java.util.Optional;

//TODO unit tests
public final class AisShipStaticUtil {

    public static Optional<Integer> lengthMetres(Optional<Integer> a, Optional<Integer> b,
            Optional<Integer> c, Optional<Integer> d) {
        if (a.isPresent() && b.isPresent())
            return Optional.of(a.get() + b.get());
        else {
            if (!a.isPresent() && !c.isPresent() && b.isPresent() && d.isPresent())
                return b;
            else
                return Optional.empty();
        }
    }

    public static Optional<Integer> lengthMetres(AisShipStatic m) {
        return lengthMetres(m.getDimensionA(), m.getDimensionB(), m.getDimensionC(),
                m.getDimensionD());
    }

    public static Optional<Integer> widthMetres(Optional<Integer> a, Optional<Integer> b,
            Optional<Integer> c, Optional<Integer> d) {
        if (c.isPresent() && d.isPresent())
            return Optional.of(c.get() + d.get());
        else {
            if (!a.isPresent() && !c.isPresent() && b.isPresent() && d.isPresent())
                return d;
            else
                return Optional.empty();
        }
    }

    public static Optional<Integer> widthMetres(AisShipStatic m) {
        return widthMetres(m.getDimensionA(), m.getDimensionB(), m.getDimensionC(),
                m.getDimensionD());
    }
}
