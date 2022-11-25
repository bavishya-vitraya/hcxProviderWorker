package hcxprovider.hcxproviderconsumer.dto;

import hcxprovider.hcxproviderconsumer.enums.PolicyType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Claim {
    private Long id;
    private Long creatorId;
    private String createdDate;
    private boolean deleted;
    private int hospitalId;
    private String hospitalPatientId;
    private int insuranceAgencyId;
    private String updatedDate;
    private String intimationNumber;
    private int state;
    private String status;
    private int age;
    private String dob;
    private String gender;
    private String medicalCardId;
    private String patientName;
    private String policyHolderName;
    private String policyNumber;
    private PolicyType policyType;
    private Date policyInceptionDate;
    private String cityName;
    private Date policyEndDate;
    private String policyName;
    private String productCode;
    private Date policyStartDate;
    private int medicalEventId;
    private String patient_mobile_no;
    private String attendent_mobile_no;
    private String patient_email_id;
}
