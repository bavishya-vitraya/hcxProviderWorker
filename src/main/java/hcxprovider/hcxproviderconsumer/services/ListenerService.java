package hcxprovider.hcxproviderconsumer.services;

import hcxprovider.hcxproviderconsumer.dto.Message;
import hcxprovider.hcxproviderconsumer.dto.MessageResDTO;
import hcxprovider.hcxproviderconsumer.model.PreAuthRequest;
import hcxprovider.hcxproviderconsumer.model.PreAuthResponse;
import org.springframework.stereotype.Service;


public interface ListenerService {
    boolean hcxGenerateRequest(Message msg) throws Exception;
    boolean hcxGenerateResponse(MessageResDTO msg) throws Exception;
    String buildClaimFhirProfile(PreAuthRequest preAuthRequest);
    String buildVhiClaimProfile(PreAuthResponse preAuthResponse);
}
