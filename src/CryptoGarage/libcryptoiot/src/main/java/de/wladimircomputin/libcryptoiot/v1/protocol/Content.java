package de.wladimircomputin.libcryptoiot.v1.protocol;

public class Content {
    public MessageType code;
    public String data;

    public Content(){
        this(MessageType.NOPE);
    }

    public Content(MessageType type){
        this(type, "");
    }

    public Content(MessageType type, String data){
        this.data = data;
        this.code = type;
    }
}
