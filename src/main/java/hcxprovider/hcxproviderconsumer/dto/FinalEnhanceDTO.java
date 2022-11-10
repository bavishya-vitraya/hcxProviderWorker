package hcxprovider.hcxproviderconsumer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class FinalEnhanceDTO {
    private String requestedAmount; //
    private String dateOfAdmission; //
    private String dateOfDischarge; //
    private boolean includesFinalBill; //
    private String roomCategory; //
    private List<String> diagnosis; //
    private List<String> procedure;
    private List<FileDTO> files; //
    private String amountBreakup;
    //private AdjudicationDetailsRequest adjudicationResult;
    private String vitrayaReferenceNumber;
    private String queryResponse;
}
