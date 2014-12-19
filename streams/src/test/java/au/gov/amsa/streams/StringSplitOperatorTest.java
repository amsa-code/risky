package au.gov.amsa.streams;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.regex.Pattern;

import org.junit.Test;

import rx.Observable;

public class StringSplitOperatorTest {

	@Test
	public void test() {
		List<String> list = Observable.just("boo:an", "d:you")
				.lift(new StringSplitOperator(Pattern.compile(":"))).toList()
				.toBlocking().single();
		assertEquals(asList("boo", "and", "you"), list);
	}

}
