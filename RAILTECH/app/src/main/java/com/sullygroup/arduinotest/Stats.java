package com.sullygroup.arduinotest;

/**
 * Classe contenant les information pour une stat(température ou humidité).
 * Created by jocelyn.caraman on 21/03/2017.
 */

class Stats {
    private String name;
    private int value;
    private int icon;

    Stats(String name, int value, int icon) {
        this.name = name;
        this.value = value;
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    int getValue() {
        return value;
    }

    void setValue(int value) {
        this.value = value;
    }

    int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }
}
