package dugout.core;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Attr {
	private final ConcurrentMap<String, Serializable> attr = new ConcurrentHashMap<>();

	public Attr put(String key, Serializable value) {
		attr.put(key, value);
		return this;
	}

	public <T extends Serializable> T get(String key, Class<T> clazz) {
		Serializable val = attr.get(key);
		if (val == null) {
			return null;
		}
		return clazz.cast(val);
	}

	public String str(String key) {
		return this.get(key, String.class);
	}

	public <T extends Serializable> boolean exists(String key, Class<T> clazz) {
		Serializable val = attr.get(key);
		return val != null && clazz.isAssignableFrom(val.getClass());
	}

	public Attr remove(String key) {
		attr.remove(key);
		return this;
	}

	public Attr clear() {
		attr.clear();
		return this;
	}
}
