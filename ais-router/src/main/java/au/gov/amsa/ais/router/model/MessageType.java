package au.gov.amsa.ais.router.model;

public final class MessageType {

    private final int aisMessageType;

    public MessageType(int aisMessageType) {
        this.aisMessageType = aisMessageType;
    }

    public int aisMessageType() {
        return aisMessageType;
    }

}
