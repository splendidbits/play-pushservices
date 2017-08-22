package models.pushservices.db;

import io.ebean.Finder;
import io.ebean.Model;
import io.ebean.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "tasks", schema = "pushservices")
public class Task extends Model implements Cloneable {
    public static Finder<Long, Task> find = new Finder<>(Task.class);

    // While priority can be set to any int, these might be useful. Bigger int is higher priority.
    public static final int TASK_PRIORITY_HIGH = 10;
    public static final int TASK_PRIORITY_MEDIUM = 5;
    public static final int TASK_PRIORITY_LOW = 1;

    @Id
    @JsonIgnore
    @Column(name = "id")
    @SequenceGenerator(name = "task_id_seq_gen", sequenceName = "task_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "task_id_seq_gen")
    public Long id;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    public List<Message> messages;

    @Column(name = "name")
    public String name;

    @Column(name = "priority")
    public int priority = TASK_PRIORITY_LOW;

    @Basic
    @Column(name = "added_time", columnDefinition = "timestamp without time zone")
    @Temporal(TemporalType.TIMESTAMP)
    public Date addedTime;

    @PrePersist
    public void prePersist() {
        addedTime = new Date();
    }

    public Task(String name) {
        this.messages = new ArrayList<>();
        this.name = name;
    }

    protected Task() {

    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Task) {
            Task other = (Task) obj;

            boolean sameTaskName = name != null && other.name != null && name.equals(other.name);

            boolean samePriority = priority == other.priority;

            boolean bothMessagesEmpty = messages == null && other.messages == null ||
                    (messages != null && messages.isEmpty() && other.messages != null && other.messages.isEmpty());

            boolean sameMessages = bothMessagesEmpty || (messages != null && other.messages != null &&
                    messages.containsAll(other.messages) && other.messages.containsAll(messages));

            return sameTaskName && samePriority && sameMessages;
        }
        return false;
    }

    @Override
    public int hashCode() {
        Long hashCode = 0L;

        hashCode += name != null
                ? name.hashCode()
                : hashCode;

        hashCode += priority;

        hashCode += messages != null
                ? messages.hashCode()
                : hashCode;

        return hashCode.hashCode();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}