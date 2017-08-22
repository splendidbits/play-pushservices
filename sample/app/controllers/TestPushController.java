package controllers;

import enums.pushservices.PlatformType;
import exceptions.pushservices.TaskValidationException;
import helpers.pushservices.MessageBuilder;
import interfaces.pushservices.TaskQueueCallback;
import models.pushservices.app.FailedRecipient;
import models.pushservices.app.UpdatedRecipient;
import models.pushservices.db.Credentials;
import models.pushservices.db.Message;
import models.pushservices.db.PlatformFailure;
import models.pushservices.db.Task;
import play.Logger;
import play.mvc.Controller;
import play.mvc.Result;
import services.pushservices.TaskQueue;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.*;

/**
 * Illustrates the push-services module API.
 */
public class TestPushController extends Controller {
    private TaskQueue mTaskQueue;

    @Inject
    public TestPushController(TaskQueue taskQueue) {
        mTaskQueue = taskQueue;
    }

    public Result index() {
        /*
        * Send a push message to a set of recipients.
        * NOTE: Contains sample tokens / credentials that will fail. Replace with your own.
         */
        Credentials sampleCredentials = new Credentials();
        sampleCredentials.platformType = PlatformType.SERVICE_GCM;

        // Must be registered with push provider.
        sampleCredentials.packageUri = "com.company.app";

        // Auth secret (GCM).
        sampleCredentials.authKey = "AEFbawuefAWEFwaEFea9OAKFAEWfeawKk";

        // Auth secret (APNS).
        sampleCredentials.certBody = "-----BEGIN CERTIFICATE REQUEST-----\n" +
                "MIICvDCCAaQCAQAwdzELMAkGA1UEBhMCVVMxDTALBgNVBAgMBFV0YWgxDzANBgNV\n" +
                "BAcMBkxpbmRvbjEWMBQGA1UECgwNRGlnaUNlcnQgSW5jLjERMA8GA1UECwwIRGln\n" +
                "aUNlcnQxHTAbBgNVBAMMFGV4YW1wbGUuZGlnaWNlcnQuY29tMIIBIjANBgkqhkiG\n" +
                "90BAQEFAAOCAQ8AMIIBCgKCAQEA8d+To7d+2kPWeBv/orU3LVbJwDrSQbeKamCmo\n" +
                "wp5bqDxIwV20zqRb7APUOKYoVEFFOEQs6T6gImnIolhbiH6m4zgZ/CPvWBOkZc+c\n" +
                "1Po2EmvBz+AD5sBdT5kzGQA6NbWyZGldxRthNLOs1efOhdnWFuhI162qmcflgpiI\n" +
                "WDuwq4C9f+YkeJhNn9dF5+owm8cOQmDrV8NNdiTqin8q3qYAHHJRW28glJUCZkTZ\n" +
                "wIaSR6crBQ8TbYNE0dc+Caa3DOIkz1EOsHWzTx+n0zKfqcbgXi4DJx+C1bjptYPR\n" +
                "BPZL8DAeWuA8ebudVT44yEp82G96/Ggcf7F33xMxe0yc+Xa6owIDAQABoAAwDQYJ\n" +
                "KoZIhvcNAQEFBQADggEBAB0kcrFccSmFDmxox0Ne01UIqSsDqHgL+XmHTXJwre6D\n" +
                "hJSZwbvEtOK0G3+dr4Fs11WuUNt5qcLsx5a8uk4G6AKHMzuhLsJ7XZjgmQXGECpY\n" +
                "Q4mC3yT3ZoCGpIXbw+iP3lmEEXgaQL0Tx5LFl/okKbKYwIqNiyKWOMj7ZR/wxWg/\n" +
                "ZDGRs55xuoeLDJ/ZRFf9bI+IaCUd1YrfYcHIl3G87Av+r49YVwqRDT0VDV7uLgqn\n" +
                "29XI1PpVUNCPQGn9p/eX6Qo7vpDaPybRtA2R7XLKjQaF9oXWeCUqy1hvJac9QFO2\n" +
                "97Ob1alpHPoZ7mWiEuJwjBPii6a9M9G30nUo39lBi1w=\n" +
                "-----END CERTIFICATE REQUEST-----";

        Set<String> sampleDeviceTokens = new HashSet<>();
        sampleDeviceTokens.add("d1NqItxeuyk:APA91bFcidzSlRTlyijOP04UCR8KXHfxi4j2VHfLv9TcE14QwjckJ3qB4gm69zbCjRygt0AgDWORBTjZcUeaMXQ5x8U8mFJLx1Ss1h3oveMtG4Jrp_i_sw41pvGljjXwr9OzTJ84CV73");
        sampleDeviceTokens.add("fdiktPsnZDU:APA91bEHetahBunbXxD_-7RdoKVpIClSiijili1DUTtUDYJv00rBLTBf0nDsO4fEl1FURn9mPFdxhsMj1cxYUjIYCz8WSHcKnr-ST6jfiAGBd0TnDcZsFFEZfvbbuT8HHcxeCaMhWTLT");
        sampleDeviceTokens.add("BADTOKEN-fdiktPsnZDU:APA91bEHetahBunbXxD-ST6jfiAGBd0TnDcZsFFEZfvbbuT8HHcxeCaMhWTLT");

        Map<String, String> sampleData = new HashMap<>();
        sampleData.put("sample_first_key", "sample_first_value");
        sampleData.put("sample_second_key", "sample_second_value");

        MessageBuilder.Builder messageBuilder = new MessageBuilder.Builder()
                .setCollapseKey("piccadilly_line")
                .setPlatformCredentials(sampleCredentials)
                .setTimeToLiveSeconds(60 * 60 * 6)
                .setDeviceTokens(sampleDeviceTokens)
                .setData(sampleData);

        try {
            Task piccadillyStatus = new Task("piccadilly-line-update");
            piccadillyStatus.messages = Collections.singletonList(messageBuilder.build());

            mTaskQueue.queueTask(piccadillyStatus, new TaskQueueCallback() {
                @Override
                public void updatedRecipients(@Nonnull List<UpdatedRecipient> updatedRegistrations) {
                    Logger.info("Recipients have updated registration tokens");
                }

                @Override
                public void failedRecipients(@Nonnull List<FailedRecipient> failedRecipients) {
                    Logger.info("Message delivery failed for some recipients");
                }

                @Override
                public void messageCompleted(@Nonnull Message originalMessage) {
                    // failedRecipients() or updatedRecipients() may still have been invoked.
                    Logger.info("Message delivery has completed.");
                }

                @Override
                public void messageFailed(@Nonnull Message originalMessage, PlatformFailure failure) {
                    Logger.info("Message failed for all recipients");
                }
            });

        } catch (TaskValidationException e) {
            Logger.error("There was an error creating or queueing the task.");
            Logger.error(e.getMessage());
            return internalServerError();
        }

        return ok();
    }

    public Result robots() {
        return ok("User-agent: *\nDisallow: /");
    }
}
