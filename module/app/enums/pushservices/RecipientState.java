package enums.pushservices;

import io.ebean.annotation.EnumValue;

/**
 * An enum defining a possible state of a Message Recipient.
 *
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 4/7/16 Splendid Bits.
 */
public enum RecipientState {
    @EnumValue("IDLE")
    STATE_IDLE,

    @EnumValue("PROCESSING")
    STATE_PROCESSING,

    @EnumValue("FAILED")
    STATE_FAILED,

    @EnumValue("WAITING_RETRY")
    STATE_WAITING_RETRY,

    @EnumValue("COMPLETE")
    STATE_COMPLETE;

    RecipientState() {
    }
}