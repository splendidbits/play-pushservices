## Push Notifications module for Play Framework

Be sure to read the [Release Notes](VERSIONS.md).



A Play Framework Java module that makes it super simple to add **GCM** or (*soon*) **APNS** Push Notifications to your project. It has been built in a way that makes adding other push notification services a breeze.

This has been used in heavy deployment, pushing thousands of transit route alerts per day to [SEPTA Instant](https://splendidbits.co/septainstant) for Android.



## Features

* **Message Building.** An extensible message builder that makes it easy to add data and recipients for a particular message.
* **Simple Asyncronous Callbacks.** Know exactly when each message has failed, partially failed, or completed with one or more detailed recipient failures.
* **Safe Session Restoration.** Don't worry about an unexpected reboot or crashe in the middle of sending a batch of important push messages. It can be configured to use any datastore that Ebean supports, and as it keeps track of the state and data of each recipient, it'll will resume where it left off, retrying earlier messages.
* **Push Provider Abstraction.** Easy interfaces and data-models mean that you're not concerned with individual Push Provider (APNS or GCM) nuances.
* **Task Queing.** A task system that keeps track of the delivery status of each message for every notification recipient. The Task Queues mitigate a self denial-of-service for heavy notification delivery periods. 
* **Batching.** The module intelligently decides if you have more recipients for a message than a provider supports, and internally handles batching the message, and rejoining the responses, so you don't need to worry about message recipient limitations.
* **And More!** Review the code, or read the usage section below, and get started today within minutes.




## Usage

This repository contains a sample Play Framework that gives you an idea how easy it is to add Push Notifications to your Play project. These examples are for [Google Cloud Messaging](https://firebase.google.com/docs/cloud-messaging/).

The example below demonstrates sending late train alerts for a particular London Underground line, but you can use it to send any kind of notification. 



**1:** Create credentials for your push provider. 

```java
Credentials googleCredentials = new Credentials(PlatformType.SERVICE_GCM);
googleCredentials.packageUri = "com.company.app";
googleCredentials.authKey = "AEFbawuefAWEFwaEFea9OAKFAEWfeawKk";
```

**2:** Add some sample data into key / value map.
```java
Map<String, String> messageData = new HashMap<>();
messageData.put("route", "Piccadilly Line");
messageData.put("direction", "Northbound");
messageData.put("destination", "Cockfosters");
messageData.put("minutes_late", "3");
messageData.put("train_id", "2342");
```

**3:** Build the notification message with `MessageBuilder`.
```java
Message lateTrain = new MessageBuilder.Builder()
        .setCollapseKey("piccadilly_line")
        .setPlatformCredentials(googleCredentials)
        .addDeviceToken("APA91bFcidzSlRTlyijOP04UCR8KXHfxi4j2VHfLv9TcE14QwjckJ3qB4gm69zbCjRygt")
        .addDeviceToken("7RdKURjhqgaegbJDYTIAJTYRSshDFas6jili1DUTtUDYJv00rBLTBf0nDsO4fEl1Fjua")
        .setData(messageData);
        .build();
```

**4:** Add one or more messages to a task, and queue it for dispatch.
```java
@Inject TaskQueue taskQueue;

List<Message> messages = Arrays.asList(lateTrain, secondLateTrain, ...);
taskQueue.queueMessages(messages, null);
```


(Optionially, listen for provider updates such as token updates, deliver failures, etc).

```java
taskQueue.queueMessages(messages, new TaskQueueListener() {
	@Override
	public void updatedRecipients(@Nonnull List<UpdatedRecipient> updatedeRecipients) {
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
```



## Installation

##### `build.sbt`

Add the jCenter repository and dependency to your `build.sbt` file.

```bash
resolvers += "Bintray jCenter repository" at "https://jcenter.bintray.com"
libraryDependencies += "com.splendidbits" % "play-pushservices" % "1.2"
```



##### `application.conf`

Add the pushservices module hook to your Play application.  The simple way is by appending the`PushServicesModule` to the list of modules enabled in application.conf.

```
play.modules.enabled += "injection.pushservices.modules.PushServicesModule"
```

Another approach for those that need to enable to disable Guice modules under certain conditions such as tests, add the module to a GuiceApplicationLoader. You can see an example of this in the sample app.

```
public class YourApplicationClassLoader extends GuiceApplicationLoader {
    @Override
    public GuiceApplicationBuilder builder(ApplicationLoader.Context context) {
        return initialBuilder
        	.in(context.environment())
        	.bindings(new PushServicesModule());
    }
}
```

Add the module and database properties to your `application.conf ` file. Change the database name, platform name, and database credentials as you see fit, but leave the attribute prefix as *pushservices*.

```bash
# PushServices module database configuration.
pushservices.driver=org.postgresql.Driver
pushservices.name="my_app"
pushservices.url="jdbc:postgresql://localhost:5432/my_app_db"
pushservices.username="sampleuser"
pushservices.password="samplepass"
pushservices.databasePlatformName="postgres"
```

(You may also add any number of other support EBean ServerConfig properties. See the [ebean documentation](http://ebean-orm.github.io/docs/configuration/serverconfig) for more information).

##### `sample/pushservices-create-all.sql` 

Import the pushservices database schema found in the sample project. You may change the database name and credentials as you see fit, but do not alter the schema or table names. 



## Requirements

* Guice dependency injection.
* An ebean compatible database or datastore.

The module has been tested on the following versions of Play Framework, but should be backwards compatible. Please update the table below with your own findings.

| Play Version | Compatible |
| :----------: | :--------: |
|    2.6.x     |     Y      |
|    2.5.x     |     Y      |
|    2.4.x     |     ?      |



## Future features

* **APNS.** (Apple Push Notification Service) support.
* **WNS.** Windows Notification Service.
* **Scala** API support.
* **Statistics** A nice web dashboard.




## Contributions

The PushServices module is distributed under the GNU General Public License v3.0. (This means you may use it commercially or non-commercially, but must share improvements ).



Happy messaging!

Created by [Daniel Watson](https://twitter.com/iamprobablylost)
