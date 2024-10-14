package de.wladimircomputin.libcryptoiot.v1.protocol;

import java.util.Objects;


public class DiscoveryDevice {
    public String type;
    public String name;
    public String ip;

    public DiscoveryDevice(){
        this("", "", "");
    }

    public DiscoveryDevice(String raw){
        try {
            raw = raw.replace(MessageType.DATA.toString() + ":::", "");
            String[] parts = raw.split(":");
            this.type = parts[0];
            this.name = parts[1];
            this.ip = parts[2];
        } catch (Exception x){
            this.type = "";
            this.name = "";
            this.ip = "";
        }

    }

    public DiscoveryDevice(String type, String name, String ip){
        this.type = type;
        this.name = name;
        this.ip = ip;
    }

    public boolean isEmpty(){
        return this.type.isEmpty() && this.name.isEmpty() && this.ip.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiscoveryDevice that = (DiscoveryDevice) o;
        return Objects.equals(type, that.type) && Objects.equals(name, that.name) && Objects.equals(ip, that.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name, ip);
    }
}
