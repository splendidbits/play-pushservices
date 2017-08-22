package helpers.pushservices;

import enums.pushservices.Failure;

import javax.annotation.Nonnull;

/**
 * Static methods for commonly used GCM Platform functions.
 * <p>
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 5/11/16 Splendid Bits.
 */
public class PlatformHelper {

    /**
     * Get a GCM Response failure type for a given GCM error.
     *
     * @param error GCM response error.
     * @return failureType error for Message or Recipient.
     */
    public static Failure getGcmFailureType(@Nonnull String error) {
        Failure failure;

        switch (error) {
            case "MissingRegistration":
                failure = Failure.MESSAGE_REGISTRATIONS_MISSING;
                break;
            case "InvalidRegistration":
                failure = Failure.RECIPIENT_REGISTRATION_INVALID;
                break;
            case "NotRegistered":
                failure = Failure.RECIPIENT_NOT_REGISTERED;
                break;
            case "DeviceMessageRate Exceeded":
                failure = Failure.RECIPIENT_RATE_EXCEEDED;
                break;
            case "InvalidPackageName":
                failure = Failure.MESSAGE_PACKAGE_INVALID;
                break;
            case "MismatchSenderId":
                failure = Failure.PLATFORM_AUTH_MISMATCHED;
                break;
            case "MessageTooBig":
                failure = Failure.MESSAGE_TOO_LARGE;
                break;
            case "InvalidDataKey":
                failure = Failure.MESSAGE_PAYLOAD_INVALID;
                break;
            case "InvalidTtl":
                failure = Failure.MESSAGE_TTL_INVALID;
                break;
            case "DeviceMessageRate":
                failure = Failure.PLATFORM_LIMIT_EXCEEDED;
                break;
            case "Unavailable":
                failure = Failure.TEMPORARILY_UNAVAILABLE;
                break;
            default:
                failure = Failure.ERROR_UNKNOWN;
                break;
        }

        if (error.contains("401") || error.contains("Authentication")) {
            return Failure.PLATFORM_AUTH_INVALID;
        }
        return failure;
    }

    /**
     * Get a human readable error for a {@link Failure}.
     * @param failure Failure to get message string for.
     * @return Error string.
     */
    public static String getGcmFailureName(@Nonnull Failure failure) {
        if (failure == Failure.MESSAGE_REGISTRATIONS_MISSING) {
            return "MissingRegistration";

        } else if (failure == Failure.RECIPIENT_REGISTRATION_INVALID) {
            return "InvalidRegistration";

        } else if (failure == Failure.RECIPIENT_NOT_REGISTERED) {
            return "NotRegistered";

        } else if (failure == Failure.RECIPIENT_RATE_EXCEEDED) {
            return "DeviceMessageRate Exceeded";

        } else if (failure == Failure.MESSAGE_PACKAGE_INVALID) {
            return "InvalidPackageName";

        } else if (failure == Failure.PLATFORM_AUTH_MISMATCHED) {
            return "MismatchSenderId";

        } else if (failure == Failure.MESSAGE_TOO_LARGE) {
            return "MessageTooBig";

        } else if (failure == Failure.MESSAGE_PAYLOAD_INVALID) {
            return "InvalidDataKey";

        } else if (failure == Failure.MESSAGE_TTL_INVALID) {
            return "InvalidTtl";

        } else if (failure == Failure.PLATFORM_LIMIT_EXCEEDED) {
            return "DeviceMessageRate";

        } else if (failure == Failure.TEMPORARILY_UNAVAILABLE) {
            return "Unavailable";

        } else if (failure == Failure.PLATFORM_AUTH_INVALID) {
            return "401 Authentication Error";

        } else {
            return "Unknown Error";
        }
    }
}
