package au.gov.amsa.gt;

import java.io.InputStreamReader;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.gov.amsa.streams.Strings;

public class Shapes {

	private static final Logger log = LoggerFactory.getLogger(Shapes.class);

	private final Map<String, Shapefile> shapes;

	public Shapes() {
		this("/shapes.txt");
	}

	public Shapes(String resource) {
		InputStreamReader r = new InputStreamReader(
				Shapes.class.getResourceAsStream(resource));
		shapes = Strings
				.lines(r)
				// ignore comment lines
				.filter(line -> !line.startsWith("#"))
				// ignore blank lines
				.filter(line -> line.trim().length() > 0)
				// split by | character
				.map(line -> line.split("\\|"))
				// log
				.doOnNext(items -> {
					log.info("loading " + items[0]);
				})
				// build map
				.toMap(items -> items[0].trim(),
						items -> shapefileFromZip(items[1].trim(),
								Double.parseDouble(items[2].trim())))
				// go
				.toBlocking().single();
	}

	private static Shapefile shapefileFromZip(String resource,
			double bufferDistance) {
		return Shapefile.fromZip(Shapes.class.getResourceAsStream(resource),
				bufferDistance);
	}

	/**
	 * Returns the names of those shapes that contain the given lat, lon.
	 * 
	 * @param lat
	 * @param lon
	 * @return
	 */
	public Stream<String> containing(double lat, double lon) {
		return shapes.keySet().stream()
		// select if contains lat lon
				.filter(name -> shapes.get(name).contains(lat, lon));
	}

	public void add(String name, Shapefile shapefile) {
		shapes.put(name, shapefile);
	}

}
