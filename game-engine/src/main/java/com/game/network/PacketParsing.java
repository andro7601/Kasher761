package com.game.network;

public class PacketParsing {
    //movement bits
    public static final byte LEFT_BIT  = (byte) 0b10000000; // bit 7
    public static final byte UP_BIT    = (byte) 0b01000000; // bit 6
    public static final byte RIGHT_BIT = (byte) 0b00100000; // bit 5
    public static final byte DOWN_BIT  = (byte) 0b00010000; // bit 4

    //gunandabilitybits
    public static final byte gunShotBit=(byte) 0b10000000;//byte 7
    public static final byte AbilityBit=(byte) 0b01000000;//byte 6
}
