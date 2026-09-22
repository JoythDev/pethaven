package io.github.pethaven.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name="treatment_drugs", uniqueConstraints=@UniqueConstraint(columnNames={"treatment_id","drug_id"}))
@Entity
public class TreatmentDrug {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "treatment_id", nullable = false)
    private Treatment treatment;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "drug_id", nullable = false)
    private Drug drug;

    @Column(name = "units", nullable = false)
    private Integer units;

    @Column(name = "unit_purchase_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPurchasePrice;

    @Column(name = "unit_sale_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitSalePrice;
}
