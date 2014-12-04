package au.gov.amsa.util.nmea.saver;

public class SystemClock implements Clock{

	@Override
	public long getTimeMs() {
		return System.currentTimeMillis();
	}

}
