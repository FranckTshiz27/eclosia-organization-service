package eclosia.eclosia_organization_service.subject.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import eclosia.eclosia_organization_service.country.entity.Country;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(
        name = "subjects",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_country_subject_code",
                        columnNames = {"country_id", "code"}
                )
        }
)
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 20)
    private String abbreviation;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_subject_id")
    private Subject parentSubject;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 1;

    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("countryId")
    public UUID getCountryId() {
        return country != null ? country.getId() : null;
    }

    @JsonProperty("parentSubjectId")
    public UUID getParentSubjectId() {
        return parentSubject != null ? parentSubject.getId() : null;
    }
}
