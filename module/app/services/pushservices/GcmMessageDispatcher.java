package services.pushservices;

import appmodels.pushservices.GcmResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import enums.pushservices.FailureType;
import enums.pushservices.PlatformType;
import enums.pushservices.RecipientState;
import exceptions.pushservices.PlatformEndpointException;
import helpers.pushservices.MessageHelper;
import helpers.pushservices.PlatformHelper;
import interfaces.pushservices.PlatformResponse;
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
        CompletableFuture.runAsync(() -> dispatchMessageInternal(message, responseListener));
    }

    @Override
    public PlatformType getPlatform() {
        return PlatformType.SERVICE_GCM;
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
        if (message.getRecipients() == null || message.getRecipients().isEmpty()) {
            PlatformFailure failure = new PlatformFailure(FailureType.MESSAGE_REGISTRATIONS_MISSING,
                    PlatformHelper.getGcmFailureName(FailureType.MESSAGE_REGISTRATIONS_MISSING), currentTime);

            for (Recipient recipient : message.getRecipients()) {
                recipient.setState(RecipientState.STATE_FAILED);
                recipient.setFailure(failure);
            }
            responseListener.messageFailure(message, failure);
            return;
        }

        // Return error on no platform.
        if (message.getCredentials() == null || message.getCredentials().getPlatformType() == null) {
            PlatformFailure failure = new PlatformFailure(FailureType.PLATFORM_AUTH_INVALID,
                    PlatformHelper.getGcmFailureName(FailureType.PLATFORM_AUTH_INVALID), currentTime);

            for (Recipient recipient : message.getRecipients()) {
                recipient.setState(RecipientState.STATE_FAILED);
                recipient.setFailure(failure);
            }
            responseListener.messageFailure(message, failure);
            return;
        }

        // Split the recipients into "batches" of 1000 as it could be over the max size for a GCM message.
        final Map<Integer, List<Recipient>> recipientBatches = batchMessageRecipients(message);
        Set<Integer> processedBatches = new HashSet<>();
        ConcurrentHashMap<Integer, GcmResponse> messageBatchResponses = new ConcurrentHashMap<>();

        // Iterate through each recipient batch and get a GcmResponse for it. Then join all batches.
        for (Map.Entry<Integer, List<Recipient>> batchEntry : recipientBatches.entrySet()) {
            final int batchNumber = batchEntry.getKey();
            final List<Recipient> batch = batchEntry.getValue();

            // Send each message and save the response. When all responses are returned, combine them and call listener.
            sendMessage(message, batch)
                    .thenAccept(response -> {
                        processedBatches.add(batchNumber);

                        GcmResponse gcmResponse = parseMessageResponse(response);
                        messageBatchResponses.put(batchNumber, gcmResponse);
                        Logger.debug(String.format("Finished parsing GCM Response for batch %d", batchNumber));

                        // If all the provider responses have returned, combine all and return the result.
                        if (processedBatches.size() == recipientBatches.size()) {
                            MessageDispatchResult result = combineResponses(recipientBatches, messageBatchResponses, message.getMaximumRetries());
                            responseListener.messageSuccess(message, result.completedRecipients, result.failedRecipients,
                                    result.recipientsToUpdate, result.recipientsToRetry);
                        }

                    }).exceptionally(

                    e -> {
                        PlatformFailure newFailure = new PlatformFailure(FailureType.ERROR_UNKNOWN,
                                PlatformHelper.getGcmFailureName(FailureType.ERROR_UNKNOWN), new Date());

                        if (e instanceof PlatformEndpointException) {
                            PlatformEndpointException exception = (PlatformEndpointException) e;

                            for (Recipient recipient : message.getRecipients()) {
                                int statusCode = exception.statusCode;

                                if (statusCode == 400) {
                                    recipient.setState(RecipientState.STATE_FAILED);
                                    newFailure = new PlatformFailure(FailureType.MESSAGE_PAYLOAD_INVALID,
                                            PlatformHelper.getGcmFailureName(FailureType.MESSAGE_PAYLOAD_INVALID));

                                } else if (statusCode == 401) {
                                    recipient.setState(RecipientState.STATE_FAILED);
                                    newFailure = new PlatformFailure(FailureType.PLATFORM_AUTH_INVALID,
                                            PlatformHelper.getGcmFailureName(FailureType.PLATFORM_AUTH_INVALID));

                                } else if (statusCode == 420) {
                                    MessageHelper.setRecipientRetry(getPlatform(), recipient, message.getMaximumRetries());
                                    newFailure = new PlatformFailure(FailureType.RECIPIENT_RATE_EXCEEDED,
                                            PlatformHelper.getGcmFailureName(FailureType.RECIPIENT_RATE_EXCEEDED));

                                } else {
                                    recipient.setState(RecipientState.STATE_FAILED);
                                    newFailure = new PlatformFailure(FailureType.ERROR_UNKNOWN,
                                            PlatformHelper.getGcmFailureName(FailureType.ERROR_UNKNOWN));
                                }

                                if (recipient.getPlatformFailure() == null) {
                                    recipient.setFailure(newFailure);
                                }
                            }
                        }

                        if (processedBatches.size() == recipientBatches.size()) {
                            responseListener.messageFailure(message, newFailure);
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
        if (response.getHeaders().containsKey("Retry-After")) {
            throw new PlatformEndpointException(420, response.getBody());

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
        List<Recipient> totalMessageRecipients = message.getRecipients();
        if (totalMessageRecipients != null && !totalMessageRecipients.isEmpty()) {

            List<Recipient> currentBatchRecipients = new ArrayList<>();
            int batchNumber = 1;
            int recipientCount = 0;

            for (Recipient recipient : totalMessageRecipients) {
                if (MessageHelper.isRecipientCoolingOff(recipient)) {
                    continue;
                }

                // If there's ~1000 registrations, create a new batch
                if (recipientCount == MESSAGE_RECIPIENT_BATCH_SIZE && !currentBatchRecipients.isEmpty()) {
                    sortedBatches.put(batchNumber, currentBatchRecipients);

                    // Reset counters and batch recipients.
                    currentBatchRecipients = new ArrayList<>();
                    recipientCount = 0;
                    batchNumber++;
                }

                currentBatchRecipients.add(recipient);
                recipientCount += 1;
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
        Logger.info(String.format("Sending message %d with %d recipients to the Google GCM endpoint", message.getId(), recipients.size()));

        String jsonBody = new GsonBuilder()
                .registerTypeAdapter(Message.class, new GcmMessageSerializer(recipients))
                .create()
                .toJson(message);

        return mWsClient
                .url(message.getCredentials().getPlatformType().url)
                .setContentType("application/json")
                .setHeader("Authorization", String.format("key=%s", message.getCredentials().getAuthKey()))
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
                                                   @Nonnull Map<Integer, GcmResponse> gcmResponses, int maxRetries) {
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
                    recipient.setState(RecipientState.STATE_COMPLETE);
                    messageDispatchResult.completedRecipients.add(recipient);
                }

                // Check for changed registration token.
                if (resultData.registrationId != null && !resultData.registrationId.isEmpty()) {
                    recipient.setState(RecipientState.STATE_COMPLETE);
                    Recipient updatedRecipient = new Recipient(resultData.registrationId);
                    messageDispatchResult.recipientsToUpdate.add(new UpdatedRecipient(recipient, updatedRecipient));
                }

                // Check for recipient errors.
                if (resultData.error != null && !resultData.error.isEmpty()) {
                    FailureType failureType = PlatformHelper.getGcmFailureType(resultData.error);
                    PlatformFailure platformFailure = new PlatformFailure(failureType, resultData.error, date);

                    if (failureType.isFatal) {
                        recipient.setState(RecipientState.STATE_FAILED);
                    } else {
                        MessageHelper.setRecipientRetry(getPlatform(), recipient, maxRetries);
                    }

                    if (recipient.getPlatformFailure() == null) {
                        recipient.setFailure(platformFailure);
                    }

                    // Add the error for that particular registration
                    messageDispatchResult.failedRecipients.add(recipient);
                }

                // Bump the master registration counter for all parts.
                recipientIndex++;
            }
        }
        return messageDispatchResult;
    }

    private class MessageDispatchResult {
        List<Recipient> completedRecipients = new ArrayList<>();
        List<Recipient> recipientsToRetry = new ArrayList<>();
        List<UpdatedRecipient> recipientsToUpdate = new ArrayList<>();
        List<Recipient> failedRecipients = new ArrayList<>();
    }
}
