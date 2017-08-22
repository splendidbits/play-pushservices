package models.pushservices.db;

import enums.pushservices.MessagePriority;
import io.ebean.Finder;
import io.ebean.Model;
import io.ebean.annotation.JsonIgnore;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "messages", schema = "pushservices")
public class Message extends Model implements Cloneable {
    public static Finder<Long, Message> find = new Finder<>(Message.class);
    private static final int TTL_SECONDS_DEFAULT = 60 * 60 * 24 * 7;

    @Id
    @JsonIgnore
    @Column(name = "id")
    @SequenceGenerator(name = "message_id_seq_gen", sequenceName = "message_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "message_id_seq_gen")
    public Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(
            name = "task_id",
            table = "pushservices.tasks",
            referencedColumnName = "id")
    public Task task;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    public List<Recipient> recipients;

    @OneToOne(mappedBy = "message", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    public Credentials credentials;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    public List<PayloadElement> payloadData;

    @Column(name = "collapse_key")
    public String collapseKey;

    @Column(name = "priority")
    @Enumerated(EnumType.STRING)
    public MessagePriority messagePriority = MessagePriority.PRIORITY_NORMAL;

    @Column(name = "ttl_seconds")
    public int ttlSeconds = TTL_SECONDS_DEFAULT;

    @Column(name = "delay_while_idle")
    public boolean shouldDelayWhileIdle;

    @Column(name = "dry_run")
    public boolean isDryRun;

    @Column(name = "maximum_retries")
    public int maximumRetries = 10;

    @Basic
    @Column(name = "sent_time", columnDefinition = "timestamp without time zone")
    @Temporal(TemporalType.TIMESTAMP)
    public Date sentTime;

    @Transient
    public void addRecipient(@Nonnull Recipient recipient) {
        if (recipients == null) {
            recipients = new ArrayList<>();
        }
        recipients.add(recipient);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Message) {
            Message other = (Message) obj;

            boolean sameCollapseKey = (collapseKey != null && other.collapseKey != null && collapseKey.equals(other.collapseKey));

            boolean sameCredentials = (credentials != null && other.credentials != null && credentials.equals(other.credentials));

            boolean samePriority = (messagePriority != null && other.messagePriority != null && messagePriority.equals(other.messagePriority));

            boolean sameTtl = ttlSeconds == other.ttlSeconds;

            boolean sameDelayWhileIdle = shouldDelayWhileIdle == other.shouldDelayWhileIdle;

            boolean sameDryRun = isDryRun == other.isDryRun;

            boolean bothPayloadsEmpty = payloadData == null && other.payloadData == null ||
                    (payloadData != null && payloadData.isEmpty() && other.payloadData != null && other.payloadData.isEmpty());

            boolean samePayloadData = bothPayloadsEmpty || (payloadData != null && other.payloadData != null && other.payloadData.containsAll(payloadData));

            boolean bothRecipientsEmpty = (recipients == null && other.recipients == null) ||
                    ((recipients != null && recipients.isEmpty()) && other.recipients != null && other.recipients.isEmpty());

            boolean sameRecipients = bothRecipientsEmpty || (recipients != null && other.recipients != null &&
                    recipients.containsAll(other.recipients) && other.recipients.containsAll(recipients));

            // Match everything.
            return sameCollapseKey && sameCredentials && samePriority && sameTtl &&
                    sameDelayWhileIdle && sameDryRun && samePayloadData && sameRecipients;
        }
        return false;
    }

    @Override
    public int hashCode() {
        Long hashCode = 0L;

        hashCode += collapseKey != null
                ? collapseKey.hashCode()
                : hashCode;

        hashCode += credentials != null
                ? credentials.hashCode()
                : hashCode;

        hashCode += messagePriority != null
                ? messagePriority.hashCode()
                : hashCode;

        hashCode += ttlSeconds;

        hashCode += shouldDelayWhileIdle ? 1 : 0;

        hashCode += isDryRun ? 1 : 0;

        hashCode += payloadData != null
                ? payloadData.hashCode()
                : hashCode;

         hashCode += recipients != null
                ? recipients.hashCode()
                : hashCode;

        return hashCode.hashCode();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
