package hcxprovider.hcxproviderconsumer.consumer;

import hcxprovider.hcxproviderconsumer.dto.MessageResDTO;
import hcxprovider.hcxproviderconsumer.model.*;
import hcxprovider.hcxproviderconsumer.repository.*;
import hcxprovider.hcxproviderconsumer.dto.Message;
import hcxprovider.hcxproviderconsumer.services.ListenerService;
import hcxprovider.hcxproviderconsumer.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import netscape.javascript.JSObject;
import org.apache.tomcat.util.json.JSONParser;
import org.bson.json.JsonObject;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
     PreAuthResponseRepo preAuthResponseRepo;
    @Autowired
    CoverageEligibilityResponseRepo coverageEligibilityResponseRepo;
    @Autowired
     ClaimResponseRepo claimResponseRepo;
    @Autowired
     ListenerService listenerService;

    @RabbitListener(queues = Constants.REQ_QUEUE)
    public void recievedRequest(Message msg) throws Exception {
        try {
            log.info("retrieved message :{}",msg);
            boolean result = listenerService.hcxGenerate(msg);
            log.info(String.valueOf(result));
        }
        catch(Exception exception){
            log.error("Exception :"+exception);
        }
    }

    @RabbitListener(queues = Constants.RES_QUEUE)
    public void recievedResponse(MessageResDTO msg) throws Exception {
        try {
//            log.info(message);
//            Gson json = new Gson();
//            MessageResDTO msg = new MessageResDTO();
//            msg = (MessageResDTO) json.fromJson(message,MessageResDTO.class);
             log.info("retrieved message :{}", msg);
            String resType = msg.getResponseType();
            log.info("Response Type from Message Class object"+resType);
            CoverageEligibilityResponse coverageEligibilityResponse = new CoverageEligibilityResponse();
            ClaimResponse claimResponse = new ClaimResponse();
            PreAuthResponse preAuthResponse = new PreAuthResponse();
            if (resType.equalsIgnoreCase(Constants.COVERAGE_ELIGIBILITY)) {
                coverageEligibilityResponse = coverageEligibilityResponseRepo.findCoverageEligibilityResponseById(msg.getResponseId());
            } else if (resType.equalsIgnoreCase(Constants.CLAIM)) {
                claimResponse = claimResponseRepo.findClaimResponseById(msg.getResponseId());
            } else if (resType.equalsIgnoreCase(Constants.PRE_AUTH)) {
                preAuthResponse= preAuthResponseRepo.findPreAuthResponseById(msg.getResponseId());
            }
            log.info("CoverageEligibilityResponse:{}", coverageEligibilityResponse);
            log.info("ClaimResponse:{}", claimResponse);
            log.info("PreAuthResponse:{}", preAuthResponse);
            //boolean result = listenerService.hcxGenerate(resType);
           // log.info(String.valueOf(result));
        }
        catch(Exception exception){
            log.error("Exception :"+exception);
        }

    }
}
