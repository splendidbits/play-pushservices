package services.pushservices;

import appmodels.pushservices.GcmResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import enums.pushservices.Failure;
import enums.pushservices.RecipientState;
import exceptions.pushservices.PlatformEndpointException;
import helpers.pushservices.PlatformHelper;
import helpers.pushservices.TaskHelper;
import interfaces.pushservices.PlatformResponse;
import models.pushservices.app.FailedRecipient;
import models.pushservices.app.UpdatedRecipient;
import models.pushservices.db.Message;
import models.pushservices.db.PlatformFailure;
import models.pushservices.db.Recipient;
import play.Logger;
import play.libs.ws.WSClient;
import play.libs.ws.WSResponse;
import serializers.pushservices.GcmMessageSerializer;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Sends GCM alerts to a batch of given tokens and data.
 * <p>
 * - This class just deals with GCM concepts:
 * - A list of registration_ids
 * - A collapse_key
 * - The keys and string data to send.
 */
public class GcmMessageDispatcher extends PlatformMessageDispatcher {
    private static final int ENDPOINT_REQUEST_TIMEOUT_SECONDS = 60 * 1000;
    private static final int MESSAGE_RECIPIENT_BATCH_SIZE = 1000;
    private WSClient mWsClient;

    private GcmMessageDispatcher() {
    }

    @Inject
    protected GcmMessageDispatcher(WSClient wsClient) {
        mWsClient = wsClient;
    }

