package hcxprovider.hcxproviderconsumer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClaimIllnessTreatmentDetails {
    private long claimId;
    private String chronicIllnessDetails;
    private long procedureCorporateMappingId;
    private long procedureId;
    private String lineOfTreatmentDetails;
    private Integer leftImplant;
    private Integer rightImplant;
    private Date dateOfDiagnosis;
    private String doctorsDetails;
    private ChronicIllnessDetailsJSON chronicIllnessDetailsJSON;
}
