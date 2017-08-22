package models.pushservices.app;

import models.pushservices.db.Recipient;

import javax.annotation.Nonnull;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 27/12/2016 Splendid Bits.
 */
public class UpdatedRecipient {
    private Recipient mStaleRecipient;
    private Recipient mUpdatedRecipient;

    // Private access for immutability.
    private UpdatedRecipient() {
    }

    public UpdatedRecipient(@Nonnull Recipient staleRecipient, @Nonnull Recipient updatedRecipient) {
        mStaleRecipient = staleRecipient;
        mUpdatedRecipient = updatedRecipient;
    }

    @Nonnull
    public Recipient getStaleRecipient() {
        return mStaleRecipient;
    }

    @Nonnull
    public Recipient getUpdatedRecipient() {
        return mUpdatedRecipient;
    }
}
