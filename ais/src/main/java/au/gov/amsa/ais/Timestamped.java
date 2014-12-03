package au.gov.amsa.ais;

public class Timestamped<T extends AisMessage> {
	
	private final T message;
	private final long time;

	public Timestamped(T message, long time) {
		this.message = message;
		this.time = time;
	}
	
	public static <T extends AisMessage> Timestamped<T> create(T message , long time) {
		return new Timestamped<T>(message, time);
	}

	public T message() {
		return message;
	}

	public long time() {
		return time;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Timestamped [time=");
		builder.append(time);
		builder.append(", message=");
		builder.append(message);
		builder.append("]");
		return builder.toString();
	}
	
}
