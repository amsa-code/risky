package au.gov.amsa.ais.message;

import au.gov.amsa.ais.AisMessage;

public class AisMessageOther implements AisMessage {

	private final int messageId;
	private final String source;

	public AisMessageOther(int messageId) {
		this(messageId, null);
	}

	public AisMessageOther(int messageId, String source) {
		this.messageId = messageId;
		this.source = source;
	}

	@Override
	public int getMessageId() {
		return messageId;
	}


	

	@Override
	public String getSource() {
		return source;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AisMessageOther [messageId=");
		builder.append(messageId);
		builder.append(", source=");
		builder.append(source);
		builder.append("]");
		return builder.toString();
	}

}
