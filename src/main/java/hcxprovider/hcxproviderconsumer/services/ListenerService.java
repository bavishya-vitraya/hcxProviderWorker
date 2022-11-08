package hcxprovider.hcxproviderconsumer.services;

import org.springframework.stereotype.Service;


public interface ListenerService {
    boolean hcxGenerate(String reqType) throws Exception;
    boolean hcxGetResponse(String resType);
}
