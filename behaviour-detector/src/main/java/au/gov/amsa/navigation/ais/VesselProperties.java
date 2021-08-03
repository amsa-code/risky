package au.gov.amsa.navigation.ais;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import au.gov.amsa.navigation.Identifier;

public class VesselProperties {

	public static enum Key {
		DESTINATION;
	}

	private Map<Identifier, Map<Key, ValueAndTime>> map = new ConcurrentHashMap<Identifier, Map<Key, ValueAndTime>>();

	public synchronized void add(Identifier id, Key key, Object value, long time) {
		Optional<Map<Key, ValueAndTime>> props = Optional.ofNullable(map.get(id));
		HashMap<Key, ValueAndTime> p;
		if (!props.isPresent())
			p = new HashMap<Key,ValueAndTime>();
		else 
			p = new HashMap<Key,ValueAndTime>(props.get());
		Optional<ValueAndTime> v = Optional.ofNullable(p.get(key));
		if (!v.isPresent() || v.get().getTime() <time) {
			p.put(key,new ValueAndTime(value, time));
			map.put(id, p);
		}
	}
	

}
