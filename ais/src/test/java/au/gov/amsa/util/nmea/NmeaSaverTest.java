package au.gov.amsa.util.nmea;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.junit.Test;
import org.openjdk.jmh.util.FileUtils;

import rx.Observable;
import rx.schedulers.Schedulers;

public class NmeaSaverTest {

	@Test
	public void testSaving() throws InterruptedException, IOException {
		String line = "\\s:rEV02,d:1334337321*5A\\!AIVDM,1,1,,B,33:JeT0OjtVls<;fDlbl5CFH2000,0*71";
		String amendedLine = "\\s:rEV02,d:1334337321,c:1234567*1F\\!AIVDM,1,1,,B,33:JeT0OjtVls<;fDlbl5CFH2000,0*71";
		Clock clock = new Clock() {
			@Override
			public long getTimeMs() {
				return 1234567890;
			}
		};

		File file = new File("target/1970-01-15.txt");
		file.delete();
		new NmeaSaver(Observable.just(line), new FileFactoryPerDay(new File(
				"target")), clock).start(Schedulers.immediate());
		Collection<String> lines = FileUtils.readAllLines(file);
		assertEquals(1, lines.size());
		assertEquals(amendedLine, lines.iterator().next());
	}
}
