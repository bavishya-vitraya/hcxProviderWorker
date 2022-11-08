package hcxprovider.hcxproviderconsumer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProcedureDetails {
    private String reasonForNotPaying;
    private String procedureCode;
    private String sublimitName;
    private Boolean pedImpactOnDiagnosis;
    private Boolean considerForPayment;
    private Boolean sublimitApplicable;
}
