package hcxprovider.hcxproviderconsumer.fhir.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Identifier {
    private String system;
    private String value;
}
