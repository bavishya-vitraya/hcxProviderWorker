package hcxprovider.hcxproviderconsumer.services;

import hcxprovider.hcxproviderconsumer.dto.Message;
import hcxprovider.hcxproviderconsumer.model.PreAuthRequest;
import hcxprovider.hcxproviderconsumer.model.PreAuthResponse;


public interface ListenerService {
    boolean hcxGenerateRequest(Message msg) throws Exception;
    boolean hcxGenerateResponse(Message msg) throws Exception;
    String buildClaimFhirProfile(PreAuthRequest preAuthRequest);
    String buildClaimResponseFhirProfile(PreAuthResponse preAuthResponse);
    String buildVhiClaimProfile(PreAuthResponse preAuthResponse);
}
