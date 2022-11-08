package hcxprovider.hcxproviderconsumer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "claimrequests")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ClaimRequest {
    private String id;
    private String insurerCode;
    private String senderCode;
    private String hospitalName;
    private String requestType;
}
