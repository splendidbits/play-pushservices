create table pushservices.credentials (
  id                            bigint not null,
  platform                      varchar(4),
  authorisation_key             TEXT,
  certificate_body              TEXT,
  package_uri                   TEXT,
  constraint ck_credentials_platform check ( platform in ('GCM','APNS')),
  constraint pk_credentials primary key (id)
);
create sequence pushservices.credentials_id_seq increment by 1;

create table pushservices.messages (
  id                            bigint not null,
  credentials_id                bigint,
  collapse_key                  varchar(255),
  priority                      varchar(6),
  ttl_seconds                   integer not null,
  delay_while_idle              boolean default false not null,
  dry_run                       boolean default false not null,
  maximum_retries               integer not null,
  added_time                    timestamp without time zone,
  constraint ck_messages_priority check ( priority in ('normal','low','high')),
  constraint uq_messages_credentials_id unique (credentials_id),
  constraint pk_messages primary key (id)
);
create sequence pushservices.message_id_seq increment by 1;

create table pushservices.payload_element (
  id                            bigint not null,
  element_name                  varchar(255),
  element_value                 TEXT,
  message_id                    bigint,
  constraint pk_payload_element primary key (id)
);
create sequence pushservices.element_id_seq increment by 1;

create table pushservices.recipient_failures (
  id                            bigint not null,
  type                          varchar(30),
  message                       varchar(255),
  fail_time                     timestamp without time zone,
  constraint ck_recipient_failures_type check ( type in ('TEMPORARILY_UNAVAILABLE','PLATFORM_LIMIT_EXCEEDED','MESSAGE_PAYLOAD_INVALID','MESSAGE_TTL_INVALID','MESSAGE_PACKAGE_INVALID','RECIPIENT_RATE_EXCEEDED','PLATFORM_AUTH_MISMATCHED','RECIPIENT_REGISTRATION_INVALID','MESSAGE_TOO_LARGE','RECIPIENT_NOT_REGISTERED','PLATFORM_AUTH_INVALID','MESSAGE_REGISTRATIONS_MISSING','ERROR_UNKNOWN')),
  constraint pk_recipient_failures primary key (id)
);
create sequence pushservices.failure_id_seq increment by 1;

create table pushservices.recipients (
  id                            bigint not null,
  failure_id                    bigint,
  token                         TEXT,
  message_id                    bigint,
  state                         varchar(13),
  send_attempts                 integer not null,
  last_attempt                  timestamp without time zone,
  next_attempt                  timestamp without time zone,
  constraint ck_recipients_state check ( state in ('WAITING_RETRY','COMPLETE','FAILED','IDLE','PROCESSING')),
  constraint uq_recipients_failure_id unique (failure_id),
  constraint pk_recipients primary key (id)
);
create sequence pushservices.recipient_id_seq increment by 1;

alter table pushservices.messages add constraint fk_messages_credentials_id foreign key (credentials_id) references pushservices.credentials (id) on delete restrict on update restrict;

alter table pushservices.payload_element add constraint fk_payload_element_message_id foreign key (message_id) references pushservices.messages (id) on delete restrict on update restrict;
create index ix_payload_element_message_id on pushservices.payload_element (message_id);

alter table pushservices.recipients add constraint fk_recipients_failure_id foreign key (failure_id) references pushservices.recipient_failures (id) on delete restrict on update restrict;

alter table pushservices.recipients add constraint fk_recipients_message_id foreign key (message_id) references pushservices.messages (id) on delete restrict on update restrict;
create index ix_recipients_message_id on pushservices.recipients (message_id);

