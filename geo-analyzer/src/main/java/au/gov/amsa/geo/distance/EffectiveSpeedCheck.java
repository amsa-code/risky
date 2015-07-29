package au.gov.amsa.geo.distance;

import au.gov.amsa.risky.format.Fix;

public class EffectiveSpeedCheck {
    private final Fix fix;
    private final boolean ok;

    public EffectiveSpeedCheck(Fix fix, boolean ok) {
        this.fix = fix;
        this.ok = ok;
    }

    public Fix fix() {
        return fix;
    }

    public boolean isOk() {
        return ok;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("EffectiveSpeedCheck [fix=");
        builder.append(fix);
        builder.append(", ok=");
        builder.append(ok);
        builder.append("]");
        return builder.toString();
    }

}
