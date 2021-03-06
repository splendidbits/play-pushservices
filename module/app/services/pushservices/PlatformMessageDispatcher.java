package services.pushservices;

import enums.pushservices.PlatformType;
import helpers.pushservices.MessageBuilder;
import interfaces.pushservices.PlatformResponse;
import models.pushservices.db.Message;

import javax.annotation.Nonnull;

/**
 * An abstract class for extending and creating a Message Dispatcher for a push
 * service platform.
 */
public abstract class PlatformMessageDispatcher {


    /**
     * Dispatch a message synchronously to the Platform endpoint, and get responses back through
     * a response interface.
     *
     * @param message          The constructed platform message to send. Build using {@link MessageBuilder}.
     * @param responseListener The Platform response listener.
     */
    public abstract void dispatchMessage(@Nonnull Message message, @Nonnull PlatformResponse responseListener);

    /**
     * @return The Push Notification Service provider.
     */
    public abstract PlatformType getPlatform();


}