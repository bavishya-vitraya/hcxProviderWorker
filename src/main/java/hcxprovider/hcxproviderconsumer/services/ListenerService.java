package hcxprovider.hcxproviderconsumer.services;

import hcxprovider.hcxproviderconsumer.dto.Message;
import hcxprovider.hcxproviderconsumer.model.PreAuthRequest;
import org.springframework.stereotype.Service;


public interface ListenerService {
    boolean hcxGenerate(Message msg) throws Exception;
    String buildClaimFhirProfile(PreAuthRequest preAuthRequest);
}
