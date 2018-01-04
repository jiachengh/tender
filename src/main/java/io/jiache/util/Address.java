package io.jiache.util;


public class Address {
    private final String host;
    private final int port;

    private Address(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static Address newAddress(String host, Integer port) {
        return new Address(host, port);
    }

    public static Address parseFromString(String address) {
        String[] components = address.split(":");
        String host = components[0];
        int port = Integer.parseInt(components[1]);
        return new Address(host, port);
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Address address = (Address) o;

        if (port != address.port) return false;
        return host != null ? host.equals(address.host) : address.host == null;
    }

    @Override
    public int hashCode() {
        int result = host != null ? host.hashCode() : 0;
        result = 31 * result + port;
        return result;
    }

    @Override
    public String toString() {
        return "Address{" +
                "host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}
