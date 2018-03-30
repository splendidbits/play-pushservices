package dao.pushservices;

import enums.pushservices.FailureType;
import enums.pushservices.MessagePriority;
import enums.pushservices.PlatformType;
import enums.pushservices.RecipientState;
import exceptions.pushservices.MessageValidationException;
import helpers.pushservices.MessageBuilder;
import main.PushServicesApplicationTest;
import models.pushservices.db.*;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test core functions of the Device Data Access Layer.
 */
public class MessageDaoTest extends PushServicesApplicationTest {
    private static MessagesDao mMessagesDao;

    @BeforeClass
    public static void initialise() {
        mMessagesDao = PushServicesApplicationTest.application.injector().instanceOf(MessagesDao.class);
        mMessagesDao.wipeAll();
    }

    @Before
    public void beforeTest() {
    }

    @After
    public void afterTest() {
        mMessagesDao.wipeAll();
    }

    @Test
    public void testDatabaseMessageInsert() throws MessageValidationException {
        Credentials credentials1 = new Credentials(PlatformType.SERVICE_GCM);
        credentials1.setAuthKey("gcm_key");

        Credentials credentials2 = new Credentials(PlatformType.SERVICE_APNS);
        credentials2.setAuthKey("apns_key");

        Map<String, String> message1Data = new HashMap<>();
        message1Data.put("message1_item1", "value");
        message1Data.put("message1_item2", "value");

        Message message1 = new MessageBuilder.Builder()
                .setPlatformCredentials(credentials1)
                .setMessagePriority(MessagePriority.PRIORITY_HIGH)
                .setCollapseKey("collapse")
                .addDeviceToken("token1")
                .addDeviceToken("token2")
                .setData(message1Data)
                .setIsDryRun(true)
                .setShouldDelayWhileIdle(false)
                .setTimeToLiveSeconds(3600)
                .build();

        Message message2 = new MessageBuilder.Builder()
                .setPlatformCredentials(credentials2)
                .setMessagePriority(MessagePriority.PRIORITY_NORMAL)
                .setCollapseKey("collapse")
                .addDeviceToken("token2")
                .setIsDryRun(true)
                .setShouldDelayWhileIdle(false)
                .setTimeToLiveSeconds(3600)
                .addData("message2_item1", "value")
                .addData("message2_item2", "value")
                .build();

        assertNotNull(message1);
        assertNotNull(message2);

        assertTrue(mMessagesDao.saveMessage(message1));
        assertTrue(mMessagesDao.saveMessage(message2));

        List<Message> savedMessages = mMessagesDao.fetchMessages();
        Message savedMessage1 = savedMessages.get(0);
        Message savedMessage2 = savedMessages.get(1);

        assertNotNull(savedMessage1.getCredentials());
        assertNotNull(savedMessage2.getCredentials());

        List<PayloadElement> savedMessage1Payload = savedMessage1.getPayloadData();
        List<PayloadElement> savedMessage2Payload = savedMessage2.getPayloadData();
        assertEquals(2, savedMessage1Payload.size());
        assertEquals(2, savedMessage2Payload.size());

        assertTrue(savedMessage1Payload.get(0).getKey().contains("message1_item1") ||
                savedMessage1Payload.get(0).getKey().contains("message1_item2")
        );
        assertTrue(savedMessage1Payload.get(1).getKey().contains("message1_item1") ||
                savedMessage1Payload.get(1).getKey().contains("message1_item2")
        );

        assertTrue(savedMessage2Payload.get(0).getKey().contains("message2_item1") ||
                savedMessage2Payload.get(0).getKey().contains("message2_item2")
        );
        assertTrue(savedMessage2Payload.get(1).getKey().contains("message2_item1") ||
                savedMessage2Payload.get(1).getKey().contains("message2_item2")
        );

        assertEquals(2, savedMessage1.getRecipients().size());
        assertEquals(1, savedMessage2.getRecipients().size());
    }

    @Test
    public void testDatabaseMessageUpdate() throws MessageValidationException {
        Credentials credentials = new Credentials(PlatformType.SERVICE_GCM);
        credentials.setAuthKey("gcm_key");

        Map<String, String> message1Data = new HashMap<>();
        message1Data.put("message1_item1", "value");
        message1Data.put("message1_item2", "value");

        Message message = new MessageBuilder.Builder()
                .setPlatformCredentials(credentials)
                .setMessagePriority(MessagePriority.PRIORITY_HIGH)
                .setCollapseKey("collapse")
                .addDeviceToken("token1")
                .addDeviceToken("token2")
                .setIsDryRun(true)
                .setShouldDelayWhileIdle(false)
                .setTimeToLiveSeconds(3600)
                .setData(message1Data)
                .build();

        assertNotNull(message);
        assertTrue(mMessagesDao.saveMessage(message));

        List<Message> savedMessages = mMessagesDao.fetchMessages();
        Message savedMessage = savedMessages.get(0);

        assertEquals(RecipientState.STATE_IDLE, savedMessage.getRecipients().get(0).getState());
        assertEquals(RecipientState.STATE_IDLE, savedMessage.getRecipients().get(1).getState());
        assertNull(savedMessage.getRecipients().get(0).getPlatformFailure());
        assertNull(savedMessage.getRecipients().get(1).getPlatformFailure());

        // Update both recipients with different values
        for (Recipient recipient : savedMessage.getRecipients()) {
            if (recipient.getToken().equals("token1")) {
                recipient.setState(RecipientState.STATE_PROCESSING);
            }

            if (recipient.getToken().equals("token2")) {
                PlatformFailure failure = new PlatformFailure("recipient2 failed");
                failure.setFailureType(FailureType.MESSAGE_PACKAGE_INVALID);

                recipient.setState(RecipientState.STATE_FAILED);
                recipient.setFailure(failure);
            }
        }

        // Check that the saved values match the updated values
        assertTrue(mMessagesDao.saveMessage(savedMessage));
        List<Message> updatedMessages = mMessagesDao.fetchMessages();
        Message updatedMessage = updatedMessages.get(0);

        assertEquals(2, updatedMessage.getRecipients().size());

        for (Recipient recipient : updatedMessage.getRecipients()) {
            if (recipient.getToken().equals("token1")) {
                assertEquals(RecipientState.STATE_PROCESSING, recipient.getState());
                assertNull(recipient.getPlatformFailure());
            }

            if (recipient.getToken().equals("token2")) {
                assertEquals(RecipientState.STATE_FAILED, recipient.getState());
                assertNotNull(recipient.getPlatformFailure());
            }
        }
    }
}
