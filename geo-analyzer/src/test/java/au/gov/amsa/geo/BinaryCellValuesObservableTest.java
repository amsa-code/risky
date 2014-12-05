package au.gov.amsa.geo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.Test;

import rx.Observable;
import au.gov.amsa.geo.model.CellValue;
import au.gov.amsa.geo.model.Options;
import au.gov.amsa.util.rx.OperatorWriteBytes;

public class BinaryCellValuesObservableTest {

	private static final double PRECISION = 0.00000001;

	@Test
	public void testSavingCellValuesToBinaryFileAndReadingBackAgain() {
		Options options = Options.builder().cellSizeDegrees(1.2).build();
		CellValue cv = new CellValue(-20, 123, 0.01);
		String filename = Observable.just(cv)
				.lift(new OperatorCellValuesToBytes(options))
				.lift(new OperatorWriteBytes()).toBlocking().first();
		assertNotNull(filename);
		File file = new File(filename);
		assertTrue(file.exists());
		System.out.println(filename);

		Observable<?> o = BinaryCellValuesObservable.readValues(file);
		List<?> list = o.toList().toBlocking().single();
		assertEquals(1.2, (Double) list.get(0), 0.00001);
		assertTrue(list.get(1) instanceof CellValue);
		CellValue v = (CellValue) list.get(1);
		assertEquals(-20, v.getCentreLat(), PRECISION);
		assertEquals(123, v.getCentreLon(), PRECISION);
		assertEquals(0.01, v.getValue(), PRECISION);
		assertEquals(2, list.size());

	}
}
