package au.gov.amsa.risky.format;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;

import java.io.File;
import java.nio.ByteBuffer;

import com.google.common.base.Optional;

public class BinaryFixesUtil {

    static FixImpl toFix(int mmsi, ByteBuffer bb) {
        float lat = bb.getFloat();
        float lon = bb.getFloat();
        long time = bb.getLong();
        int latency = bb.getInt();
        final Optional<Integer> latencySeconds;
        if (latency == -1)
            latencySeconds = absent();
        else
            latencySeconds = of(latency);
        short src = bb.getShort();
        final Optional<Short> source;
        if (src == 0)
            source = absent();
        else
            source = of(src);
        byte nav = bb.get();
        final Optional<NavigationalStatus> navigationalStatus;
        if (nav == Byte.MAX_VALUE)
            navigationalStatus = absent();
        else
            navigationalStatus = of(NavigationalStatus.values()[nav]);

        // rate of turn
        bb.get();

        short sog = bb.getShort();
        final Optional<Float> speedOverGroundKnots;
        if (sog == BinaryFixes.SOG_ABSENT)
            speedOverGroundKnots = absent();
        else
            speedOverGroundKnots = of(sog / 10f);

        short cog = bb.getShort();
        final Optional<Float> courseOverGroundDegrees;
        if (cog == BinaryFixes.COG_ABSENT)
            courseOverGroundDegrees = absent();
        else
            courseOverGroundDegrees = of(cog / 10f);

        short heading = bb.getShort();
        final Optional<Float> headingDegrees;
        if (heading == BinaryFixes.HEADING_ABSENT)
            headingDegrees = absent();
        else
            headingDegrees = of(heading / 10f);
        byte cls = bb.get();
        final AisClass aisClass;
        if (cls == 0)
            aisClass = AisClass.A;
        else
            aisClass = AisClass.B;
        FixImpl fix = new FixImpl(mmsi, lat, lon, time, latencySeconds, source, navigationalStatus,
                speedOverGroundKnots, courseOverGroundDegrees, headingDegrees, aisClass);
        return fix;
    }

    static int getMmsi(File file) {
        int finish = file.getName().indexOf('.');
        String id = file.getName().substring(0, finish);
        return Integer.parseInt(id);
    }
}
