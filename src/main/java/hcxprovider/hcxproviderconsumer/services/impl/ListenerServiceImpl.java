package hcxprovider.hcxproviderconsumer.services.impl;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.util.BundleBuilder;
import hcxprovider.hcxproviderconsumer.dto.DocumentMaster;
import hcxprovider.hcxproviderconsumer.dto.Message;
import hcxprovider.hcxproviderconsumer.dto.MessageResDTO;
import hcxprovider.hcxproviderconsumer.dto.PreAuthDetails;
import hcxprovider.hcxproviderconsumer.enums.PolicyType;
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
import org.hl7.fhir.r4.model.codesystems.ClaimType;
import org.hl7.fhir.r4.model.codesystems.ProcessPriority;
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
//      File payloadFile = new ClassPathResource("input/claim.txt").getFile();
//      String payload = FileUtils.readFileToString(payloadFile);
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
    public int setSequence(int seq){
        seq = 1;
        return seq;
    }
    @Override
    public String buildClaimFhirProfile(PreAuthRequest preAuthRequest) {
        int insuranceSeq = 1, diagnosisSeq = 1, procedureSeq = 1, supportingInfoSeq = 1, itemSeq = 1, careSeq = 1, detailSeq = 1;
        PreAuthDetails preAuth = preAuthRequest.getPreAuthReq();


        Practitioner practitioner = new Practitioner();
        practitioner.setId("Practitioner/1");
        practitioner.addIdentifier().setValue(preAuth.getClaim().getCreatorId().toString()).setSystem("http://www.acme.com/identifiers/patient");
        practitioner.addName().addGiven(preAuth.getClaimIllnessTreatmentDetails().getDoctorsDetails());
        practitioner.addQualification().getCode().addCoding().setCode(preAuth.getClaimIllnessTreatmentDetails().getDoctorsDetails());

        Organization organization = new Organization();
        organization.setId("Organization/1");
        organization.addIdentifier().setValue(String.valueOf(preAuth.getClaim().getHospitalId())).setSystem("http://www.acme.com/identifiers/patient");
        organization.addContact().getPurpose().addCoding().setCode(preAuth.getClaim().getCityName()).setDisplay("cityName");

        Organization organizationInsurer = new Organization();
        organizationInsurer.setId("Organization/2");
        organizationInsurer.addIdentifier().setValue(String.valueOf(preAuth.getClaim().getInsuranceAgencyId())).setSystem("http://www.acme.com/identifiers/patient");

        Patient patient = new Patient();
        patient.setId("Patient/1");
        patient.addIdentifier().setValue(preAuth.getClaim().getHospitalPatientId()).setSystem("http://www.acme.com/identifiers/patient");
        patient.setBirthDate(preAuth.getClaim().getDob());
        patient.getGenderElement().setValue(AdministrativeGender.valueOf(preAuth.getClaim().getGender()));
        patient.addName().addGiven(preAuth.getClaim().getPatientName());
        patient.addTelecom().setValue(preAuth.getClaim().getPatient_mobile_no()).setSystem(ContactPointSystem.PHONE);
        patient.addContact().addTelecom().setSystem(ContactPointSystem.PHONE).setValue(preAuth.getClaim().getAttendent_mobile_no());
        patient.addContact().addTelecom().setSystem(ContactPointSystem.EMAIL).setValue(preAuth.getClaim().getPatient_email_id());


        Coverage coverage = new Coverage();
        coverage.setId("Coverage/1");
        coverage.setSubscriberId(preAuth.getClaim().getMedicalCardId());
        coverage.setPolicyHolder(new Reference("Patient/1"));
        coverage.addIdentifier().setValue(preAuth.getClaim().getPolicyNumber()).setSystem("https://www.gicofIndia.in/policies");
        coverage.getType().addCoding().setCode(preAuth.getClaim().getPolicyType().toString()).setDisplay("policyType");
        coverage.getPeriod().setEnd(preAuth.getClaim().getPolicyEndDate());
        coverage.getPeriod().setStart(preAuth.getClaim().getPolicyStartDate());
        coverage.addClass_().setValue(preAuth.getClaim().getPolicyName()).getType().addCoding().setCode(preAuth.getClaim().getPolicyName()).setDisplay("PolicyName");

        Meta meta = new Meta();

        Condition condition = new Condition();
        condition.setId("Condition/1");
        condition.setSubject(new Reference("Patient/1"));
        condition.getCode().addCoding().setDisplay("ChronicIllnessDetails").setCode(preAuth.getClaimIllnessTreatmentDetails().getChronicIllnessDetails());
        condition.setRecordedDate(preAuth.getClaimIllnessTreatmentDetails().getDateOfDiagnosis());
        condition.getCode().addCoding().setDisplay("illnessName").setCode(preAuth.getIllness().getIllnessName());
        condition.getCode().addCoding().setDisplay("defaultICDCode").setCode(preAuth.getIllness().getDefaultICDCode());


        Device device = new Device();
        device.setId("Device/1");


        Procedure procedure = new Procedure();
        procedure.setId("Procedure/1");
        procedure.setStatus(Procedure.ProcedureStatus.COMPLETED);
        procedure.setSubject(new Reference("Patient/1"));
        procedure.getCode().addCoding().setDisplay("procedureMethodName").setCode(preAuth.getProcedureMethod().getProcedureMethodName());
        procedure.getCode().addCoding().setDisplay("procedureCode").setCode(preAuth.getProcedureMethod().getProcedureCode());
        procedure.addNote().setText(preAuth.getProcedure().getDescription());
        procedure.getCode().addCoding().setDisplay("name").setCode(preAuth.getProcedure().getName());


        Claim claim = new Claim();
        claim.setUse(Claim.Use.PREAUTHORIZATION);
        claim.setId("Claim/1");
        claim.addIdentifier().setSystem("https://www.tmh.in/hcx-documents").setValue(preAuth.getClaim().getId().toString());
        claim.setEnterer(new Reference("Practitioner/1"));
        claim.setCreated(preAuth.getClaim().getCreatedDate());
        claim.setStatus(Claim.ClaimStatus.ACTIVE);
        claim.setProvider(new Reference("Organization/1"));
        claim.setPatient(new Reference("Patient/1"));
        claim.setInsurer(new Reference("Organization/2"));
        claim.addInsurance().setSequence(insuranceSeq++).setFocal(true).setCoverage(new Reference("Coverage/1"));
        claim.setMeta(meta);
        claim.addIdentifier().setSystem("https://www.gicofIndia.in/policies").setValue(String.valueOf(preAuth.getClaimIllnessTreatmentDetails().getClaimId()));
        claim.addDiagnosis().setSequence(diagnosisSeq++).getDiagnosisReference().setReference("Condition/1");
        claim.addSupportingInfo().setSequence(supportingInfoSeq++).setCategory(new CodeableConcept().setText(preAuth.getClaim().getPolicyInceptionDate().toString())).getTimingDateType().setValue(preAuth.getClaim().getPolicyInceptionDate());
        claim.addSupportingInfo().setSequence(supportingInfoSeq++).setCode(new CodeableConcept(new Coding().setSystem("http://hcxprotocol.io/codes/claim-supporting-info-codes").setCode(preAuth.getClaimIllnessTreatmentDetails().getLineOfTreatmentDetails()))).setCategory(new CodeableConcept(new Coding().setSystem("http://hcxprotocol.io/codes/claim-supporting-info-categories").setCode(preAuth.getClaimIllnessTreatmentDetails().getLineOfTreatmentDetails()))).setValue(new StringType(preAuth.getClaimIllnessTreatmentDetails().getLineOfTreatmentDetails()));


        claim.addProcedure().setSequence(procedureSeq++).getProcedureReference().setReference("Procedure/1");
        claim.addCareTeam().setSequence(careSeq++).getProvider().setReference("Practitioner/1");

        //claim admission details

        claim.addSupportingInfo().setSequence(supportingInfoSeq++).setCategory(new CodeableConcept(new Coding().setSystem("http://hcxprotocol.io/codes/claim-supporting-info-categories").setCode(preAuth.getClaimAdmissionDetails().getAdmissionDate().toString()))).setCode(new CodeableConcept(new Coding().setSystem("http://hcxprotocol.io/codes/claim-supporting-info-codes").setCode(preAuth.getClaimAdmissionDetails().getAdmissionDate().toString()))).getTimingDateType().setValue(preAuth.getClaimAdmissionDetails().getAdmissionDate());

        claim.addSupportingInfo().setSequence(supportingInfoSeq++).setCategory(new CodeableConcept(new Coding().setSystem("http://hcxprotocol.io/codes/claim-supporting-info-categories").setCode(preAuth.getClaimAdmissionDetails().getDischargeDate().toString()))).setCode(new CodeableConcept(new Coding().setSystem("http://hcxprotocol.io/codes/claim-supporting-info-codes").setCode(preAuth.getClaimAdmissionDetails().getDischargeDate().toString()))).getTimingDateType().setValue(preAuth.getClaimAdmissionDetails().getDischargeDate());
        claim.addItem().setSequence(itemSeq++).setProductOrService(new CodeableConcept().setText("roomType")).addDetail().setSequence(setSequence(detailSeq)).setProductOrService(new CodeableConcept().setText("roomType")).getProductOrService().addCoding().setDisplay("roomType").setCode(preAuth.getClaimAdmissionDetails().getRoomType());

        //claim.addItem().setServiced(new IntegerType(preAuth.getClaimAdmissionDetails().getStayDuration()));
        //claim.addSupportingInfo().setSequence(supportingInfoSeq++).setCategory(new CodeableConcept(new Coding().setSystem("http://hcxprotocol.io/codes/claim-supporting-info-categories").setCode(String.valueOf(preAuth.getClaimAdmissionDetails().getIcuStayDuration())))).setCode(new CodeableConcept(new Coding().setSystem("http://hcxprotocol.io/codes/claim-supporting-info-codes").setCode(String.valueOf(preAuth.getClaimAdmissionDetails().getIcuStayDuration())))).getTimingDateType().setValueAsString(String.valueOf(preAuth.getClaimAdmissionDetails().getIcuStayDuration()));

        claim.addSupportingInfo().setSequence(supportingInfoSeq++).setCategory(new CodeableConcept(new Coding().setSystem("http://hcxprotocol.io/codes/claim-supporting-info-categories").setCode(String.valueOf(preAuth.getClaimAdmissionDetails().isIcuStay())))).setValue(new BooleanType(preAuth.getClaimAdmissionDetails().isIcuStay()));

        //document master list
        claim.addItem().setSequence(itemSeq++).setProductOrService(new CodeableConcept().setText("CostEstimation")).addDetail().setSequence(setSequence(detailSeq)).setProductOrService(new CodeableConcept().setText("CostEstimation")).getNet().setUserData("CostEstimation", preAuth.getClaimAdmissionDetails().getCostEstimation());
        claim.addItem().setSequence(itemSeq++).setProductOrService(new CodeableConcept().setText("CostEstimation")).addDetail().setSequence(setSequence(detailSeq)).setProductOrService(new CodeableConcept().setText("CostEstimation")).getQuantity().setUserData("CostEstimation", preAuth.getClaimAdmissionDetails().getCostEstimation());
        claim.addItem().setSequence(itemSeq++).setProductOrService(new CodeableConcept().setText("CostEstimation")).addDetail().setSequence(setSequence(detailSeq)).setProductOrService(new CodeableConcept().setText("CostEstimation")).getCategory().addCoding().setDisplay("costEstimation").setCode(preAuth.getClaimAdmissionDetails().getCostEstimation());

        for (DocumentMaster doc : preAuth.getDocumentMasterList()) {
            claim.addSupportingInfo().setSequence(supportingInfoSeq++).setCategory(new CodeableConcept(new Coding().setSystem("http://hcxprotocol.io/codes/claim-supporting-info-categories").setCode(doc.getDocumentType()))).setValue(new StringType(doc.getDocumentType()));
            claim.addSupportingInfo().setSequence(supportingInfoSeq++).setCategory(new CodeableConcept(new Coding().setSystem("http://hcxprotocol.io/codes/claim-supporting-info-categories").setCode(doc.getFileType()))).setValue(new StringType(doc.getFileType()));
            claim.addSupportingInfo().setSequence(supportingInfoSeq++).setCategory(new CodeableConcept(new Coding().setSystem("http://hcxprotocol.io/codes/claim-supporting-info-categories").setCode(doc.getFileName()))).setValue(new StringType(doc.getFileName()));
            claim.addSupportingInfo().setSequence(supportingInfoSeq++).setCategory(new CodeableConcept(new Coding().setSystem("http://hcxprotocol.io/codes/claim-supporting-info-categories").setCode(doc.getStorageFileName()))).setValue(new StringType(doc.getStorageFileName()));
            claim.addSupportingInfo().setSequence(supportingInfoSeq++).setCategory(new CodeableConcept(new Coding().setSystem("http://hcxprotocol.io/codes/claim-supporting-info-categories").setCode(doc.getNote()))).setValue(new StringType(doc.getNote()));
            // claim.addSupportingInfo().setSequence(supportingInfoSeq++).setCategory(new CodeableConcept(new Coding().setSystem("http://hcxprotocol.io/codes/claim-supporting-info-categories").setCode(String.valueOf(doc.getDocumentStatus())))).setValue(new IntegerType(doc.getDocumentStatus()));
            claim.addSupportingInfo().setSequence(supportingInfoSeq++).setCategory(new CodeableConcept(new Coding().setSystem("http://hcxprotocol.io/codes/claim-supporting-info-categories").setCode(String.valueOf(doc.isFileSupported())))).setValue(new BooleanType(doc.isFileSupported()));
        }

        // hospitalServiceType completed
        claim.addItem().setSequence(itemSeq++).setProductOrService(new CodeableConcept().setText("roomType")).addDetail().setSequence(setSequence(detailSeq)).setProductOrService(new CodeableConcept().setText("roomType")).getCategory().addCoding().setDisplay("roomType").setCode(preAuth.getHospitalServiceType().getRoomType());

        claim.addItem().setSequence(itemSeq++).setProductOrService(new CodeableConcept().setText("roomTariffPerDay")).getUnitPrice().setCurrency("INR").setValue(preAuth.getHospitalServiceType().getRoomTariffPerDay());

        claim.setType(new CodeableConcept(new Coding().setCode(ClaimType.INSTITUTIONAL.toCode()).setSystem("http://terminology.hl7.org/CodeSystem/claim-type")));

        claim.setPriority(new CodeableConcept(new Coding().setSystem("http://terminology.hl7.org/CodeSystem/processpriority").setCode(ProcessPriority.NORMAL.toCode())));
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
        bundle.addEntry().setFullUrl(practitioner.getId()).setResource(practitioner);
        bundle.addEntry().setFullUrl(organization.getId()).setResource(organization);
        bundle.addEntry().setFullUrl(organizationInsurer.getId()).setResource(organizationInsurer);
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
