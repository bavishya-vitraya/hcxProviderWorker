package hcxprovider.hcxproviderconsumer.consumer;

import hcxprovider.hcxproviderconsumer.model.ClaimRequest;
import hcxprovider.hcxproviderconsumer.model.CoverageEligibilityRequest;
import hcxprovider.hcxproviderconsumer.model.PreAuthRequest;
import hcxprovider.hcxproviderconsumer.repository.ClaimRequestRepo;
import hcxprovider.hcxproviderconsumer.dto.Message;
import hcxprovider.hcxproviderconsumer.repository.CoverageEligibilityRequestRepo;
import hcxprovider.hcxproviderconsumer.repository.PreAuthRequestRepo;
import hcxprovider.hcxproviderconsumer.services.ListenerService;
import hcxprovider.hcxproviderconsumer.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import netscape.javascript.JSObject;
import org.apache.tomcat.util.json.JSONParser;
import org.bson.json.JsonObject;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.google.gson.Gson;

import java.util.Optional;

@Slf4j
@Component
public class VHIRequestListener {
    @Autowired
    PreAuthRequestRepo preAuthRequestRepo;
    @Autowired
    CoverageEligibilityRequestRepo coverageEligibilityRequestRepo;
    @Autowired
    ClaimRequestRepo claimRequestRepo;
    @Autowired
    ListenerService listenerService;

    @RabbitListener(queues = "${queue.name}")
    public void recievedMessage(String message) throws Exception {
        try {
            log.info(message);
            Gson json = new Gson();
            Message msg = new Message();
            msg = (Message) json.fromJson(message,Message.class);
            log.info("retrieved message :{}",msg);
            String reqType = msg.getRequestType();
            log.info("Request Type from Message Class object"+reqType);
            CoverageEligibilityRequest coverageEligibilityRequest = new CoverageEligibilityRequest();
            ClaimRequest claimRequest = new ClaimRequest();
            PreAuthRequest preAuthRequest = new PreAuthRequest();
            if (reqType.equalsIgnoreCase(Constants.COVERAGE_ELIGIBILITY)) {
                coverageEligibilityRequest = coverageEligibilityRequestRepo.findCoverageEligibilityRequestById(msg.getRequestId());
            } else if (reqType.equalsIgnoreCase(Constants.CLAIM)) {
                claimRequest = claimRequestRepo.findClaimRequestById(msg.getRequestId());
            } else if (reqType.equalsIgnoreCase(Constants.PRE_AUTH)) {
                preAuthRequest = preAuthRequestRepo.findPreAuthRequestById(msg.getRequestId());
            }
            log.info("CoverageEligibility:{}", coverageEligibilityRequest);
            log.info("ClaimReq:{}", claimRequest);
            log.info("PreAuthReq:{}", preAuthRequest);
            boolean result = listenerService.hcxGenerate(reqType);
            log.info(String.valueOf(result));
        }
        catch(Exception exception){
            log.error("Exception :"+exception);
        }
    }
}
