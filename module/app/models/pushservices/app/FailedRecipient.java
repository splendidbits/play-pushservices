package models.pushservices.app;

import models.pushservices.db.PlatformFailure;
import models.pushservices.db.Recipient;

import javax.annotation.Nonnull;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 27/12/2016 Splendid Bits.
 */
public class FailedRecipient {
    private Recipient mRecipient;
    private PlatformFailure mFailure;

    // Private access for immutability.
    private FailedRecipient() {
    }

    public FailedRecipient(@Nonnull Recipient recipient, @Nonnull PlatformFailure failure) {
        mRecipient = recipient;
        mFailure = failure;
    }

    @Nonnull
    public Recipient getRecipient() {
        return mRecipient;
    }

    @Nonnull
    public PlatformFailure getFailure() {
        return mFailure;
    }
}
