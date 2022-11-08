package hcxprovider.hcxproviderconsumer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "CoverageEligibilityResponse")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CoverageEligibilityResponse {
    @Id
    private String id;
    private String responseType;
    private String hospitalName;
    private String insurerCode;
    private String senderCode;
}
