package hcxprovider.hcxproviderconsumer.services.impl;

import hcxprovider.hcxproviderconsumer.services.ListenerService;
import hcxprovider.hcxproviderconsumer.utils.Constants;
import io.hcxprotocol.impl.HCXOutgoingRequest;
import io.hcxprotocol.init.HCXIntegrator;
import io.hcxprotocol.utils.Operations;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class ListenerServiceImpl implements ListenerService {
    @Value("classpath:resources/keys/vitraya-mock-provider-private-key.pem")
    String privateKeyPath;

    @Value("${protocolBasePath}")
    String protocolBasePath;

    @Value("${authBasePath}")
    String authBasePath;

    @Value("${participantCode}")
    String participantCode;

    @Value("${recipientCode}")
    String recipientCode;

    @Value("${username}")
    String username;

    @Value("${password}")
    String password;

    @Value("${igUrl}")
    String igUrl;

    @Value("classpath:resources/input/message.txt")
    String payloadPath;

    public Map<String, Object> setConfig() throws IOException {
        Map<String, Object> config = new HashMap<>();
        File file = new ClassPathResource("keys/vitraya-mock-provider-private-key.pem").getFile();
        String privateKey= FileUtils.readFileToString(file);
        config.put("protocolBasePath", protocolBasePath);
        config.put("authBasePath", authBasePath);
        config.put("participantCode",participantCode);
        config.put("username", username);
        config.put("password",password);
        config.put("encryptionPrivateKey", privateKey);
        config.put("igUrl", igUrl);
        return config;
    }

    @Override
    public boolean hcxGenerate(String reqType) throws Exception {

        File payloadFile = new ClassPathResource("input/message.txt").getFile();
        String payload = FileUtils.readFileToString(payloadFile);

        HCXIntegrator.init(setConfig());
        Operations operation;
        if (reqType.equalsIgnoreCase(Constants.PRE_AUTH)) {
            operation = Operations.PRE_AUTH_SUBMIT;
        } else if (reqType.equalsIgnoreCase(Constants.CLAIM)) {
            operation = Operations.CLAIM_SUBMIT;
        } else{
            operation = Operations.COVERAGE_ELIGIBILITY_CHECK;
        }
        Map<String,Object> output = new HashMap<>();
        HCXOutgoingRequest hcxOutgoingRequest = new HCXOutgoingRequest();
        return hcxOutgoingRequest.generate(payload,operation,recipientCode,output);

    }
}
