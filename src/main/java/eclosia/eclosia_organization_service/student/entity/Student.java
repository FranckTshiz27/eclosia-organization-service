package eclosia.eclosia_organization_service.student.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import eclosia.eclosia_organization_service.city.entity.City;
import eclosia.eclosia_organization_service.commune.entity.Commune;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(
        name = "students",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_student_number",
                        columnNames = "student_number"
                )
        }
)
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Matricule permanent.
     * Peut etre genere automatiquement ou saisi
     * manuellement selon les parametres de l'ecole.
     */
    @Column(name = "student_number", length = 50)
    private String studentNumber;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "middle_name", length = 100)
    private String middleName;

    /**
     * MALE
     * FEMALE
     */
    @Column(nullable = false, length = 30)
    private String gender;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    /**
     * Lieu de naissance
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "birth_country_id", nullable = false)
    private Country birthCountry;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "birth_city_id")
    private City birthCity;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "birth_commune_id")
    private Commune birthCommune;

    /**
     * Nationalite (libre de saisie)
     */
    @Column(length = 150)
    private String nationality;

    /**
     * Adresse de residence
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id")
    private Country country;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    private City city;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commune_id")
    private Commune commune;

    @Column(length = 150)
    private String quarter;

    @Column(length = 150)
    private String avenue;

    @Column(length = 30)
    private String number;

    @Column(length = 30)
    private String phoneNumber;

    @Column(length = 150)
    private String email;

    @Column(length = 500)
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("birthCountryId")
    public UUID getBirthCountryId() {
        return birthCountry != null ? birthCountry.getId() : null;
    }

    @JsonProperty("birthCityId")
    public UUID getBirthCityId() {
        return birthCity != null ? birthCity.getId() : null;
    }

    @JsonProperty("birthCommuneId")
    public UUID getBirthCommuneId() {
        return birthCommune != null ? birthCommune.getId() : null;
    }

    @JsonProperty("countryId")
    public UUID getCountryId() {
        return country != null ? country.getId() : null;
    }

    @JsonProperty("cityId")
    public UUID getCityId() {
        return city != null ? city.getId() : null;
    }

    @JsonProperty("communeId")
    public UUID getCommuneId() {
        return commune != null ? commune.getId() : null;
    }
}
