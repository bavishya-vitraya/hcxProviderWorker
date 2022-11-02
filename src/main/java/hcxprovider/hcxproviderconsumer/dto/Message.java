package hcxprovider.hcxproviderconsumer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private String requestId;
    private String hospitalCode;
    private String insurerCode;
    private String requestType;
}
