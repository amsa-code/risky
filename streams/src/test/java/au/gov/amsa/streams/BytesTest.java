package au.gov.amsa.streams;

import java.io.File;
import java.nio.charset.Charset;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import rx.Observable;
import rx.functions.Func1;

import com.github.davidmoten.rx.testing.TestingHelper;

public class BytesTest extends TestCase {

	public static TestSuite suite() {
		return TestingHelper
				.function(STRINGS)
				// test empty
				.name("testStringsFromNoFile").fromEmpty().expectEmpty()
				.name("testStringsFromFile")
				.from("src/test/resources/test1.txt")
				.expect("hello there how\n" + "are you?")
				// get suite
				.testSuite(BytesTest.class);
	}

	public void testDummy() {
		// keep eclipse happy
	}

	private static final Func1<Observable<String>, Observable<String>> STRINGS = new Func1<Observable<String>, Observable<String>>() {

		@Override
		public Observable<String> call(Observable<String> o) {
			return o.flatMap(new Func1<String, Observable<String>>() {

				@Override
				public Observable<String> call(String filename) {
					return Bytes.from(new File(filename)).map(
							new Func1<byte[], String>() {

								@Override
								public String call(byte[] bytes) {
									return new String(bytes, Charset
											.forName("UTF-8"));
								}
							});
				}
			});
		}
	};
}
