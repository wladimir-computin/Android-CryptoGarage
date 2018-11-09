package de.wladimircomputin.libcryptogarage.protocol;

public class Content {
    public MessageType code;
    public String data;

    public Content(){
        new Content(MessageType.NOPE);
    }

    public Content(MessageType type){
        new Content(type, "");
    }

    public Content(MessageType type, String data){
        this.data = data;
        this.code = type;
    }
}
