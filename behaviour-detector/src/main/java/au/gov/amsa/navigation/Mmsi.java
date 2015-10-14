package au.gov.amsa.navigation;

public class Mmsi implements Identifier {

    private final int mmsi;
    private final int hashCode;

    public Mmsi(int mmsi) {
        this.mmsi = mmsi;
        this.hashCode = calculateHashCode();
    }

    public int value() {
        return mmsi;
    }

    @Override
    public long uniqueId() {
        return mmsi;
    }

    private int calculateHashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (mmsi ^ (mmsi >>> 32));
        return result;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Mmsi other = (Mmsi) obj;
        if (mmsi != other.mmsi)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Mmsi [mmsi=" + mmsi + "]";
    }

}
