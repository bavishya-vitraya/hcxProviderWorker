package hcxprovider.hcxproviderconsumer.services;

import hcxprovider.hcxproviderconsumer.dto.Message;
import hcxprovider.hcxproviderconsumer.dto.PreAuthVhiResponse;
import hcxprovider.hcxproviderconsumer.model.PreAuthRequest;
import hcxprovider.hcxproviderconsumer.model.PreAuthResponse;

import java.text.ParseException;


public interface ListenerService {
    boolean hcxGenerateRequest(Message msg) throws Exception;
    boolean vhiGenerateResponse(Message msg) throws Exception;
    String buildClaimFhirProfile(PreAuthRequest preAuthRequest) throws ParseException;
    PreAuthVhiResponse buildVhiClaimProfile(String fhirPayload);
}
