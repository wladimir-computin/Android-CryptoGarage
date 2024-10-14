package de.wladimircomputin.libcryptoiot.v1.protocol;

public enum MessageType {
    HELLO("HELLO"),
    ACK("OK"),
    ERR("FAIL"),
    DATA("DATA"),
    NOPE("")
    ;

    private final String text;

    MessageType(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }

    public static MessageType fromString(String type){
        for (MessageType t : MessageType.values()){
            if (t.toString().equalsIgnoreCase(type)){
                return t;
            }
        }
        return NOPE;
    }
}
