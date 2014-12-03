package au.gov.amsa.ais.rx;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public class CraftProperties {

	private final Map<CraftPropertyName, TreeMap<Long, String>> map;

	private final Mmsi mmsi;

	public CraftProperties(Mmsi mmsi,
			Map<CraftPropertyName, TreeMap<Long, String>> map) {
		this.mmsi = mmsi;
		this.map = map;
	}

	public CraftProperties(Mmsi mmsi) {
		this(mmsi, new HashMap<CraftPropertyName, TreeMap<Long, String>>());
	}

	public Map<CraftPropertyName, TreeMap<Long, String>> getMap() {
		return map;
	}

	public Mmsi getMmsi() {
		return mmsi;
	}

	public CraftProperties add(CraftProperty p) {
		Map<CraftPropertyName, TreeMap<Long, String>> m = new HashMap<>(map);
		if (m.get(p.getName()) == null)
			m.put(p.getName(), new TreeMap<Long, String>());
		TreeMap<Long, String> tree = m.get(p.getName());
		Entry<Long, String> before = tree.floorEntry(p.getTime());
		Entry<Long, String> after = tree.ceilingEntry(p.getTime());
		if (before != null && before.getValue().equals(p.getValue()))
			return this;
		else {
			// replace before with p if same time
			if (after != null && after.getValue().equals(p.getValue()))
				tree.remove(after.getKey());
			tree.put(p.getTime(), p.getValue());
		}
		return new CraftProperties(mmsi, m);
	}
}
