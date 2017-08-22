package models.pushservices.db;

import com.fasterxml.jackson.annotation.JsonIgnore;
import enums.pushservices.PlatformType;
import io.ebean.Finder;
import io.ebean.Model;

import javax.persistence.*;

@Entity
@Table(name = "credentials", schema = "pushservices")
public class Credentials extends Model implements Cloneable {
    public static Finder<Long, Credentials> find = new Finder<>(Credentials.class);

    @Id
    @JsonIgnore
    @Column(name = "id")
    @SequenceGenerator(name = "credentials_id_seq_gen", sequenceName = "credentials_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "credentials_id_seq_gen")
    public Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "message_id",
            table = "pushservices.messages",
            referencedColumnName = "id")
    public Message message;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform")
    public PlatformType platformType;

    @Column(name = "authorisation_key", columnDefinition = "TEXT")
    public String authKey;

    @Column(name = "certificate_body", columnDefinition = "TEXT")
    public String certBody;

    @Column(name = "package_uri", columnDefinition = "TEXT")
    public String packageUri;

    @SuppressWarnings("unused")
    public Credentials() {
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Credentials) {
            Credentials other = (Credentials) obj;

            boolean samePlatform = (platformType != null && other.platformType != null && platformType.equals(other.platformType));

            boolean sameAuthorisationKey = (authKey != null && other.authKey != null && authKey.equals(other.authKey));

            boolean sameCertificateBody = (certBody != null && other.certBody != null && certBody.equals(other.certBody));

            boolean samePackageUri = (packageUri != null && other.packageUri != null && packageUri.equals(other.packageUri));

            // Match everything.
            return samePlatform && samePackageUri && (sameAuthorisationKey || sameCertificateBody);
        }
        return false;
    }

    @Override
    public int hashCode() {
        Long hashCode = 0L;

        hashCode += platformType != null
                ? platformType.hashCode()
                : hashCode;

        hashCode += authKey != null
                ? authKey.hashCode()
                : hashCode;

        hashCode += certBody != null
                ? certBody.hashCode()
                : hashCode;

        hashCode += packageUri != null
                ? packageUri.hashCode()
                : hashCode;

        return hashCode.hashCode();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
