package enums.pushservices;

import io.ebean.annotation.EnumValue;

/**
 * The poirot attribute for the push message. Has an effect on device notification sound,
 * vibration, visibility, etc.
 */
public enum MessagePriority {
    @EnumValue("low")
    PRIORITY_LOW,

    @EnumValue("normal")
    PRIORITY_NORMAL,

    @EnumValue("high")
    PRIORITY_HIGH,
}