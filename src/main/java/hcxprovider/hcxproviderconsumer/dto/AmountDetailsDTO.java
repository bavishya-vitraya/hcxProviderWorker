package hcxprovider.hcxproviderconsumer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AmountDetailsDTO {
    private BigDecimal claimedAmountAfterDiscount;
    private BigDecimal consideredAmountTotal;
    private BigDecimal discountInHospitalBill;
    private List<ClaimedAmountBreakupDTO> claimedAmountBreakup;
}
