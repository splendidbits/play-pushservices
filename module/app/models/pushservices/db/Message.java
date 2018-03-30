package models.pushservices.db;

import enums.pushservices.MessagePriority;
import io.ebean.Finder;
import io.ebean.Model;
import io.ebean.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "messages", schema = "pushservices")
public class Message extends Model {
    public static Finder<Long, Message> find = new Finder<>(Message.class);
    private static final int TTL_SECONDS_DEFAULT = 60 * 60 * 24 * 7;

    @Id
    @JsonIgnore
    @Column(name = "id")
    @SequenceGenerator(name = "gen", sequenceName = "pushservices.message_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gen")
    private Long id;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Recipient> recipients;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "credentials_id")
    private Credentials credentials;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<PayloadElement> payloadData;

    @Column(name = "collapse_key")
    private String collapseKey;

    @Column(name = "priority")
    @Enumerated(EnumType.STRING)
    private MessagePriority messagePriority = MessagePriority.PRIORITY_NORMAL;

    @Column(name = "ttl_seconds")
    private int ttlSeconds = TTL_SECONDS_DEFAULT;

    @Column(name = "delay_while_idle")
    private boolean shouldDelayWhileIdle;

    @Column(name = "dry_run")
    private boolean dryRun;

    @Column(name = "maximum_retries")
    private int maximumRetries = 10;

    @Basic
    @Column(name = "added_time", columnDefinition = "timestamp without time zone")
    @Temporal(TemporalType.TIMESTAMP)
    private Date addedTime;

    @PrePersist
    public void updatedTime() {
        setAddedTime(new Date());
    }

    public Long getId() {
        return id;
    }

    public List<Recipient> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<Recipient> recipients) {
        this.recipients = recipients;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    public List<PayloadElement> getPayloadData() {
        return payloadData;
    }

    public void setPayloadData(List<PayloadElement> payloadData) {
        this.payloadData = payloadData;
    }

    public String getCollapseKey() {
        return collapseKey;
    }

    public void setCollapseKey(String collapseKey) {
        this.collapseKey = collapseKey;
    }

    public MessagePriority getMessagePriority() {
        return messagePriority;
    }

    public void setMessagePriority(MessagePriority messagePriority) {
        this.messagePriority = messagePriority;
    }

    public int getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(int ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public boolean isShouldDelayWhileIdle() {
        return shouldDelayWhileIdle;
    }

    public void setShouldDelayWhileIdle(boolean shouldDelayWhileIdle) {
        this.shouldDelayWhileIdle = shouldDelayWhileIdle;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public int getMaximumRetries() {
        return maximumRetries;
    }

    public void setMaximumRetries(int maximumRetries) {
        this.maximumRetries = maximumRetries;
    }

    public Date getAddedTime() {
        return addedTime;
    }

    private void setAddedTime(Date addedTime) {
        this.addedTime = addedTime;
    }
}
