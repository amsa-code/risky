package au.gov.amsa.risky.format;

public enum NavigationalStatus {
	// order of these should reflect numerical order in nav status int returned
	// from ITU standard ais position report A
	UNDER_WAY_USING_ENGINE, AT_ANCHOR, NOT_UNDER_COMMAND, RESTRICTED_MANOEUVRABILITY, CONSTRAINED_BY_HER_DRAUGHT, MOORED, AGROUND, ENGAGED_IN_FISHING, UNDER_WAY, RESERVED_1, RESERVED_2, FUTURE_1, FUTURE_2, FUTURE_3, AIS_SART;
}
