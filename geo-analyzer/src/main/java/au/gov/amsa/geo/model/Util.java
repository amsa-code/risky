package au.gov.amsa.geo.model;

import java.text.SimpleDateFormat;

import au.gov.amsa.util.navigation.Position;

import com.google.common.base.Optional;

public class Util {
	public static Position toPos(HasPosition a) {
		return new Position(a.getPosition().getLat(), a.getPosition().getLon());
	}

	public static double greatCircleDistanceNm(HasPosition p1, HasPosition p2) {
		return toPos(p1).getDistanceToKm(toPos(p2)) / 1.852;
	}

	public static String formatDate(Optional<Long> time) {
		if (time.isPresent()) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			return sdf.format(time.get());
		} else
			return "";
	}
}
