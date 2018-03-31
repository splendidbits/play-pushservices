Push Notification Services module for Play Framework
-------------------------

#### 1.2.1 - 2018.03.31
- Persistence and TaskQueue provider RATE_EXCEEDED fixes.
- Fix multiple callbacks to interface.

#### 1.2 - 2018.03.29
- **IMPORTANT:** The backing persistence schemas have changed. Remove any existing *pushservices* database, and create afresh using the database creation sql scripts found in the sample project. 
- Remove the idea of "Task" containers. It was overly complicated and had limited use. Message is now the top level of heirarchy.
- Fix many issues with ebean persistence and make it generally way more reliable. 
- Make Messages and children completely atomic in the ebean and TaskQueue lifecycle.
- Fix issues with retrying recipients under some conditions. Make the TaskQueue a lot more efficient.
- Improve testing.
- Updated sample project. 


#### 1.1.2 - 2018.03 24
- EBean insertion amd update cascade fixes.
- Thread syncronization improvements.


#### 1.1 - 2018.03.23
- Support all ebean server and datasource config flags in custom application.conf properties.
- Modify DAO to replace deprecated ebean method calls.
- Fix EBean provider lazyloading instances.
- Update dependencies (Play, ebean, Gson).
- Improvements to the lifecycle manager to more gracefully startup and shutdown TaskQueue and datastore.
- Miscellaneous bugfixes. 


#### 1.0.2 - 2017.08.23
 - Improvements to default database configuration


#### 1.0.1 - 2017.08.22
 - Sample application and documentation


#### 1.0 - 2017.08.21
 - Initial release