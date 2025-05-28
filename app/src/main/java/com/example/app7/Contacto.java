package com.example.app7;

public class Contacto {
    private final String nombre;
    private final String ip;

    public Contacto(String nombre, String ip) {
        this.nombre = nombre;
        this.ip = ip;
    }

    public String getNombre() {
        return nombre;
    }

    public String getIp() {
        return ip;
    }

    @Override
    public String toString() {
        return nombre;
    }
}