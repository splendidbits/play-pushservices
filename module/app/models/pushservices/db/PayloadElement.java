package models.pushservices.db;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.ebean.Finder;
import io.ebean.Model;

import javax.annotation.Nonnull;
import javax.persistence.*;

@Entity
@Table(name = "payload_element", schema = "pushservices")
public class PayloadElement extends Model {
    public static Finder<Long, PayloadElement> find = new Finder<>(PayloadElement.class);

    @Id
    @JsonIgnore
    @Column(name = "id")
    @SequenceGenerator(name = "gen", sequenceName = "pushservices.element_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gen")
    private Long id;

    @Column(name = "element_name")
    private String key;

    @Column(name = "element_value", columnDefinition = "TEXT")
    private String value;

    @ManyToOne
    private Message message;

    public PayloadElement(@Nonnull String name, @Nonnull String value) {
        setKey(name);
        setValue(value);
    }

    public Long getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }
}