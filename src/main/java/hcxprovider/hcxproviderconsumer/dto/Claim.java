package hcxprovider.hcxproviderconsumer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Claim {

    private Integer id;
    private Integer creatorId;
    private String createdDate;
    private boolean deleted;
    private Integer hospitalId;
    private String hospitalPatientId;
    private Integer insuranceAgencyId;
    private String updatedDate;
    private Integer state;
    private String status;
    private Integer age;
    private String dob;
    private String gender;
    private String medicalCardId;
    private String patientName;
    private String policyHolderName;
    private String policyNumber;
    private String policyType;
    private boolean validated;
    private String policyInceptionDate;
    private String cityName;
    private Integer coPayPercentage;
    private String policyEndDate;
    private String policyName;
    private String productCode;
    private String policyStartDate;
    private BigDecimal sumInsured;
    private BigDecimal availableSumInsured;
    private Integer cumulativeBonus;
    private Integer authorisedAmount;
    private String premiumPaymentZone;
    private String preExistingDesease;
    private Integer medicalEventId;
    private String metadata;
    private String automaticRestoration;
    private String superRestoration;
    private Integer rechargeBenefitAmount;
    private Integer superRestorationBenefitAmount;
    private String patient_mobile_no;
    private String attendent_mobile_no;
    private String patient_email_id;
    private String ped_list;
    private boolean isSTPClaimReview;
    private boolean sendToManual;
}
