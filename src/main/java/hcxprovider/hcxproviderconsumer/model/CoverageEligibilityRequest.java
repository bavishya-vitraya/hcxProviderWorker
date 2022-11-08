package hcxprovider.hcxproviderconsumer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "CoverageEligibilityRequests")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CoverageEligibilityRequest {

  @Id
  private String id;
  private String requestType;
  private String hospitalName;
  private String insurerCode;
  private String senderCode;
}
