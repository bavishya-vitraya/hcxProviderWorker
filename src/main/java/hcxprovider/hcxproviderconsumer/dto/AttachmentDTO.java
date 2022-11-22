package hcxprovider.hcxproviderconsumer.dto;

import hcxprovider.hcxproviderconsumer.enums.ServiceType;
import hcxprovider.hcxproviderconsumer.enums.VitrayaRoomCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttachmentDTO {
    private int serviceTypeId;
    private boolean deleted;
    private Date updatedDate; //claim
    private int state; //claim
    private String status; //claim
    private int age; //claim
    private String productCode; //claim
    private int medicalEventId; //claim
    private long procedureCorporateMappingId; //claimIllnessTreatmentDetails
    private long procedureId; //claimIllnessTreatmentDetails
    private Integer leftImplant; //claimIllnessTreatmentDetails
    private Integer rightImplant; //claimIllnessTreatmentDetails
    private int hospitalServiceTypeId; //claimAdmissionDetails
    private int stayDuration; //claimAdmissionDetails
    private String costEstimation;  //claimAdmissionDetails
    private BigDecimal packageAmount; //claimAdmissionDetails
    private int icuStayDuration; //claimAdmissionDetails
    private int icuServiceTypeId; //claimAdmissionDetails
    private String vitrayaRoomCategory;//hospitalServiceType
    private String insurerRoomType;//hospitalServiceType
    private boolean singlePrivateAC;//hospitalServiceType
    private ServiceType serviceType;//hospitalServiceType
    private long parentTableId; //documentMasterList
    private int illnessCategoryId;//illness
}