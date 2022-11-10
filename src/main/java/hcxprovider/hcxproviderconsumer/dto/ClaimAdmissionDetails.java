package hcxprovider.hcxproviderconsumer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClaimAdmissionDetails {
    private Integer id;
    private Integer claimId;
    private String admissionDate;
    private String dischargeDate;
    private String roomType;
    private Integer hospitalServiceTypeId;
    private Integer stayDuration;
    private String costEstimation;
    private boolean isPackage;
    private BigDecimal packageAmount;
    private boolean icuStay;
    private Integer icuStayDuration;
    private Integer icuServiceTypeId;
    private String createTime;
    private String updateTime;
}
