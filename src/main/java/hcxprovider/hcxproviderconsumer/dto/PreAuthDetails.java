package hcxprovider.hcxproviderconsumer.dto;

import lombok.Data;

import java.util.List;

@Data
public class PreAuthDetails {
    private List<FileDTO> files;
    private String policySource;
    private Integer requestedAmount;
    private String hospitalCode;
    private String admissionType;
    private String policyNumber;
    private String healthId;
    private String vitrayaReferenceNumber;
    private String attendantMobileNumber;
    private AdditionalDataDTO additionalData;
    private List<String> reasonForAdmission;
    private String email;
}
