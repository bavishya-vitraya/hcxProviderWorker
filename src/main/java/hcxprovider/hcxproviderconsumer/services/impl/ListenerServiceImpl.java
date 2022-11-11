package hcxprovider.hcxproviderconsumer.services.impl;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import hcxprovider.hcxproviderconsumer.dto.Message;
import hcxprovider.hcxproviderconsumer.dto.PreAuthDetails;
import hcxprovider.hcxproviderconsumer.dto.Procedure;
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
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;

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

        Practitioner practitioner = new Practitioner();
        practitioner.addIdentifier().setId(req.getClaim().getCreatorId().toString());

        Claim claim = new Claim();
        claim.setUse(Claim.Use.PREAUTHORIZATION);
        claim.addIdentifier().setValue(req.getServiceTypeId().toString());
        claim.getEnterer().setId(req.getClaim().getCreatorId().toString());
        claim.setCreated(new Date(req.getClaim().getCreatedDate()));
        claim.setStatus(Claim.ClaimStatus.CANCELLED);
        claim.getProvider().setId(req.getClaim().getHospitalId().toString());
        claim.getPatient().setId(req.getClaim().getHospitalPatientId());
        claim.getInsurer().setId(req.getClaim().getInsuranceAgencyId().toString());
        // claim.getPatient().setDOb claim.dob 17
        //claim.getPatient(). claim.gender 18
        claim.getPatient().getIdentifier().setId(req.getClaim().getMedicalCardId());
        // claim.getPatient() claim.name 20
        claim.addInsurance().getCoverage().setReference(req.getClaim().getPolicyHolderName());
        claim.addInsurance().getCoverage().setReference(req.getClaim().getPolicyNumber());
        claim.addInsurance().getCoverage().setReference(req.getClaim().getPolicyType());
        claim.addInsurance().getCoverage().setReference(req.getClaim().getPolicyEndDate());
        //29
        claim.addInsurance().getCoverage().setReference(req.getClaim().getPolicyStartDate());
        claim.addSupportingInfo().getCategory().addCoding().setCode(req.getClaim().getPreExistingDesease());
        claim.getMeta().setSource(req.getClaim().getMetadata());
        //  claim.getPatient().ge44-46
        claim.addSupportingInfo().getCategory().addCoding().setCode(req.getClaim().getPed_list());
        claim.addIdentifier().setId(req.getClaimIllnessTreatmentDetails().getClaimId().toString());
        claim.addDiagnosis().getDiagnosis().setUserData("chronicIllnessDetails", req.getClaimIllnessTreatmentDetails().getChronicIllnessDetails().toString());
        claim.addSupportingInfo().getCategory().addCoding().setCode(req.getClaimIllnessTreatmentDetails().getLineOfTreatmentDetails());
        claim.addProcedure().getProcedure().setUserData("leftImplant", req.getClaimIllnessTreatmentDetails().getLeftImplant());
        claim.addProcedure().getProcedure().setUserData("rightImplant", req.getClaimIllnessTreatmentDetails().getRightImplant());
        claim.addDiagnosis().getDiagnosis().setUserData("dateOfDiagnosis", req.getClaimIllnessTreatmentDetails().getDateOfDiagnosis());
        claim.addCareTeam().getProvider().setReference(req.getClaimIllnessTreatmentDetails().getDoctorsDetails());
        // claim.addSupportingInfo().getCategory().addCoding(req.getClaimIllnessTreatmentDetails().getChronicIllnessDetailsJSON().getChronicIllnessList());
        claim.addIdentifier().setId(req.getClaimAdmissionDetails().getClaimId().toString());
        claim.addSupportingInfo().getCategory().addCoding().setCode(req.getClaimAdmissionDetails().getAdmissionDate());
        claim.addSupportingInfo().getCategory().addCoding().setCode(req.getClaimAdmissionDetails().getDischargeDate());
        claim.addItem().addDetail().getCategory().addCoding().setCode(req.getClaimAdmissionDetails().getRoomType());
        claim.addItem().getServiced().setUserData("stayDuration", req.getClaimAdmissionDetails().getStayDuration());
        // claim.addItem().addDetail().getNet().setCurrency((req.getClaimAdmissionDetails().getCostEstimation()));
        claim.addSupportingInfo().getCategory().addCoding().setCode(req.getClaimAdmissionDetails().getIcuStayDuration().toString());
        claim.addItem().addDetail().getCategory().addCoding().setCode((req.getHospitalServiceType().getRoomType()));
        claim.addProcedure().getProcedure().setUserData("description", req.getProcedure().getDescription());
        claim.addProcedure().getProcedure().setUserData("name", req.getProcedure().getName());
        claim.addProcedure().getProcedure().setUserData("procedureCode", req.getProcedure().getProcedureCode());
        claim.addProcedure().getProcedure().setUserData("procedureId", req.getProcedureMethod().getProcedureId());
        claim.addProcedure().getProcedure().setUserData("procedureMethodName", req.getProcedureMethod().getProcedureMethodName());
        claim.addProcedure().getProcedure().setUserData("procedureMethodDisplayName", req.getProcedureMethod().getProcedureMethodDisplayName());
        claim.addProcedure().getProcedure().setUserData("procedureCode", req.getProcedureMethod().getProcedureCode());
        claim.addSupportingInfo().getCategory().addCoding().setCode(req.getDocumentMasterList().get(0).getFileName());
        claim.addSupportingInfo().getCategory().addCoding().setCode(req.getDocumentMasterList().get(0).getStorageFileName());
        claim.addDiagnosis().getDiagnosis().setUserData("illnessName", req.getIllness().getIllnessName());
        claim.addDiagnosis().getDiagnosis().setUserData("defaultICDCode", req.getIllness().getDefaultICDCode());
        claim.addDiagnosis().getDiagnosis().setUserData("illnessDescription", req.getIllness().getIllnessDescription());
        claim.addDiagnosis().getDiagnosis().setUserData("relatedDisease", req.getIllness().getRelatedDisease());
        claim.addDiagnosis().getDiagnosis().setUserData("active", req.getIllness().isActive());

        IParser p = FhirContext.forR4().newJsonParser().setPrettyPrint(true);
        String messageString = p.encodeResourceToString(claim);
        System.out.println("here is the json " + messageString);
        //log.info("Document bundle: {}", ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(document));
        return "success";
    }
}
