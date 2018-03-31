package enums.pushservices;

import io.ebean.annotation.EnumValue;
import models.pushservices.db.PlatformFailure;

/**
 * Message and Recipient Platform failure types.
 * <p>
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 5/5/16 Splendid Bits.
 * <p>
 * JavaDoc taken from:
 * https://developers.google.com/cloud-messaging/http-server-ref
 */
public enum FailureType {

    /**
     * [GCM] [APNS]
     * The sender account used to send a message couldn't be authenticated. Possible causes are:
     * <p>
     * - Authorization header missing or with invalid syntax in HTTP request.
     * - Invalid project number sent as key.
     * - Key valid but with GCM service disabled.
     * - Request originated from a server not whitelisted in the Server Key IPs.
     * - Check that the token you're sending inside the Authentication header is the correct API key
     * associated with your project. See Checking the validity of an API Key for details.
     */
    @EnumValue("PLATFORM_AUTH_INVALID")
    PLATFORM_AUTH_INVALID(true),

    /**
     * [GCM] [APNS]
     * A registration token is tied to a certain group of senders. When a client app registers for GCM,
     * it must specify which senders are allowed to send messages. You should use one of those sender
     * IDs when sending messages to the client app. If you switch to a different sender,
     * the existing registration tokens won't work.
     */
    @EnumValue("PLATFORM_AUTH_MISMATCHED")
    PLATFORM_AUTH_MISMATCHED(true),

    /**
     * [GCM] [APNS]
     *
     * The rate of messages to a particular device is too high. Reduce the number of
     * messages sent to this device and do not immediately retry sending to this device.
     */
    @EnumValue("PLATFORM_LIMIT_EXCEEDED")
    PLATFORM_LIMIT_EXCEEDED(false),

    /**
     * [GCM] [APNS]
     * <p>
     * The server encountered an error while trying to process the request. You could retry the same
     * request following the requirements listed in "Timeout" (see row above). If the error persists,
     * please report the problem in the android-gcm group.
     */
    @EnumValue("TEMPORARILY_UNAVAILABLE")
    TEMPORARILY_UNAVAILABLE(false),

    /**
     * [GCM] [APNS]
     * <p>
     * Check that the total size of the payload data included in a message does not exceed GCM limits:
     * 4096 bytes for most messages, or 2048 bytes in the case of messages to topics or notification
     * messages on iOS. This includes both the keys and the values.
     */
    @EnumValue("MESSAGE_TOO_LARGE")
    MESSAGE_TOO_LARGE(true),

    /**
     * [GCM] [APNS]
     *
     * Check that the request contains a registration token (in the registration_id
     * in a plain text message, or in the to or registration_ids field in JSON).
     */
    @EnumValue("MESSAGE_REGISTRATIONS_MISSING")
    MESSAGE_REGISTRATIONS_MISSING(true),

    /**
     * [GCM] [APNS]
     *
     * Make sure the message was addressed to a registration token whose package
     * name matches the value passed in the request.
     */

    @EnumValue("MESSAGE_PACKAGE_INVALID")
    MESSAGE_PACKAGE_INVALID(true),

    /**
     * [GCM] [APNS]
     *
     * Check that the payload data does not contain a key (such as from, or gcm, or any value prefixed
     * by google) that is used internally by GCM. Note that some words (such as collapse_key) are also
     * used by GCM but are allowed in the payload, in which case the payload value will be
     * overridden by the GCM value.
     */
    @EnumValue("MESSAGE_PAYLOAD_INVALID")
    MESSAGE_PAYLOAD_INVALID(true),

    /**
     * [GCM] [APNS]
     */
    @EnumValue("MESSAGE_TTL_INVALID")
    MESSAGE_TTL_INVALID(true),

    /**
     * [GCM] [APNS]
     *
     * You are sending too many messages. TaskQueue will exponentially retry until message max
     * retries has been reached.
     */
    @EnumValue("RECIPIENT_RATE_EXCEEDED")
    RECIPIENT_RATE_EXCEEDED(false),

    /**
     * [GCM] [ANS]
     * <p>
     * An existing registration token may cease to be valid in a number of scenarios, including:
     * - If the client app unregisters with GCM.
     * - If the client app is automatically unregistered, which can happen if the user uninstalls
     * the application. For example, on iOS, if the APNS Feedback Service reported the APNS
     * token as invalid.
     * - If the registration token expires (for example, Google might decide to refresh registration
     * tokens, or the APNS token has expired for iOS devices).
     * - If the client app is updated but the new version is not configured to receive messages.
     * <p>
     * For all these cases, remove this registration token from the app server and stop using it
     * to send messages.
     */
    @EnumValue("RECIPIENT_NOT_REGISTERED")
    RECIPIENT_NOT_REGISTERED(true),

    /**
     * [GCM] [APNS]
     * <p>
     * Check the format of the registration token you pass to the server. Make sure it
     * matches the registration token the client app receives from registering with GCM.
     * Do not truncate or add additional characters.
     */
    @EnumValue("RECIPIENT_REGISTRATION_INVALID")
    RECIPIENT_REGISTRATION_INVALID(true),

    /**
     * [GCM [APNS]
     *
     * Unknown Error. Check the {@link PlatformFailure}
     * error message.
     */
    @EnumValue("ERROR_UNKNOWN")
    ERROR_UNKNOWN(true);

    /**
     * true if the error non recoverable and no messages were delivered.
     */
    public boolean isFatal;
    FailureType(boolean critical) {
        isFatal = critical;
    }
}