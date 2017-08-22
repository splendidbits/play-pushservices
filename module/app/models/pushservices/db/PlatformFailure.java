package models.pushservices.db;

import com.fasterxml.jackson.annotation.JsonIgnore;
import enums.pushservices.Failure;
import io.ebean.Finder;
import io.ebean.Model;

import javax.persistence.*;
import java.util.Date;

/**
 * GNU General Public License v3.0.
 * (This means you can use it as you wish, host and share modifications.)
 * Copyright 4/6/16 Splendid Bits.
 */
@Entity
@Table(name = "recipient_failures", schema = "pushservices")
public class PlatformFailure extends Model implements Cloneable {
    public static Finder<Long, PlatformFailure> find = new Finder<>(PlatformFailure.class);

    @Id
    @JsonIgnore
    @Column(name = "id")
    @SequenceGenerator(name = "failure_id_seq_gen", sequenceName = "failure_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "failure_id_seq_gen")
    public Long id;

    @OneToOne(fetch = FetchType.LAZY)
    public Recipient recipient;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    public Failure failure;

    @Column(name = "message")
    public String failureMessage;

    @Basic
    @Column(name = "fail_time", columnDefinition = "timestamp without time zone")
    @Temporal(TemporalType.TIMESTAMP)
    public Date failTime;

    @PrePersist
    public void prePersist() {
        if (failTime == null) {
            failTime = new Date();
        }
    }

    @SuppressWarnings("unused")
    protected PlatformFailure() {
    }

    public PlatformFailure(String failureMessage) {
        this.failureMessage = failureMessage;
    }

    public PlatformFailure(Failure failure, String failureMessage) {
        this.failure = failure;
        this.failureMessage = failureMessage;
    }

    public PlatformFailure(Failure failure, String failureMessage, Date failTime) {
        this.failure = failure;
        this.failureMessage = failureMessage;
    }

    public boolean isFatalError() {
        if (failure != null) {
            return failure.isFatal;
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PlatformFailure) {
            PlatformFailure other = (PlatformFailure) obj;

            boolean sameType = (failure != null && other.failure != null && failure.equals(other.failure));

            boolean sameFailureMessage = (failureMessage != null && other.failureMessage != null && failureMessage.equals(other.failureMessage));

            // Match everything.
            return sameType && sameFailureMessage;
        }
        return false;
    }

    @Override
    public int hashCode() {
        Long hashCode = 0L;

        hashCode += failure != null
                ? failure.hashCode()
                : hashCode;

        hashCode += failureMessage != null
                ? failureMessage.hashCode()
                : hashCode;

        hashCode += recipient != null
                ? recipient.hashCode()
                : hashCode;

        return hashCode.hashCode();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
