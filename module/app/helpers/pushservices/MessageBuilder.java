package helpers.pushservices;

import enums.pushservices.MessagePriority;
import exceptions.pushservices.TaskValidationException;
import models.pushservices.db.Credentials;
import models.pushservices.db.Message;
import models.pushservices.db.PayloadElement;
import models.pushservices.db.Recipient;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Easily build a Push Service's Platform {@link Message} which can be then
 * added to a task and sent to the platform providers using the {@link java.util.TaskQueue}.
 */
public class MessageBuilder {
    public static class Builder {
        private final int ONE_WEEK_IN_SECONDS = 60 * 60 * 24 * 7;
        private final String DEFAULT_COLLAPSE_KEY = "default_collapse";
        private Credentials mCredentials;
        private Set<String> mMessageTokens = new HashSet<>();
        private Map<String, String> mMessageData = new HashMap<>();
        private MessagePriority mMessagePriority = MessagePriority.PRIORITY_LOW;
        private int mTtl = ONE_WEEK_IN_SECONDS;
        private boolean mShouldDelayWhileIdle = true;
        private boolean mIsDryRun = false;
        private String mCollapseKey;

        /**
         * Build the platform raw push service platform message.
         *
         * @return Push Service {@link Message} for a push platform.
         */
        @Nullable
        public Message build() throws TaskValidationException {
            Message message = new Message();
            message.credentials = mCredentials;
            message.collapseKey = mCollapseKey != null ? mCollapseKey : DEFAULT_COLLAPSE_KEY;
            message.ttlSeconds = mTtl;
            message.dryRun = mIsDryRun;
            message.shouldDelayWhileIdle = mShouldDelayWhileIdle;
            message.messagePriority = mMessagePriority;

            // Add recipients.
            for (String token : mMessageTokens) {
                Recipient recipient = new Recipient(token);
                message.addRecipient(recipient);
            }

            // Validate that the necessary attributes have been set.
            if (message.credentials == null) {
                throw new RuntimeException("Credentials were missing set attributes.");
            }

            // Add data.
            message.payloadData = new ArrayList<>();
            for (Map.Entry<String, String> entry : mMessageData.entrySet()) {
                message.payloadData.add(new PayloadElement(entry.getKey(), entry.getValue()));
            }

            boolean hasAuthKey = message.credentials.authKey != null && !message.credentials.authKey.isEmpty();
            boolean hasCertBody = message.credentials.certBody != null && !message.credentials.certBody.isEmpty();
            boolean hasPlatformType = message.credentials.platformType != null;

            if (!hasAuthKey && !hasCertBody) {
                throw new TaskValidationException("Either an auth key or cert must be supplied.");

            } else if (!hasPlatformType) {
                throw new TaskValidationException("A PlatformType must be supplied.");
            }

            return message;
        }

        /**
         * Add a of platform device token for the message to be sent to.
         *
         * @param tokens device tokens to send message to.
         */
        public Builder setDeviceTokens(@Nonnull Set<String> tokens) {
            mMessageTokens = tokens;
            return this;
        }

        /**
         * Setting this to true allows a response back from the server for a sent message,
         * but it not to end up on the devices.
         *
         * @param isDryRun true if the message should not actually be sent. defaults to false.
         */
        public Builder setIsDryRun(boolean isDryRun) {
            mIsDryRun = isDryRun;
            return this;
        }

        /**
         * Add a set of key/value payload attributes to the message. This can be anything you want,
         * but is limited to 4096 bytes for all payload.
         *
         * @param attributeKey   key of message attribute.
         * @param attributeValue value of message attribute.
         */
        public Builder putData(String attributeKey, String attributeValue) {
            if (attributeKey != null && attributeValue != null) {
                mMessageData.put(attributeKey, attributeValue);
            }
            return this;
        }

        /**
         * Add a set of key/value payload attributes to the message. This can be anything you want,
         * but is limited to 4096 bytes for all payload.
         *
         * @param messageDataAttributes attribute keys and values for message payload.
         */
        public Builder setData(@Nonnull Map<String, String> messageDataAttributes) {
            mMessageData = messageDataAttributes;
            return this;
        }

        /**
         * Set the platform account (push services) that contains endpoints, platform type,
         * and authorisation keys, etc for that provider.
         *
         * @param credentials push service platform credentials.
         */
        public Builder setPlatformCredentials(@Nonnull Credentials credentials) {
            try {
                mCredentials = (Credentials) credentials.clone();

            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
            return this;
        }

        /**
         * Set the time-to-live for a push message (can differs between platforms, but is usually
         * the time an unread or unsent message is kept on the server before giving up sending.
         *
         * @param ttl time to live in seconds. Default is one week.
         */
        public Builder setTimeToLiveSeconds(int ttl) {
            mTtl = ttl;
            return this;
        }

        /**
         * Sets the importance of the platform Message. Sometimes has an effect on either delivery
         * or device notification timing. For example, {@link MessagePriority} PRIORITY_HIGH will
         * also set the setShouldDelayWhileIdle attribute to false. and on GCM will ignore Doze mode.
         * <p>
         * Note: Be considerate of users' time. Set the priority to use the lowest priority the
         * message data can afford.
         *
         * @param messagePriority Priority of platform message.
         */
        public Builder setMessagePriority(@Nonnull MessagePriority messagePriority) {
            mMessagePriority = messagePriority;
            return this;
        }

        /**
         * Should the message Wake up the device (and often screen, sound etc) or is it okay to only
         * alert when the user is engaging with device.
         * <p>
         * Setting to false will override the {@link MessagePriority} and set it
         * to MessagePriority.HIGH.
         *
         * @deprecated Google have deprecated this feature in favor of setMessagePriority
         * @param shouldDelayWhileIdle default is true.
         */
        @Deprecated
        public Builder setShouldDelayWhileIdle(boolean shouldDelayWhileIdle) {
            mShouldDelayWhileIdle = shouldDelayWhileIdle;
            return this;
        }

        /**
         * Set the collapse key. If two messages are sent to a device with the same key, the first message
         * (alert, etc) will be overwritten by the second.
         *
         * @param collapseKey collapse key string.
         */
        public Builder setCollapseKey(@Nonnull String collapseKey) {
            mCollapseKey = collapseKey;
            return this;
        }

    }
}
