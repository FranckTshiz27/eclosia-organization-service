package eclosia.eclosia_organization_service.state.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Data
@Table(name = "states")
public class State {

    @Id
    private Long id;

    private String name;

    @Column(name = "country_id")
    private Long countryId;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "country_name")
    private String countryName;

    @Column(length = 20)
    private String iso2;

    @Column(name = "iso3166_2", length = 20)
    private String iso3166_2;

    @Column(name = "fips_code", length = 20)
    private String fipsCode;

    @Column(length = 100)
    private String type;

    @Column(length = 50)
    private String level;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "native")
    private String nativeName;

    @Column(precision = 12, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 12, scale = 8)
    private BigDecimal longitude;

    private String timezone;

    @Column(name = "wiki_data_id", length = 50)
    private String wikiDataId;

    private Long population;
}
