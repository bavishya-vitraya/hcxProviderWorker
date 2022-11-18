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
    private Date updatedDate;
    private int state;
    private String status;
    private int age;
    private String productCode;
    private int medicalEventId;
    private long procedureCorporateMappingId;
    private long procedureId;
    private Integer leftImplant;
    private Integer rightImplant;
    private int hospitalServiceTypeId;
    private int stayDuration;
    private String costEstimation;
    private BigDecimal packageAmount;
    private int icuStayDuration;
    private int icuServiceTypeId;
    private String vitrayaRoomCategory;
    private String insurerRoomType;
    private boolean singlePrivateAC;
    private ServiceType serviceType;
    private long parentTableId;
    private int illnessCategoryId;
}
