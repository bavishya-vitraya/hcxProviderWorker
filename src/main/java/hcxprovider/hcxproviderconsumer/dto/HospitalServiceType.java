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
public class HospitalServiceType {
    private VitrayaRoomCategory vitrayaRoomCategory;
    private String roomType;
    private String insurerRoomType;
    private boolean singlePrivateAC;
    private BigDecimal roomTariffPerDay;
    private ServiceType serviceType;
}
