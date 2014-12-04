package au.gov.amsa.navigation;

import java.util.Date;

import com.github.davidmoten.grumpy.core.Position;

public class CollisionCandidate {
	private final VesselPosition p1;
	private final VesselPosition p2;
	private final long time;

	public CollisionCandidate(VesselPosition p1, VesselPosition p2, long time) {
		this.p1 = p1;
		this.p2 = p2;
		this.time = time;
	}

	public VesselPosition position1() {
		return p1;
	}

	public VesselPosition position2() {
		return p2;
	}

	public long time() {
		return time;
	}

	@Override
	public String toString() {
		return "CollisionCandidate [\np1=" + p1 + ", \np2=" + p2
				+ ", \ncollisionTime=" + new Date(time) + "\nseparationKm="
				+ separationKm() + "]";
	}

	public double separationKm() {
		return new Position(p1.lat(), p1.lon()).getDistanceToKm(new Position(p2
				.lat(), p2.lon()));
	}
}
