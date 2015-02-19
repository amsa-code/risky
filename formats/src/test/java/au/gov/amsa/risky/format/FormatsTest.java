package au.gov.amsa.risky.format;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

public class FormatsTest {

	@Test
	public void testRebase() {
		File f = new File("target/rebase/a/b/c/d");
		File input = new File("target/rebase");
		File output = new File("target/rebase2");
		assertEquals(new File("target/rebase2/a/b/c/d").getAbsolutePath(),
				Formats.rebase(f, input, output).getAbsolutePath());
	}

}
