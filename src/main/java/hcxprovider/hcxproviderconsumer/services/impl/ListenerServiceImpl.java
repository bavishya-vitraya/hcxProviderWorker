package hcxprovider.hcxproviderconsumer.services.impl;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.util.BundleBuilder;
import hcxprovider.hcxproviderconsumer.dto.Message;
import hcxprovider.hcxproviderconsumer.dto.MessageResDTO;
import hcxprovider.hcxproviderconsumer.dto.PreAuthDetails;
import hcxprovider.hcxproviderconsumer.model.ClaimRequest;
import hcxprovider.hcxproviderconsumer.model.CoverageEligibilityRequest;
import hcxprovider.hcxproviderconsumer.model.PreAuthRequest;
import hcxprovider.hcxproviderconsumer.model.PreAuthResponse;
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
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.joda.time.DateTime;
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
    public boolean hcxGenerateRequest(Message msg) throws Exception {
//        File payloadFile = new ClassPathResource("input/claim.txt").getFile();
//        String payload = FileUtils.readFileToString(payloadFile);
        String payload = null;
        CoverageEligibilityRequest coverageEligibilityRequest = new CoverageEligibilityRequest();
        ClaimRequest claimRequest = new ClaimRequest();
        PreAuthRequest preAuthRequest = new PreAuthRequest();
        String reqType = msg.getRequestType();

        Operations operation = null;
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
        Map<String, Object> output = new HashMap<>();
        HCXOutgoingRequest hcxOutgoingRequest = new HCXOutgoingRequest();
        Boolean response = hcxOutgoingRequest.generate(payload, operation, recipientCode, output);
        log.info(String.valueOf(response));
        log.info("{}", output);
        return true;
    }


    @Override
    public boolean hcxGenerateResponse(MessageResDTO msg) throws Exception {
        return false;
    }

    @Override
    public String buildClaimFhirProfile(PreAuthRequest preAuthRequest) {
        PreAuthDetails preAuth = preAuthRequest.getPreAuthReq();


        Practitioner entererPractitioner = new Practitioner();
        entererPractitioner.setId("Practitioner/1");
        entererPractitioner.addIdentifier().setValue(preAuth.getClaim().getCreatorId().toString()).setSystem("http://hl7.org/fhir/resource-types");

        Practitioner carePractitioner = new Practitioner();
        carePractitioner.setId("Practitioner/2");
        carePractitioner.addName().addGiven(preAuth.getClaimIllnessTreatmentDetails().getDoctorsDetails());
        carePractitioner.addQualification().getCode().addCoding().setCode(preAuth.getClaimIllnessTreatmentDetails().getDoctorsDetails());

        Organization organization = new Organization();
        organization.setId("Organization/1");
        organization.addIdentifier().setValue(preAuth.getClaim().getHospitalId().toString()).setSystem("http://abdm.gov.in/facilities");
        organization.addContact().getPurpose().addCoding().setCode(preAuth.getClaim().getCityName()).setDisplay("cityName");

        Organization organizationInsurer = new Organization();
        organizationInsurer.setId("Organization/2");
        organizationInsurer.addIdentifier().setValue(preAuth.getClaim().getInsuranceAgencyId().toString()).setSystem("http://abdm.gov.in/facilities");

        Patient patient = new Patient();
        patient.setId("Patient/1");
        patient.addIdentifier().setValue(preAuth.getClaim().getHospitalPatientId()).setSystem("http://abdm.gov.in/patients");
        patient.setBirthDate(new Date(preAuth.getClaim().getDob()));
        patient.getGenderElement().setValue(AdministrativeGender.valueOf(preAuth.getClaim().getGender()));
        patient.addName().addGiven(preAuth.getClaim().getPatientName());
        patient.addTelecom().setValue(preAuth.getClaim().getPatient_mobile_no()).setSystem(ContactPointSystem.PHONE);
        patient.addContact().addTelecom().setSystem(ContactPointSystem.PHONE).setValue(preAuth.getClaim().getAttendent_mobile_no());
        patient.addContact().addTelecom().setSystem(ContactPointSystem.EMAIL).setValue(preAuth.getClaim().getPatient_email_id());
        //47

        Coverage insuranceCoverage = new Coverage();
        insuranceCoverage.setId("Coverage/1");
        insuranceCoverage.setSubscriberId(preAuth.getClaim().getMedicalCardId());
        insuranceCoverage.setPolicyHolder(new Reference("Patient/1"));
        insuranceCoverage.addIdentifier().setValue(preAuth.getClaim().getPolicyNumber()).setSystem("https://www.gicofIndia.in/policies");
        insuranceCoverage.getType().addCoding().setCode(preAuth.getClaim().getPolicyType()).setDisplay("policyType");

        insuranceCoverage.getPeriod().setEnd(new Date(preAuth.getClaim().getPolicyEndDate()));
        insuranceCoverage.getPeriod().setStart(new Date(preAuth.getClaim().getPolicyStartDate()));


        Coverage coverage = new Coverage();
        coverage.addClass_().setValue(preAuth.getClaim().getPolicyName());
        // coverage37

        Meta meta = new Meta();
        //  meta.setSource(preAuth.getClaim().getMetadata());


        Condition condition = new Condition();
        condition.setId("Condition/1");
        condition.setSubject(new Reference("Patient/1"));
        condition.getCode().addCoding().setDisplay("ChronicIllnessDetails").setCode(preAuth.getClaimIllnessTreatmentDetails().getChronicIllnessDetails());
        condition.setRecordedDate(new Date(preAuth.getClaimIllnessTreatmentDetails().getDateOfDiagnosis()));
        condition.getCode().addCoding().setDisplay("illnessName").setCode(preAuth.getIllness().getIllnessName());
        condition.getCode().addCoding().setDisplay("defaultICDCode").setCode(preAuth.getIllness().getDefaultICDCode());
        condition.getCode().addCoding().setDisplay("illnessDescription").setCode(preAuth.getIllness().getIllnessDescription());
        condition.getCode().addCoding().setDisplay("relatedDisease").setCode(preAuth.getIllness().getRelatedDisease());


        Device device = new Device();
        device.setId("Device/1");


        Procedure procedure = new Procedure();
        procedure.setId("Procedure/1");
        procedure.setStatus(Procedure.ProcedureStatus.COMPLETED);
        procedure.setSubject(new Reference("Patient/1"));
        procedure.addFocalDevice().setManipulated(new Reference("Device/1")).getAction().addCoding().setCode(preAuth.getClaimIllnessTreatmentDetails().getLeftImplant().toString()).setDisplay("LeftImplant");
        procedure.addFocalDevice().setManipulated(new Reference("Device/1")).getAction().addCoding().setCode(preAuth.getClaimIllnessTreatmentDetails().getRightImplant().toString()).setDisplay("RightImplant");
        procedure.addIdentifier().setSystem("https://www.gicofIndia.in/policies").setValue(preAuth.getProcedureMethod().getProcedureId().toString());
        procedure.getCode().addCoding().setDisplay("procedureMethodName").setCode(preAuth.getProcedureMethod().getProcedureMethodName());
        procedure.getCode().addCoding().setDisplay("procedureMethodDisplayName").setCode(preAuth.getProcedureMethod().getProcedureMethodDisplayName());
        procedure.getCode().addCoding().setDisplay("procedureCode").setCode(preAuth.getProcedureMethod().getProcedureCode());
        procedure.addNote().setText(preAuth.getProcedure().getDescription());
        procedure.getCode().addCoding().setDisplay("name").setCode(preAuth.getProcedure().getName());


        Claim claim = new Claim();
        claim.setUse(Claim.Use.PREAUTHORIZATION);
        claim.setId("Claim/1");
        claim.addIdentifier().setSystem("https://www.tmh.in/hcx-documents").setValue(preAuth.getClaim().getId().toString());
        claim.setEnterer(new Reference("Practitioner/1"));
        claim.setCreated(new Date(preAuth.getClaim().getCreatedDate()));
        claim.setStatus(Claim.ClaimStatus.ACTIVE);
        claim.setProvider(new Reference("Organization/1"));
        claim.setPatient(new Reference("Patient/1"));
        claim.setInsurer(new Reference("Organization/2"));
        claim.addInsurance().setSequence(1).setFocal(true).setCoverage(new Reference("Coverage/1"));
        claim.setMeta(meta);
        claim.addIdentifier().setSystem("https://www.gicofIndia.in/policies").setValue(preAuth.getClaimIllnessTreatmentDetails().getClaimId().toString());
        claim.addDiagnosis().setSequence(1).getDiagnosisReference().setReference("Condition/1");
        claim.addSupportingInfo().setSequence(1).getCategory().addCoding().setCode(preAuth.getClaim().getPolicyInceptionDate());
        claim.addSupportingInfo().setSequence(2).setCategory(new CodeableConcept().setText(preAuth.getClaim().getPolicyInceptionDate())).getTimingDateType().setValue(new Date(preAuth.getClaim().getPolicyInceptionDate()));
        //47

        claim.addProcedure().setSequence(1).getProcedureReference().setReference("Procedure/1");
        claim.addCareTeam().setSequence(1).getProvider().setReference("Practitioner/2");

        // doubt
        claim.getType().setText("claim");
        claim.getPriority().setText("claim");

        //67

        //claim admission details
        //claim.addSupportingInfo().setSequence(5).getCategory().addCoding().setSystem("ONS").setCode("ONS-1").setDisplay(preAuth.getClaimAdmissionDetails().getAdmissionDate());
        //claim.addSupportingInfo().setSequence(6).getCategory().addCoding().setSystem("ONS").setCode("ONS-2").setDisplay(preAuth.getClaimAdmissionDetails().getDischargeDate());
        claim.addItem().setSequence(1).setProductOrService(new CodeableConcept().setText("roomType")).addDetail().setSequence(1).setProductOrService(new CodeableConcept().setText("roomType")).getCategory().addCoding().setCode(preAuth.getClaimAdmissionDetails().getRoomType()).setDisplay("roomType");
        claim.addItem().setSequence(2).setProductOrService(new CodeableConcept().setText("roomType")).addDetail().setSequence(2).setProductOrService(new CodeableConcept().setText("roomType")).getProductOrService().addCoding().setDisplay("roomType").setCode(preAuth.getClaimAdmissionDetails().getRoomType());
        //76

        //document master list
        claim.addItem().setSequence(3).setProductOrService(new CodeableConcept().setText("CostEstimation")).addDetail().setSequence(3).setProductOrService(new CodeableConcept().setText("CostEstimation")).getNet().setUserData("CostEstimation", preAuth.getClaimAdmissionDetails().getCostEstimation());
        claim.addItem().setSequence(4).setProductOrService(new CodeableConcept().setText("CostEstimation")).addDetail().setSequence(4).setProductOrService(new CodeableConcept().setText("CostEstimation")).getQuantity().setUserData("CostEstimation", preAuth.getClaimAdmissionDetails().getCostEstimation());
        claim.addItem().setSequence(5).setProductOrService(new CodeableConcept().setText("CostEstimation")).addDetail().setSequence(5).setProductOrService(new CodeableConcept().setText("CostEstimation")).getCategory().addCoding().setDisplay("costEstimation").setCode(preAuth.getClaimAdmissionDetails().getCostEstimation());
//        claim.addSupportingInfo().setSequence(7).getCategory().addCoding().setSystem("ONS").setCode(String.valueOf(preAuth.getClaimAdmissionDetails().isIcuStay()));
//        claim.addSupportingInfo().setSequence(8).getCategory().addCoding().setSystem("ONS").setCode("ONS-6").setDisplay(preAuth.getClaimAdmissionDetails().getIcuStayDuration().toString());
//        claim.addSupportingInfo().setSequence(9).getCategory().addCoding().setSystem("ATT").setCode(preAuth.getDocumentMasterList().get(0).getDocumentType());
//        claim.addSupportingInfo().setSequence(10).getCategory().addCoding().setSystem("ATT").setCode(preAuth.getDocumentMasterList().get(0).getFileName());
//        claim.addSupportingInfo().setSequence(11).getCategory().addCoding().setSystem("ATT").setCode(preAuth.getDocumentMasterList().get(0).getStorageFileName());
//        claim.addSupportingInfo().setSequence(12).getCategory().addCoding().setSystem("ATT").setCode(preAuth.getDocumentMasterList().get(0).getFileType());


        // hospitalServiceType completed
        claim.addItem().setSequence(6).setProductOrService(new CodeableConcept().setText("roomType")).addDetail().setSequence(6).setProductOrService(new CodeableConcept().setText("roomType")).getCategory().addCoding().setDisplay("roomType").setCode(preAuth.getHospitalServiceType().getRoomType());
        claim.addItem().setSequence(7).setProductOrService(new CodeableConcept().setText("roomTariffPerDay")).getUnitPrice().setCurrency("INR").setValue(preAuth.getHospitalServiceType().getRoomTariffPerDay());


        //37(3rd)
        Composition composition = new Composition();
        composition.setId("composition/" + UUID.randomUUID().toString());
        composition.setStatus(Composition.CompositionStatus.FINAL);
        composition.getType().addCoding().setSystem("https://www.hcx.org/document-type").setCode("HcxClaimRequest");
        composition.addAuthor().setReference("Organization/1");
        composition.setDate(new Date());
        composition.setTitle("Claim Request");
        composition.addSection().addEntry().setReference("Claim/1");

        FhirContext fhirctx = FhirContext.forR4();
        Bundle bundle = new Bundle();
        bundle.setId(UUID.randomUUID().toString());
        bundle.setType(Bundle.BundleType.DOCUMENT);
        bundle.getIdentifier().setSystem("https://www.tmh.in/bundle").setValue(bundle.getId());
        bundle.setTimestamp(new Date());
        bundle.addEntry().setFullUrl(composition.getId()).setResource(composition);
        bundle.addEntry().setFullUrl(patient.getId()).setResource(patient);
        bundle.addEntry().setFullUrl(carePractitioner.getId()).setResource(carePractitioner);
        bundle.addEntry().setFullUrl(organization.getId()).setResource(organization);
        bundle.addEntry().setFullUrl(organizationInsurer.getId()).setResource(organizationInsurer);
        bundle.addEntry().setFullUrl(entererPractitioner.getId()).setResource(entererPractitioner);
        bundle.addEntry().setFullUrl(procedure.getId()).setResource(procedure);
        bundle.addEntry().setFullUrl(condition.getId()).setResource(condition);
        bundle.addEntry().setFullUrl(claim.getId()).setResource(claim);


        IParser p = fhirctx.newJsonParser().setPrettyPrint(true);
        String messageString = p.encodeResourceToString(bundle);
        System.out.println("here is the json " + messageString);
        // log.info("Document bundle: {}", ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(document));
        return messageString;
    }

    @Override
    public String buildVhiClaimProfile(PreAuthResponse preAuthResponse) {
        return null;
    }
}
