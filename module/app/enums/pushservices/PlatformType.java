package enums.pushservices;

import io.ebean.annotation.EnumValue;

/**
 * The type of push messaging service (platform) that is being used.
 *
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 4/30/16 Splendid Bits.
 */
public enum PlatformType {
    @EnumValue("GCM")
    SERVICE_GCM("GCM", "https://fcm.googleapis.com/fcm/send"),

    @EnumValue( "APNS")
    SERVICE_APNS("APNS", "https://gateway.sandbox.push.apple.com");

    public String name;
    public String url;
    PlatformType(String name, String url) {
        this.name = name;
        this.url = url;
    }
}
