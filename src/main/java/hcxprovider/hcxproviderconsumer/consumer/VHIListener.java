package hcxprovider.hcxproviderconsumer.consumer;

import hcxprovider.hcxproviderconsumer.repository.*;
import hcxprovider.hcxproviderconsumer.dto.Message;
import hcxprovider.hcxproviderconsumer.services.ListenerService;
import hcxprovider.hcxproviderconsumer.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class VHIListener {

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
            boolean result = listenerService.hcxGenerateRequest(msg);
            log.info(String.valueOf(result));
        }
        catch(Exception e){
            log.error("Exception {}",e);
        }
    }

    @RabbitListener(queues = Constants.RES_QUEUE)
    public void recievedResponse(Message msg) throws Exception {
            log.info("retrieved message :{}",msg);
            listenerService.vhiGenerateResponse(msg);
    }
}
