package hcxprovider.hcxproviderconsumer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClaimIllnessTreatmentDetails {
    private Integer id;
    private Integer claimId;
    private Integer illnessId;
    private String chronicIllnessDetails;
    private Integer treatmentId;
    private Integer procedureDepartmentId;
    private Integer procedureCorporateMappingId;
    private Integer procedureId;
    private String lineOfTreatmentDetails;
    private Integer leftImplant;
    private Integer rightImplant;
    private String dateOfDiagnosis;
    private String createTime;
    private String updateTime;
    private String doctorsDetails;
    private ChronicIllnessDetailsJSON chronicIllnessDetailsJSON;
}
