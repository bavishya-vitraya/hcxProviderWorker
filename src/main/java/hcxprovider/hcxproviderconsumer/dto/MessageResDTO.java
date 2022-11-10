package hcxprovider.hcxproviderconsumer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageResDTO {
    private String responseId;
    private String senderCode;
    private String insurerCode;
    private String responseType;
    private String response;
}
