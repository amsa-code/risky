package au.gov.amsa.ais.rx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CraftPropertiesTest {

	private static final Mmsi MMSI = new Mmsi(1234);

	@Test
	public void testIsEmpty() {
		CraftProperties c = create();
		assertTrue(c.getMap().entrySet().isEmpty());
	}

	@Test
	public void testImmutable() {
		CraftProperties c = create();
		c.add(p("a", 1));
		assertTrue(c.getMap().entrySet().isEmpty());
	}

	@Test
	public void testAddOne() {
		CraftProperties c = create();
		CraftProperties c2 = c.add(p("a", 1));
		assertEquals("a", c2.getMap().get(CraftPropertyName.NAME).get(1L));
	}

	@Test
	public void testAddOneBeforeWithSameValue() {
		CraftProperties c = create();
		CraftProperties c2 = c.add(p("a", 1)).add(p("a", 0));
		assertEquals(null, c2.getMap().get(CraftPropertyName.NAME).get(1L));
		assertEquals("a", c2.getMap().get(CraftPropertyName.NAME).get(0L));
	}

	@Test
	public void testAddOneBeforeWithDifferentValue() {
		CraftProperties c = create();
		CraftProperties c2 = c.add(p("a", 1)).add(p("b", 0));
		assertEquals("a", c2.getMap().get(CraftPropertyName.NAME).get(1L));
		assertEquals("b", c2.getMap().get(CraftPropertyName.NAME).get(0L));
	}

	@Test
	public void testAddOneAfterWithSameValue() {
		CraftProperties c = create();
		CraftProperties c2 = c.add(p("a", 1)).add(p("a", 2));
		assertEquals("a", c2.getMap().get(CraftPropertyName.NAME).get(1L));
		assertEquals(null, c2.getMap().get(CraftPropertyName.NAME).get(2L));
	}

	@Test
	public void testAddOneAfterWithDifferentValue() {
		CraftProperties c = create();
		CraftProperties c2 = c.add(p("a", 1)).add(p("b", 2));
		assertEquals("a", c2.getMap().get(CraftPropertyName.NAME).get(1L));
		assertEquals("b", c2.getMap().get(CraftPropertyName.NAME).get(2L));
	}

	@Test
	public void testInsertOneAfterWithSameValueAsBefore() {
		CraftProperties c = create();
		CraftProperties c2 = c.add(p("a", 1)).add(p("c", 3)).add(p("a", 2));
		assertEquals("a", c2.getMap().get(CraftPropertyName.NAME).get(1L));
		assertEquals(null, c2.getMap().get(CraftPropertyName.NAME).get(2L));
		assertEquals("c", c2.getMap().get(CraftPropertyName.NAME).get(3L));
	}

	@Test
	public void testInsertOneAfterWithDifferentValueAsBefore() {
		CraftProperties c = create();
		CraftProperties c2 = c.add(p("a", 1)).add(p("c", 3)).add(p("b", 2));
		assertEquals("a", c2.getMap().get(CraftPropertyName.NAME).get(1L));
		assertEquals("b", c2.getMap().get(CraftPropertyName.NAME).get(2L));
		assertEquals("c", c2.getMap().get(CraftPropertyName.NAME).get(3L));
	}

	@Test
	public void testInsertOneAfterWithSameValueAsAfter() {
		CraftProperties c = create();
		CraftProperties c2 = c.add(p("a", 1)).add(p("c", 3)).add(p("c", 2));
		assertEquals("a", c2.getMap().get(CraftPropertyName.NAME).get(1L));
		assertEquals("c", c2.getMap().get(CraftPropertyName.NAME).get(2L));
		assertEquals(null, c2.getMap().get(CraftPropertyName.NAME).get(3L));
	}

	@Test
	public void testAddOneWithSameTimeShouldRetainOnlyLatestValue() {
		CraftProperties c = create();
		CraftProperties c2 = c.add(p("a", 1)).add(p("b", 1));
		assertEquals("b", c2.getMap().get(CraftPropertyName.NAME).get(1L));
	}

	private static CraftProperties create() {
		return new CraftProperties(MMSI);
	}

	private static CraftProperty p(String name, long time) {
		return new CraftProperty(MMSI, CraftPropertyName.NAME, name, time);
	}

}
