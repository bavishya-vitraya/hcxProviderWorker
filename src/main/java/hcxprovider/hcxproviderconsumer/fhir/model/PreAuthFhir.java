package hcxprovider.hcxproviderconsumer.fhir.model;

import hcxprovider.hcxproviderconsumer.fhir.dto.Entry;
import hcxprovider.hcxproviderconsumer.fhir.dto.Identifier;
import hcxprovider.hcxproviderconsumer.fhir.dto.Meta;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PreAuthFhir {
    private String resourceType;
    private String id;
    private Meta meta;
    private Identifier identifier;
    private String type;
    private String timestamp;
    private List<Entry> entry;


}
