package hcxprovider.hcxproviderconsumer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimedAmountBreakupDTO {
    private int componentNumberOfDaysClaimed; // Required where component is Room or ICU or Nursing. TBD - Flush our the details.
    private int componentNumberOfDaysAllowed; //    Number of days allowed for this treatment. Required where component is Room or ICU or Nursing.
    private BigDecimal componentPerDayAmountClaimed; /*Required where component is Room or ICU or Nursing. TBD - Flush our the details.*/
    private BigDecimal componentPerDayAmountEligible; // Per day amount eligible for this treatment. Required where component is Room or ICU or Nursing.
    private BigDecimal componentPerDayAmountConsidered; // Per day amount considered for this treatment. Lesser of claimed and eligible. Required where component is Room or ICU or Nursing.
    private String componentName; //    Name of the component of the bill.
    private BigDecimal componentTotalClaimedAmount; // Claimed amount rolled up at this component level.
    private BigDecimal componentNetAmountClaimed; // Net Amount after deductible.
    private BigDecimal componentDeductibleAmount; // Amount deductible for non-payables.
    private BigDecimal componentNonPayables; // Non payabale amount due to product conditions.
    private BigDecimal componentPayableAmount; //Payable amount for this component.
    private String componentDeductibleOrNonPayablesReason;
}
