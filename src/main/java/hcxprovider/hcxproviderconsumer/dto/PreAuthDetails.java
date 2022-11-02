package hcxprovider.hcxproviderconsumer.dto;

import lombok.Data;

@Data
public class PreAuthDetails {
    private String claimId;
    private String hospitalName;
    private String patientName;
    private String patientContact;
    private String patientEmail;
    private String procedure;
    private String quotedAmount;
}
