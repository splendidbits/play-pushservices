package models.pushservices.db;

import com.fasterxml.jackson.annotation.JsonIgnore;
import enums.pushservices.FailureType;
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
public class PlatformFailure extends Model {
    public static Finder<Long, PlatformFailure> find = new Finder<>(PlatformFailure.class);

    @Id
    @JsonIgnore
    @Column(name = "id")
    @SequenceGenerator(name = "gen", sequenceName = "pushservices.failure_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gen")
    private Long id;

    @OneToOne(mappedBy = "failure")
    private Recipient recipient;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private FailureType failureType;

    @Column(name = "message")
    private String failureMessage;

    @Basic
    @Column(name = "fail_time", columnDefinition = "timestamp without time zone")
    @Temporal(TemporalType.TIMESTAMP)
    private Date failTime;

    public PlatformFailure(String failureMessage) {
        setFailureMessage(failureMessage);
    }

    public PlatformFailure(FailureType failureType, String failureMessage) {
        setFailureType(failureType);
        setFailureMessage(failureMessage);
    }

    public PlatformFailure(FailureType failureType, String failureMessage, Date failTime) {
        setFailureType(failureType);
        setFailureMessage(failureMessage);
        setFailTime(failTime);
    }

    @PrePersist
    public void prePersist() {
        if (failTime == null) {
            setFailTime(new Date());
        }
    }

    public Long getId() {
        return id;
    }

    public Recipient getRecipient() {
        return recipient;
    }

    public void setRecipient(Recipient recipient) {
        this.recipient = recipient;
    }

    public FailureType getFailureType() {
        return failureType;
    }

    public void setFailureType(FailureType failureType) {
        this.failureType = failureType;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
    }

    public Date getFailTime() {
        return failTime;
    }

    public void setFailTime(Date failTime) {
        this.failTime = failTime;
    }
}