    /**
     * Dispatch a message synchronously to the Google GCM service.
     *
     * @param message          The message to send.
     * @param responseListener The response listener.
     */
    public void dispatchMessage(@Nonnull Message message, @Nonnull PlatformResponse responseListener) {
        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                dispatchMessageInternal(message, responseListener);
            }
        });
    }

    /**
     * Dispatch a message synchronously to the Google GCM service.
     *
     * @param message          The message to send.
     * @param responseListener The response listener.
     */
    private void dispatchMessageInternal(@Nonnull Message message, @Nonnull PlatformResponse responseListener) {
        final Date currentTime = new Date();

        // Return error on no recipients.
        if (message.recipients == null || message.recipients.isEmpty()) {
            PlatformFailure platformFailure = new PlatformFailure(Failure.MESSAGE_REGISTRATIONS_MISSING,
                    PlatformHelper.getGcmFailureName(Failure.MESSAGE_REGISTRATIONS_MISSING), currentTime);
            responseListener.messageFailure(message, platformFailure);
            return;
        }

        // Return error on no platform.
        if (message.credentials == null || message.credentials.platformType == null) {
            PlatformFailure platformFailure = new PlatformFailure(Failure.PLATFORM_AUTH_INVALID,
                    PlatformHelper.getGcmFailureName(Failure.PLATFORM_AUTH_INVALID), currentTime);

            responseListener.messageFailure(message, platformFailure);
            return;
        }

        // Split the recipients into "batches" of 1000 as it could be over the max size for a GCM message.
        final Map<Integer, List<Recipient>> recipientBatches = batchMessageRecipients(message);
        List<Integer> processedBatches = new CopyOnWriteArrayList<>();
        ConcurrentHashMap<Integer, GcmResponse> messageBatchResponses = new ConcurrentHashMap<>();

        // Iterate through each recipient batch and get a GcmResponse for it. Then join all batches.
        for (Map.Entry<Integer, List<Recipient>> batchEntry : recipientBatches.entrySet()) {
            final int batchNumber = batchEntry.getKey();
            final List<Recipient> batch = batchEntry.getValue();

            // Send each message and save the response. When all responses are returned, combine them and call listener.
            sendMessage(message, batch).thenAccept(new Consumer<WSResponse>() {

                @Override
                public void accept(WSResponse response) {
                    GcmResponse gcmResponse = parseMessageResponse(response);
                    messageBatchResponses.put(batchNumber, gcmResponse);
                    processedBatches.add(batchNumber);
                    Logger.debug(String.format("Finished parsing GCM Response for batch %d", batchNumber));

                    // If all the provider responses have returned, combine all and return the result.
                    if (processedBatches.size() == recipientBatches.size()) {
                        MessageDispatchResult result = combineResponses(recipientBatches, messageBatchResponses);
                        responseListener.messageSuccess(message, result.completedRecipients, result.failedRecipients,
                                result.recipientsToUpdate, result.recipientsToRetry);
                    }
                }

            }).exceptionally(
                    e -> {
                        if (e instanceof PlatformEndpointException) {
                            // Transform a known exception into a application {@link PlatformFailure}.
                            PlatformEndpointException exception = (PlatformEndpointException) e;

                            if (exception.mStatusCode == 400) {
                                PlatformFailure platformFailure = new PlatformFailure(Failure.MESSAGE_PAYLOAD_INVALID,
                                        PlatformHelper.getGcmFailureName(Failure.MESSAGE_PAYLOAD_INVALID), currentTime);

                                responseListener.messageFailure(message, platformFailure);

                            } else if (exception.mStatusCode == 401) {
                                PlatformFailure platformFailure = new PlatformFailure(Failure.PLATFORM_AUTH_INVALID,
                                        PlatformHelper.getGcmFailureName(Failure.PLATFORM_AUTH_INVALID), currentTime);

                                responseListener.messageFailure(message, platformFailure);

                            } else if (exception.mStatusCode >= 500 && exception.mStatusCode <= 599) {
                                Calendar earliestRetryDate = Calendar.getInstance();

                                MessageDispatchResult messageDispatchResult = new MessageDispatchResult();
                                PlatformFailure platformFailure = new PlatformFailure(Failure.TEMPORARILY_UNAVAILABLE,
                                        PlatformHelper.getGcmFailureName(Failure.TEMPORARILY_UNAVAILABLE), currentTime);

                                for (Recipient recipient : message.recipients) {
                                    earliestRetryDate.setTime(recipient.timeAdded);
                                    earliestRetryDate.add(Calendar.MINUTE, (recipient.sendAttemptCount * 2));

                                    recipient.state = RecipientState.STATE_WAITING_RETRY;
                                    recipient.failure = platformFailure;
                                    TaskHelper.setRecipientCoolingOff(recipient, earliestRetryDate);

                                    messageDispatchResult.recipientsToRetry.add(new FailedRecipient(recipient, platformFailure));
                                }

                                responseListener.messageSuccess(message, messageDispatchResult.completedRecipients,
                                        messageDispatchResult.failedRecipients, messageDispatchResult.recipientsToUpdate,
                                        messageDispatchResult.recipientsToRetry);

                            } else {
                                PlatformFailure platformFailure = new PlatformFailure(
                                        Failure.ERROR_UNKNOWN,
                                        PlatformHelper.getGcmFailureName(Failure.ERROR_UNKNOWN),
                                        currentTime);

                                responseListener.messageFailure(message, platformFailure);
                            }
                        }
                        return null;
                    });
        }
    }

    /**
     * Parse an incoming response back from a GCM send action.
     *
     * @param response PlatformMessageResult The WSResponse back from google which
     *                 should contain a json success / fail map.
     * @return GcmResponse result from google for gcm message.
     */
    private GcmResponse parseMessageResponse(WSResponse response) {
        if (response.getAllHeaders().containsKey("Retry-After")) {
            throw new PlatformEndpointException(500, response.getBody());

        } else if (response.getStatus() != 200) {
            throw new PlatformEndpointException(response.getStatus(), response.getBody());
        }

        GcmResponse gcmResponse = new Gson().fromJson(response.getBody(), GcmResponse.class);
        Logger.debug(String.format("\n" +
                        "[%1$d] canonical ids.\n" +
                        "[%2$d] successful message recipients.\n" +
                        "[%3$d] failed message recipients.",
                gcmResponse.canonicalIdCount, gcmResponse.successCount, gcmResponse.failCount));
        return gcmResponse;
    }

    /**
     * Internally send a message using the GCM protocol to google. If a message contains
     * more than 1000 registration ids, it'll split that into multiples messages.
     *
     * @param message the message.
     * @return A list of organised recipients in batches of 1000.
     */
    @Nonnull
    private Map<Integer, List<Recipient>> batchMessageRecipients(@Nonnull Message message) {
        HashMap<Integer, List<Recipient>> sortedBatches = new HashMap<>();

        // Add each registration_id to the message in batches of 1000.
        List<Recipient> totalMessageRecipients = message.recipients;
        if (totalMessageRecipients != null && !totalMessageRecipients.isEmpty()) {

            List<Recipient> currentBatchRecipients = new ArrayList<>();
            int batchNumber = 1;
            int recipientCount = 0;

            for (Recipient recipient : totalMessageRecipients) {
                // Do not count or add non-valid recipients.
                if (TaskHelper.isRecipientPending(recipient) && !TaskHelper.isRecipientCoolingOff(recipient)) {

                    // If there's ~1000 registrations, create a new batch
                    if (recipientCount == MESSAGE_RECIPIENT_BATCH_SIZE && !currentBatchRecipients.isEmpty()) {
                        sortedBatches.put(batchNumber, currentBatchRecipients);

                        // Reset counters and batch recipients.
                        currentBatchRecipients = new CopyOnWriteArrayList<>();
                        recipientCount = 0;
                        batchNumber++;
                    }

                    currentBatchRecipients.add(recipient);
                    recipientCount += 1;
                }
            }

            // When done, add the current batch recipients to the map;
            if (!currentBatchRecipients.isEmpty()) {
                sortedBatches.put(batchNumber, currentBatchRecipients);
            }

        }
        return sortedBatches;
    }

    /**
     * Send a message and get a synchronous application response in return.
     *
     * @param message    the message or message part to send.
     * @param recipients blocks of 1000 recipients inside a collection.
     * @return WSResponse google request response.
     */
    @Nonnull
    private CompletionStage<WSResponse> sendMessage(@Nonnull Message message, @Nonnull List<Recipient> recipients) {
        String jsonBody = new GsonBuilder()
                .registerTypeAdapter(Message.class, new GcmMessageSerializer(recipients))
                .create()
                .toJson(message);

        return mWsClient
                .url(message.credentials.platformType.url)
                .setContentType("application/json")
                .setHeader("Authorization", String.format("key=%s", message.credentials.authKey))
                .setRequestTimeout(ENDPOINT_REQUEST_TIMEOUT_SECONDS)
                .setFollowRedirects(true)
                .post(jsonBody);
    }

    /**
     * Combine a list of Google response for a given list of responses for messages sent.
     *
     * @param recipientBatches list of recipient batches included in the message-set.
     * @param gcmResponses     A list of received GoogleResponses.
     * @return The master GoogleResponse for the original message send back to the client.
     */
    @Nonnull
    private MessageDispatchResult combineResponses(@Nonnull Map<Integer, List<Recipient>> recipientBatches,
                                                   @Nonnull Map<Integer, GcmResponse> gcmResponses) {
        MessageDispatchResult messageDispatchResult = new MessageDispatchResult();
        Date date = new Date();

        // Iterate over each registration result from each message sent.
        for (final Map.Entry<Integer, GcmResponse> responseEntry : gcmResponses.entrySet()) {
            int batchNumber = responseEntry.getKey();

            GcmResponse response = responseEntry.getValue();
            List<Recipient> batchRecipients = recipientBatches.get(batchNumber);

            int recipientIndex = 0;
            for (GcmResponse.ResultData resultData : response.results) {
                Recipient recipient = batchRecipients.get(recipientIndex);

                // A successful message.
                if (resultData.messageId != null && !resultData.messageId.isEmpty()) {
                    messageDispatchResult.completedRecipients.add(recipient);
                }

                // Check for changed registration token.
                if (resultData.registrationId != null && !resultData.registrationId.isEmpty()) {
                    Recipient updatedRecipient = new Recipient(resultData.registrationId);
                    messageDispatchResult.recipientsToUpdate.add(new UpdatedRecipient(recipient, updatedRecipient));
                }

                // Check for recipient errors.
                if (resultData.error != null && !resultData.error.isEmpty()) {
                    Failure failure = PlatformHelper.getGcmFailureType(resultData.error);
                    PlatformFailure platformFailure = new PlatformFailure(failure, resultData.error, date);
                    recipient.failure = platformFailure;

                    // Add the error for that particular registration
                    messageDispatchResult.failedRecipients.add(new FailedRecipient(recipient, platformFailure));
                }

                // Bump the master registration counter for all parts.
                recipientIndex++;
            }
        }
        return messageDispatchResult;
    }

    private class MessageDispatchResult {
        CopyOnWriteArrayList<Recipient> completedRecipients = new CopyOnWriteArrayList<>();
        CopyOnWriteArrayList<FailedRecipient> recipientsToRetry = new CopyOnWriteArrayList<>();
        CopyOnWriteArrayList<UpdatedRecipient> recipientsToUpdate = new CopyOnWriteArrayList<>();
        CopyOnWriteArrayList<FailedRecipient> failedRecipients = new CopyOnWriteArrayList<>();
    }
}
