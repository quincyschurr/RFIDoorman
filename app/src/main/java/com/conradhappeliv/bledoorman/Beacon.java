package com.conradhappeliv.bledoorman;

public class Beacon {
    public int rssi;
    public String deviceAddress;
    public long detectTime;
    Beacon(int r, String a, long t) {
        rssi = r;
        deviceAddress = a;
        detectTime = t;
    }
}
