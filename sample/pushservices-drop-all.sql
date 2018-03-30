alter table if exists pushservices.messages drop constraint if exists fk_messages_credentials_id;

alter table if exists pushservices.payload_element drop constraint if exists fk_payload_element_message_id;
drop index if exists ix_payload_element_message_id;

alter table if exists pushservices.recipients drop constraint if exists fk_recipients_failure_id;

alter table if exists pushservices.recipients drop constraint if exists fk_recipients_message_id;
drop index if exists ix_recipients_message_id;

drop table if exists pushservices.credentials cascade;
drop sequence if exists pushservices.credentials_id_seq;

drop table if exists pushservices.messages cascade;
drop sequence if exists pushservices.message_id_seq;

drop table if exists pushservices.payload_element cascade;
drop sequence if exists pushservices.element_id_seq;

drop table if exists pushservices.recipient_failures cascade;
drop sequence if exists pushservices.failure_id_seq;

drop table if exists pushservices.recipients cascade;
drop sequence if exists pushservices.recipient_id_seq;

