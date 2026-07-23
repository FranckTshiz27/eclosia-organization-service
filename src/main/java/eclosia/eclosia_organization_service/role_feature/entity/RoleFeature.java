package eclosia.eclosia_organization_service.role_feature.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import eclosia.eclosia_organization_service.feature.entity.Feature;
import eclosia.eclosia_organization_service.role.entity.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(
        name = "security_role_feature",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_role_feature",
                        columnNames = {
                                "role_id",
                                "feature_id"
                        }
                )
        }
)
public class RoleFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "role_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_role_feature_role")
    )
    private Role role;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "feature_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_role_feature_feature")
    )
    private Feature feature;

    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("roleId")
    public UUID getRoleId() {
        return role != null ? role.getId() : null;
    }

    @JsonProperty("featureId")
    public UUID getFeatureId() {
        return feature != null ? feature.getId() : null;
    }
}
