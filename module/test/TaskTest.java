import dao.pushservices.TasksDao;
import enums.pushservices.MessagePriority;
import enums.pushservices.PlatformType;
import exceptions.pushservices.TaskValidationException;
import helpers.pushservices.MessageBuilder;
import models.pushservices.db.Credentials;
import models.pushservices.db.Message;
import models.pushservices.db.Recipient;
import models.pushservices.db.Task;
import org.junit.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test core functions of the Device Data Access Layer.
 */
public class TaskTest extends PushServicesApplicationTest {
    private static TasksDao mTasksDao;

    @BeforeClass
    public static void initialise() {
        mTasksDao = application.injector().instanceOf(TasksDao.class);
    }

    @Before
    public void beforeTest() {
    }

    @After
    public void afterTest() {
    }

    @Test
    public void testMessageBuilder() throws TaskValidationException {
        HashSet<String> tokens = new HashSet<>();
        tokens.add("test_token");

        Credentials credentials = new Credentials();
        credentials.authKey = "test_auth_key";
        credentials.platformType = PlatformType.SERVICE_GCM;

        Message message = new MessageBuilder.Builder()
                .setPlatformCredentials(credentials)
                .setMessagePriority(MessagePriority.PRIORITY_HIGH)
                .setCollapseKey("collapse")
                .setDeviceTokens(tokens)
                .setIsDryRun(true)
                .setShouldDelayWhileIdle(false)
                .setTimeToLiveSeconds(3600)
                .putData("test_key_1", "test_data_1")
                .putData("test_key_2", "test_data_2")
                .putData("test_key_3", "test_data_3")
                .build();

        assertNotNull(message);
        assertNotNull(message.recipients);
        assertNotNull(message.payloadData);
        assertNotNull(message.credentials);

        assertFalse(message.recipients.isEmpty());
        assertFalse(message.payloadData.isEmpty());

        assertEquals(message.credentials.authKey, "test_auth_key");
        assertEquals(message.ttlSeconds, 3600);
        assertTrue(message.isDryRun);
        assertFalse(message.shouldDelayWhileIdle);
    }

    @Test
    public void testDatabaseTaskInsert() {
        Recipient recipient1 = new Recipient();
        recipient1.token = "token1";

        Recipient recipient2 = new Recipient();
        recipient2.token = "token2";

        Message message1 = new Message();
        message1.addRecipient(recipient1);
        message1.addRecipient(recipient2);

        Message message2 = new Message();
        message2.addRecipient(recipient1);
        message2.addRecipient(recipient2);

        List<Message> messageList = new ArrayList<>();
        messageList.add(message1);
        messageList.add(message2);

        Task task = new Task("test");
        task.addedTime = new Date();
        task.priority = Task.TASK_PRIORITY_HIGH;
        task.messages = messageList;
        assertTrue(mTasksDao.saveTask(task));

        List<Task> savedTasks = mTasksDao.findTasks("test");
        assertNotNull(savedTasks);
        for (Task fetchedTask : savedTasks) {
            assertNotNull(fetchedTask.messages);
            assertFalse(fetchedTask.messages.isEmpty());

            assertTrue(mTasksDao.deleteTask(fetchedTask.id));
        }
    }
}
