package hcxprovider.hcxproviderconsumer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class DiagnosisDetails {
    private String reasonForNotPaying;
    private String sublimitName;
    private boolean pedImpactOnDiagnosis;
    private String hospitalDiagnosis;
    private String insuranceDiagnosis;
    private boolean considerForPayment;
    private boolean sublimitApplicable;
}
