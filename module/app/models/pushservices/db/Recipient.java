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
public class Recipient extends Model implements Cloneable {
    public static Finder<Long, Recipient> find = new Finder<>(Recipient.class);

    @Id
    @JsonIgnore
    @Column(name = "id")
    @SequenceGenerator(name = "recipient_id_seq_gen", sequenceName = "recipient_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "recipient_id_seq_gen")
    public Long id;

    @Column(name = "token", columnDefinition = "TEXT")
    public String token;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(
            name = "message_id",
            table = "pushservices.messages",
            referencedColumnName = "id",
            unique = false,
            updatable = true)
    public Message message;

    @Column(name = "state")
    public RecipientState state = RecipientState.STATE_IDLE;

    @Basic
    @Column(name = "time_added", columnDefinition = "timestamp without time zone")
    @Temporal(TemporalType.TIMESTAMP)
    public Date timeAdded;

    @Column(name = "send_attempts")
    public int sendAttemptCount = 1;

    @OneToOne(mappedBy = "recipient", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    public PlatformFailure failure;

    @Basic
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "previous_attempt", columnDefinition = "timestamp without time zone")
    public Date previousAttempt;

    @Basic
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "next_attempt", columnDefinition = "timestamp without time zone")
    public Date nextAttempt;

    @PrePersist
    public void prePersist() {
        timeAdded = new Date();
        previousAttempt = new Date();
    }

    @PreUpdate
    public void preUpdate() {
        previousAttempt = new Date();
    }

    @SuppressWarnings("unused")
    public Recipient() {
    }

    public Recipient(@Nonnull String token) {
        this.token = token;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Recipient) {
            Recipient other = (Recipient) obj;

            boolean sameToken = (token != null && other.token != null && token.equals(other.token));

            boolean sameState = (state != null && other.state != null && state.equals(other.state));

            boolean sameFailure = (failure == null && other.failure == null ||
                    (failure != null && other.failure != null && failure.equals(other.failure)));

            // Match everything.
            return sameToken && sameState && sameFailure;
        }
        return false;
    }

    @Override
    public int hashCode() {
        Long hashCode = 0L;

        hashCode += token != null
                ? token.hashCode()
                : hashCode;

        hashCode += state != null
                ? state.hashCode()
                : hashCode;

        hashCode += failure != null
                ? failure.hashCode()
                : hashCode;

        return hashCode.hashCode();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
