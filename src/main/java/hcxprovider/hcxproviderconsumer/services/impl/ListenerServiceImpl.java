package hcxprovider.hcxproviderconsumer.services.impl;

import ca.uhn.fhir.context.FhirContext;
import hcxprovider.hcxproviderconsumer.dto.Message;
import hcxprovider.hcxproviderconsumer.dto.PreAuthDetails;
import hcxprovider.hcxproviderconsumer.model.ClaimRequest;
import hcxprovider.hcxproviderconsumer.model.CoverageEligibilityRequest;
import hcxprovider.hcxproviderconsumer.model.PreAuthRequest;
import hcxprovider.hcxproviderconsumer.repository.ClaimRequestRepo;
import hcxprovider.hcxproviderconsumer.repository.CoverageEligibilityRequestRepo;
import hcxprovider.hcxproviderconsumer.repository.PreAuthRequestRepo;
import hcxprovider.hcxproviderconsumer.services.ListenerService;
import hcxprovider.hcxproviderconsumer.utils.Constants;
import io.hcxprotocol.impl.HCXOutgoingRequest;
import io.hcxprotocol.init.HCXIntegrator;
import io.hcxprotocol.utils.Operations;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.Identifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class ListenerServiceImpl implements ListenerService {
    @Value("classpath:resources/keys/vitraya-mock-provider-private-key.pem")
    String privateKeyPath;

    @Value("${hcx.protocolBasePath}")
    String protocolBasePath;

    @Value("${hcx.authBasePath}")
    String authBasePath;

    @Value("${hcx.participantCode}")
    String participantCode;

    @Value("${hcx.recipientCode}")
    String recipientCode;

    @Value("${hcx.username}")
    String username;

    @Value("${hcx.password}")
    String password;

    @Value("${hcx.igUrl}")
    String igUrl;

    @Autowired
    PreAuthRequestRepo preAuthRequestRepo;

    @Autowired
    CoverageEligibilityRequestRepo coverageEligibilityRequestRepo;

    @Autowired
    ClaimRequestRepo claimRequestRepo;

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
    public boolean hcxGenerate(Message msg) throws Exception {
        //File payloadFile = new ClassPathResource("input/preAuthTest.txt").getFile();
        //String payload = FileUtils.readFileToString(payloadFile);

        CoverageEligibilityRequest coverageEligibilityRequest = new CoverageEligibilityRequest();
        ClaimRequest claimRequest = new ClaimRequest();
        PreAuthRequest preAuthRequest = new PreAuthRequest();
        String reqType = msg.getRequestType();
        String payload;
        Operations operation;
        if (reqType.equalsIgnoreCase(Constants.COVERAGE_ELIGIBILITY)) {
            coverageEligibilityRequest = coverageEligibilityRequestRepo.findCoverageEligibilityRequestById(msg.getRequestId());
            log.info("CoverageEligibility:{}", coverageEligibilityRequest);
            operation = Operations.COVERAGE_ELIGIBILITY_CHECK;
        } else if (reqType.equalsIgnoreCase(Constants.CLAIM)) {
            claimRequest = claimRequestRepo.findClaimRequestById(msg.getRequestId());
            log.info("ClaimReq:{}", claimRequest);
            operation = Operations.CLAIM_SUBMIT;
            payload = buildClaimFhirProfile(preAuthRequest);
        } else if (reqType.equalsIgnoreCase(Constants.PRE_AUTH)) {
            preAuthRequest = preAuthRequestRepo.findPreAuthRequestById(msg.getRequestId());
            log.info("PreAuthReq:{}", preAuthRequest);
            operation = Operations.PRE_AUTH_SUBMIT;
            payload = buildClaimFhirProfile(preAuthRequest);
        }
        HCXIntegrator.init(setConfig());
        Map<String,Object> output = new HashMap<>();
        HCXOutgoingRequest hcxOutgoingRequest = new HCXOutgoingRequest();
        //Boolean response = hcxOutgoingRequest.generate(payload,operation,recipientCode,output);
        //log.info(String.valueOf(response));
       // log.info("{}",output);
        return true;
    }

    @Override
    public String buildClaimFhirProfile(PreAuthRequest preAuthRequest) {
        PreAuthDetails req = preAuthRequest.getPreAuthReq();
        FhirContext fhir = FhirContext.forR4();
        Claim claim = new Claim();
        claim.setUse(Claim.Use.PREAUTHORIZATION);
        claim.setEnterer(preAuthRequest.)
        return "success";
    }
}
