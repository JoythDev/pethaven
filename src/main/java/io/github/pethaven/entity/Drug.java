package io.github.pethaven.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "drugs")
@Entity
public class Drug {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 120, unique = true, nullable = false)
    private String name;

    @Column(name = "purchase_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "sale_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "units_available", nullable = false)
    private Integer unitsAvailable;

    @Column(name = "units_sold", nullable = false)
    private Integer unitsSold;

    @Builder.Default
    @OneToMany(mappedBy = "drug")
    private List<TreatmentDrug> treatmentDrugs = new ArrayList<>();

}
