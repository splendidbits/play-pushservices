package models.pushservices.db;

import com.fasterxml.jackson.annotation.JsonIgnore;
import enums.pushservices.RecipientState;
import io.ebean.Finder;
import io.ebean.Model;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "recipients", schema = "pushservices")
public class Recipient extends Model {
    public static Finder<Long, Recipient> find = new Finder<>(Recipient.class);

    @Id
    @JsonIgnore
    @Column(name = "id")
    @SequenceGenerator(name = "gen", sequenceName = "pushservices.recipient_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gen")
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private PlatformFailure failure;

    @Column(name = "token", columnDefinition = "TEXT")
    private String token;

    @ManyToOne
    private Message message;

    @Column(name = "state")
    private RecipientState state;

    @Column(name = "send_attempts")
    private int sendAttemptCount;

    @Basic
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_attempt", columnDefinition = "timestamp without time zone")
    private Date lastSendAttempt;

    @Basic
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "next_attempt", columnDefinition = "timestamp without time zone")
    private Date nextAttempt;

    @PrePersist
    public void prePersist() {
        state = RecipientState.STATE_IDLE;
        sendAttemptCount = 0;
    }

    public Recipient(@Nonnull String token) {
        setToken(token);
    }

    public Long getId() {
        return id;
    }

    public PlatformFailure getPlatformFailure() {
        return failure;
    }

    public void setFailure(PlatformFailure failure) {
        this.failure = failure;
    }

    public String getToken() {
        return token;
    }

    private void setToken(String token) {
        this.token = token;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public RecipientState getState() {
        return state;
    }

    public void setState(RecipientState state) {
        this.state = state;
    }

    public int getSendAttemptCount() {
        return sendAttemptCount;
    }

    public void setSendAttemptCount(int sendAttemptCount) {
        this.sendAttemptCount = sendAttemptCount;
    }

    public Date getLastSendAttempt() {
        return lastSendAttempt;
    }

    public void setLastSendAttempt(Date lastSendAttempt) {
        this.lastSendAttempt = lastSendAttempt;
    }

    public Date getNextAttempt() {
        return nextAttempt;
    }

    public void setNextAttempt(Date nextAttempt) {
        this.nextAttempt = nextAttempt;
    }
}
