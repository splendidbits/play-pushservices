package controllers;

import enums.pushservices.PlatformType;
import exceptions.pushservices.MessageValidationException;
import helpers.pushservices.MessageBuilder;
import interfaces.pushservices.TaskQueueListener;
import models.pushservices.app.UpdatedRecipient;
import models.pushservices.db.Credentials;
import models.pushservices.db.Message;
import models.pushservices.db.PlatformFailure;
import models.pushservices.db.Recipient;
import play.Logger;
import play.mvc.Controller;
import play.mvc.Result;
import services.pushservices.TaskQueue;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Illustrates the push-services module API.
 */
public class SampleController extends Controller {
    private TaskQueue mTaskQueue;

    @Inject
    public SampleController(TaskQueue taskQueue) {
        mTaskQueue = taskQueue;
    }

    public Result robots() {
        return ok("User-agent: *\nDisallow: /");
    }

    public CompletionStage<Result> index() {
        return CompletableFuture.supplyAsync(() -> {
            /*
             * Send a push message to a set of recipients.
             * NOTE: Contains sample tokens / credentials that will fail. Replace with your own.
             */
            Credentials sampleCredentials = new Credentials(PlatformType.SERVICE_GCM);
            sampleCredentials.setPackageUri("com.company.app");
            sampleCredentials.setAuthKey("AEFbawuefAWEFwaEFea9OAKFAEWfeawKk");

            Set<String> sampleDeviceTokens = new HashSet<>();
            sampleDeviceTokens.add("d1NqItxeuykwdWQaefeAWEFwawefEWEDgregsdKXHfxi4j2VHfLv9TcE14QwjckJ3qB4gm6");
            sampleDeviceTokens.add("7RdoKVpIClSiijili1DUTtUDYJv00rBLTBf0nDsfdiktPsnZDU:APA91bEHetahBunbXxD");
            sampleDeviceTokens.add("fdiktPsnZDU:APA91bEHetahBunbXST6jfiAGBd0TnDcZsFFEZfvbbuT8HHcxeCaMhWTLT");

            Map<String, String> sampleData = new HashMap<>();
            sampleData.put("sample_first_key", "sample_first_value");
            sampleData.put("sample_second_key", "sample_second_value");

            MessageBuilder.Builder messageBuilder = new MessageBuilder.Builder()
                    .setCollapseKey("piccadilly_line")
                    .setPlatformCredentials(sampleCredentials)
                    .setTimeToLiveSeconds(60 * 60 * 6)
                    .setSoftFailRetries(2)
                    .setDeviceTokens(sampleDeviceTokens)
                    .setData(sampleData);

            try {
                mTaskQueue.queueMessages(Collections.singletonList(messageBuilder.build()), new TaskQueueListener() {
                    @Override
                    public void updatedRecipients(@Nonnull List<UpdatedRecipient> updatedRegistrations) {
                        Logger.info("Recipients have updated registration tokens");
                    }

                    @Override
                    public void failedRecipients(@Nonnull List<Recipient> failedRecipients) {
                        Logger.info("Message delivery failed for some recipients");
                    }

                    @Override
                    public void messageCompleted(@Nonnull Message originalMessage) {
                        Logger.info("Message delivery has completed.");
                    }

                    @Override
                    public void messageFailed(@Nonnull Message originalMessage, PlatformFailure failure) {
                        Logger.info("Message failed for all recipients");
                    }
                });

            } catch (MessageValidationException e) {
                Logger.error("There was an error creating or queueing the task.");
                Logger.error(e.getMessage());
                return badRequest();
            }

            return ok();
        });
    }
}
