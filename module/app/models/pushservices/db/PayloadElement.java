package models.pushservices.db;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.ebean.Finder;
import io.ebean.Model;

import javax.annotation.Nonnull;
import javax.persistence.*;

@Entity
@Table(name = "payload_element", schema = "pushservices")
public class PayloadElement extends Model implements Cloneable {
    public static Finder<Long, PayloadElement> find = new Finder<>(PayloadElement.class);

    @Id
    @JsonIgnore
    @Column(name = "id")
    @SequenceGenerator(name = "element_id_seq_gen", sequenceName = "element_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "element_id_seq_gen")
    public Long id;

    @Column(name = "element_name")
    public String key;

    @Column(name = "element_value", columnDefinition = "TEXT")
    public String value;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "message_id",
            table = "pushservices.messages",
            referencedColumnName = "id")
    public Message message;

    @SuppressWarnings("unused")
    public PayloadElement() {
    }

    public PayloadElement(@Nonnull String name, @Nonnull String value) {
        this.key = name;
        this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PayloadElement) {
            PayloadElement other = (PayloadElement) obj;

            boolean sameName = (key != null && other.key != null && key.equals(other.key));

            boolean sameValue = (value != null && other.value != null && value.equals(other.value));

            // Match everything.
            return sameName && sameValue;
        }
        return false;
    }

    @Override
    public int hashCode() {
        Long hashCode = 0L;

        hashCode += key != null
                ? key.hashCode()
                : hashCode;

        hashCode += value != null
                ? value.hashCode()
                : hashCode;

        return hashCode.hashCode();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}