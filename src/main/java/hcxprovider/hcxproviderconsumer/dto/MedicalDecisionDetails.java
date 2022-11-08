package hcxprovider.hcxproviderconsumer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class MedicalDecisionDetails {
    private BigDecimal coPayPercentage;
    private BigDecimal amountConsidered;
    private String diagnosisOrProcedureName;
    private BigDecimal ambulanceCharges;
    private BigDecimal agreedPackageRate;
    private Boolean ambulanceChargeApplicable;
}
