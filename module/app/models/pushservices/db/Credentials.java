package models.pushservices.db;

import com.fasterxml.jackson.annotation.JsonIgnore;
import enums.pushservices.PlatformType;
import io.ebean.Finder;
import io.ebean.Model;

import javax.persistence.*;

@Entity
@Table(name = "credentials", schema = "pushservices")
public class Credentials extends Model {
    public static Finder<Long, Credentials> find = new Finder<>(Credentials.class);

    @Id
    @JsonIgnore
    @Column(name = "id")
    @SequenceGenerator(name = "gen", sequenceName = "pushservices.credentials_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gen")
    protected Long id;

    @OneToOne(mappedBy = "credentials")
    private Message message;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform")
    private PlatformType platformType;

    @Column(name = "authorisation_key", columnDefinition = "TEXT")
    private String authKey;

    @Column(name = "certificate_body", columnDefinition = "TEXT")
    private String certBody;

    @Column(name = "package_uri", columnDefinition = "TEXT")
    private String packageUri;

    public Credentials(PlatformType platformType) {
        setPlatformType(platformType);
    }

    public Long getId() {
        return id;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public PlatformType getPlatformType() {
        return platformType;
    }

    private void setPlatformType(PlatformType platformType) {
        this.platformType = platformType;
    }

    public String getAuthKey() {
        return authKey;
    }

    public void setAuthKey(String authKey) {
        this.authKey = authKey;
    }

    public String getCertBody() {
        return certBody;
    }

    public void setCertBody(String certBody) {
        this.certBody = certBody;
    }

    public String getPackageUri() {
        return packageUri;
    }

    public void setPackageUri(String packageUri) {
        this.packageUri = packageUri;
    }
}
