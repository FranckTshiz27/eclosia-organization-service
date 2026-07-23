package eclosia.eclosia_organization_service.feature.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import eclosia.eclosia_organization_service.feature.enums.FeatureAction;
import eclosia.eclosia_organization_service.security_module.entity.SecurityModule;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
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
        name = "security_feature",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_feature_module_action",
                        columnNames = {
                                "module_id",
                                "action"
                        }
                )
        }
)
public class Feature {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "module_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_feature_module")
    )
    private SecurityModule module;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FeatureAction action;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "system_feature", nullable = false)
    private Boolean systemFeature = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    @JsonProperty("code")
    public String getCode() {
        if (module == null || module.getCode() == null || action == null) {
            return null;
        }
        return module.getCode().toLowerCase() + "." + action.name().toLowerCase();
    }

    @JsonProperty("moduleId")
    public UUID getModuleId() {
        return module != null ? module.getId() : null;
    }

    @JsonProperty("moduleCode")
    public String getModuleCode() {
        return module != null ? module.getCode() : null;
    }

    @JsonProperty("moduleName")
    public String getModuleName() {
        return module != null ? module.getName() : null;
    }
}
