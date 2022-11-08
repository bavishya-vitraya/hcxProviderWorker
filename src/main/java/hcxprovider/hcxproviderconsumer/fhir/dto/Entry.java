package hcxprovider.hcxproviderconsumer.fhir.dto;

import lombok.Data;

@Data
public class Entry {
   private String fullUrl;
   private Resource resource;
}
