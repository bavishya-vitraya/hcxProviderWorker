package hcxprovider.hcxproviderconsumer.services.impl;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import hcxprovider.hcxproviderconsumer.dto.*;
import hcxprovider.hcxproviderconsumer.model.ClaimRequest;
import hcxprovider.hcxproviderconsumer.model.CoverageEligibilityRequest;
import hcxprovider.hcxproviderconsumer.model.PreAuthRequest;
import hcxprovider.hcxproviderconsumer.model.PreAuthResponse;
import hcxprovider.hcxproviderconsumer.repository.ClaimRequestRepo;
import hcxprovider.hcxproviderconsumer.repository.CoverageEligibilityRequestRepo;
import hcxprovider.hcxproviderconsumer.repository.PreAuthRequestRepo;
import hcxprovider.hcxproviderconsumer.repository.PreAuthResponseRepo;
import hcxprovider.hcxproviderconsumer.services.ListenerService;
import hcxprovider.hcxproviderconsumer.utils.Constants;
import io.hcxprotocol.impl.HCXOutgoingRequest;
import io.hcxprotocol.init.HCXIntegrator;
import io.hcxprotocol.utils.Operations;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.codesystems.Adjudication;
import org.hl7.fhir.r4.model.codesystems.ClaimType;
import org.hl7.fhir.r4.model.codesystems.ProcessPriority;
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

    @Autowired
    PreAuthResponseRepo preAuthResponseRepo;

    FhirContext fhirctx = FhirContext.forR4();
    IParser parser = fhirctx.newJsonParser().setPrettyPrint(true);

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
        String reqType = msg.getMessageType();

        Operations operation = null;
        if (reqType.equalsIgnoreCase(Constants.COVERAGE_ELIGIBILITY)) {
            coverageEligibilityRequest = coverageEligibilityRequestRepo.findCoverageEligibilityRequestById(msg.getReferenceId());
            log.info("CoverageEligibility:{}", coverageEligibilityRequest);
            operation = Operations.COVERAGE_ELIGIBILITY_CHECK;
        } else if (reqType.equalsIgnoreCase(Constants.CLAIM)) {
            claimRequest = claimRequestRepo.findClaimRequestById(msg.getReferenceId());
            log.info("ClaimReq:{}", claimRequest);
            operation = Operations.CLAIM_SUBMIT;
            payload = buildClaimFhirProfile(preAuthRequest);
        } else if (reqType.equalsIgnoreCase(Constants.PRE_AUTH)) {
            preAuthRequest = preAuthRequestRepo.findPreAuthRequestById(msg.getReferenceId());
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
    public boolean vhiGenerateResponse(Message msg) throws Exception {
        String resType = msg.getMessageType();
        PreAuthResponse vhiResponse = new PreAuthResponse();
        Operations operation = null;
        if (resType.equalsIgnoreCase(Constants.PRE_AUTH_RESPONSE)) {
            vhiResponse = preAuthResponseRepo.findPreAuthResponseById(msg.getReferenceId());
            log.info("PreAuthResponse:{}", vhiResponse);
            PreAuthVhiResponse preAuthVhiResponse = buildVhiClaimProfile(vhiResponse.getFhirPayload());
            vhiResponse.setPreAuthResponse(preAuthVhiResponse);
            preAuthResponseRepo.save(vhiResponse);

        }
       return true;
    }
    @Override
    public PreAuthVhiResponse buildVhiClaimProfile(String fhirPayload) {
        PreAuthVhiResponse result = new PreAuthVhiResponse();
        Bundle bundle = parser.parseResource(Bundle.class, fhirPayload);
        ClaimResponse claimResponse = new ClaimResponse();
        for(Bundle.BundleEntryComponent entryComponent: bundle.getEntry()){
            String resourceType = entryComponent.getResource().getResourceType().toString();
            log.info(String.valueOf(entryComponent.getResource().getResourceType()));
            if(resourceType.equalsIgnoreCase(Constants.CLAIM_RESPONSE_RESOURCE)) {
                claimResponse = (ClaimResponse) entryComponent.getResource();
                result.setClaimNumber(claimResponse.getPreAuthRef());
                result.setClaimStatusInString(claimResponse.getDisposition());
                String encodedAttachment = claimResponse.getProcessNote().get(0).getText();
                byte[] decodedBytes = Base64.getDecoder().decode(encodedAttachment);
                String decodedAttachment = new String(decodedBytes);
                AttachmentResDTO attachmentResDTO = new Gson().fromJson(decodedAttachment, new TypeToken<AttachmentResDTO>() {
                }.getType());

                result.setQuery(attachmentResDTO.getQuery());
                result.setFiles(attachmentResDTO.getFiles());

                result.setApprovedAmount(claimResponse.getTotal().get(0).getAmount().getValue());
                if (claimResponse.getOutcome().toString().equalsIgnoreCase("COMPLETE")) {
                    result.setClaimStatus(PreAuthVhiResponse.AdjudicationClaimStatus.APPROVED);
                }

            }

        }
        log.info("vhi result{}", new Gson().toJson(result));
        log.info("Parsed Claim Response from Fhir:, {}", claimResponse.getDisposition());
        return result;
    }
    public int setSequence(int seq){
        seq = 1;
        return seq;
    }
    @Override
    public String buildClaimFhirProfile(PreAuthRequest preAuthRequest) {
        int insuranceSeq = 1, diagnosisSeq = 1, procedureSeq = 1, supportingInfoSeq = 1, itemSeq = 1, careSeq = 1, detailSeq = 1;
        PreAuthDetails preAuth = preAuthRequest.getPreAuthReq();
        AttachmentDTO attachmentDTO = new AttachmentDTO();
        Claim claim = new Claim();
        Bundle bundle = new Bundle();


        Practitioner practitioner = new Practitioner();
        practitioner.setId("Practitioner/1");
        practitioner.addIdentifier().setValue(preAuth.getClaim().getCreatorId().toString()).setSystem("http://www.acme.com/identifiers/patient").setType(new CodeableConcept(new Coding().setSystem("http://terminology.hl7.org/CodeSystem/v2-0203").setCode("PLAC").setDisplay("Placer Identifier")));
        List<DoctorDetailsDto> doctorDetailsDtoList = new Gson().fromJson(preAuth.getClaimIllnessTreatmentDetails().getDoctorsDetails(), new TypeToken<List<DoctorDetailsDto>>() {
        }.getType());
        for (DoctorDetailsDto doctor : doctorDetailsDtoList) {
            practitioner.addName().addGiven(doctor.getDoctorName());
            practitioner.addQualification().getCode().setText(doctor.getQualification()).addCoding().setCode("BS").setSystem("http://terminology.hl7.org/CodeSystem/v2-0360|2.7").setDisplay("Qualification");
        }

        Organization organization = new Organization();
        organization.setId("ProviderOrganization/1");
        organization.addIdentifier().setValue(String.valueOf(preAuth.getClaim().getHospitalId())).setSystem("http://www.acme.com/identifiers/patient").setType(new CodeableConcept(new Coding().setCode("PRN").setSystem("http://terminology.hl7.org/CodeSystem/v2-0203").setDisplay("Provider number")));
        organization.addContact().getPurpose().setText(preAuth.getClaim().getCityName()).addCoding().setCode("PATINF").setSystem("http://terminology.hl7.org/CodeSystem/contactentity-type").setDisplay("Patient");

        Organization organizationInsurer = new Organization();
        organizationInsurer.setId("InsurerOrganization/2");
        organizationInsurer.addIdentifier().setValue(String.valueOf(preAuth.getClaim().getInsuranceAgencyId())).setSystem("http://www.acme.com/identifiers/patient");

        Patient patient = new Patient();
        patient.setId("Patient/1");
        patient.addIdentifier().setValue(preAuth.getClaim().getHospitalPatientId()).setSystem("http://www.acme.com/identifiers/patient");
        patient.setBirthDate(preAuth.getClaim().getDob());
        patient.getGenderElement().setValue(AdministrativeGender.valueOf(preAuth.getClaim().getGender()));
        patient.addName().addGiven(preAuth.getClaim().getPatientName());
        patient.addTelecom().setValue(preAuth.getClaim().getPatient_mobile_no()).setSystem(ContactPointSystem.PHONE);
        patient.addContact().addTelecom().setSystem(ContactPointSystem.PHONE).setValue(preAuth.getClaim().getAttendent_mobile_no());
        patient.addTelecom().setSystem(ContactPointSystem.EMAIL).setValue(preAuth.getClaim().getPatient_email_id());


        Coverage coverage = new Coverage();
        coverage.setId("Coverage/1");
        coverage.setStatus(Coverage.CoverageStatus.ACTIVE);
        coverage.setBeneficiary(new Reference("Patient/1"));
        coverage.addPayor().setReference("ProviderOrganization/1");
        coverage.setSubscriberId(preAuth.getClaim().getMedicalCardId());
        coverage.setPolicyHolder(new Reference("Patient/1"));
        coverage.addIdentifier().setValue(preAuth.getClaim().getPolicyNumber()).setSystem("https://www.gicofIndia.in/policies");
        coverage.getType().setText(preAuth.getClaim().getPolicyType().toString()).addCoding().setCode("HIP").setDisplay("health insurance plan policy");
        coverage.getPeriod().setStart(preAuth.getClaim().getPolicyStartDate()).setEnd(preAuth.getClaim().getPolicyEndDate());
        coverage.addClass_().setValue(preAuth.getClaim().getPolicyName()).setType(new CodeableConcept(new Coding().setCode("class").setSystem("http://terminology.hl7.org/CodeSystem/coverage-class").setDisplay("Class")));

        Meta meta = new Meta();

        Condition condition = new Condition();
        condition.setId("Condition/1");
        condition.setSubject(new Reference("Patient/1"));
        condition.setRecordedDate(preAuth.getClaimIllnessTreatmentDetails().getDateOfDiagnosis());



        Device device = new Device();
        device.setId("Device/1");


        Procedure procedure = new Procedure();
        procedure.setId("Procedure/1");
        procedure.setStatus(Procedure.ProcedureStatus.COMPLETED);
        procedure.setSubject(new Reference("Patient/1"));
        procedure.addNote().setText(preAuth.getProcedure().getDescription());
        procedure.setCode(new CodeableConcept(new Coding().setSystem("http://snomed.info/sct").setDisplay("Percutaneous transluminal angioplasty").setCode("5431005")).setText(preAuth.getProcedure().getName()));



        claim.setUse(Claim.Use.PREAUTHORIZATION);
        claim.setId("Claim/1");
        claim.addIdentifier().setSystem("https://www.tmh.in/hcx-documents").setValue(preAuth.getClaim().getId().toString());
        claim.setEnterer(new Reference("Practitioner/1"));
        claim.setCreated(preAuth.getClaim().getCreatedDate());
        claim.setStatus(Claim.ClaimStatus.ACTIVE);
        claim.setProvider(new Reference("Organization/1"));
        claim.setPatient(new Reference("Patient/1"));
        claim.setInsurer(new Reference("InsurerOrganization/2"));
        claim.addInsurance().setSequence(insuranceSeq++).setFocal(true).setCoverage(new Reference("Coverage/1"));
        claim.setMeta(meta);
        //   claim.addIdentifier().setSystem("https://www.gicofIndia.in/policies").setValue(String.valueOf(preAuth.getClaimIllnessTreatmentDetails().getClaimId()));
        claim.addDiagnosis().setSequence(diagnosisSeq++).getDiagnosisReference().setReference("Condition/1");
        claim.addSupportingInfo().setSequence(supportingInfoSeq++).setCategory(new CodeableConcept(new Coding().setSystem("http://hcxprotocol.io/codes/claim-supporting-info-categories").setCode("INF").setDisplay("PolicyInceptionDate"))).getTimingDateType().setValue(preAuth.getClaim().getPolicyInceptionDate());
        claim.addSupportingInfo().setSequence(supportingInfoSeq++).setCode(new CodeableConcept(new Coding().setSystem("http://hcxprotocol.io/codes/claim-supporting-info-codes").setCode("TRD-3").setDisplay("Surgical Management"))).setCategory(new CodeableConcept(new Coding().setSystem("http://hcxprotocol.io/codes/claim-supporting-info-categories").setCode("TRD").setDisplay("Treatment detail"))).setValue(new StringType(preAuth.getClaimIllnessTreatmentDetails().getLineOfTreatmentDetails()));


        claim.addProcedure().setSequence(procedureSeq++).getProcedureReference().setReference("Procedure/1");
        claim.addCareTeam().setSequence(careSeq++).getProvider().setReference("Practitioner/1");

        //claim admission details

        claim.addSupportingInfo().setSequence(supportingInfoSeq++).setCategory(new CodeableConcept(new Coding().setSystem("http://hcxprotocol.io/codes/claim-supporting-info-categories").setCode("ONS").setDisplay("Period, start or end dates of aspects of the Condition"))).setCode(new CodeableConcept(new Coding().setSystem("http://hcxprotocol.io/codes/claim-supporting-info-codes").setCode("ONS-1").setDisplay("Admission date -Discharge date"))).getTimingDateType().setValue(preAuth.getClaimAdmissionDetails().getAdmissionDate());

        claim.addSupportingInfo().setSequence(supportingInfoSeq++).setCategory(new CodeableConcept(new Coding().setSystem("http://hcxprotocol.io/codes/claim-supporting-info-categories").setCode("ONS").setDisplay("Period, start or end dates of aspects of the Condition"))).setCode(new CodeableConcept(new Coding().setSystem("http://hcxprotocol.io/codes/claim-supporting-info-codes").setCode("ONS-2").setDisplay("Discharge start-discharge end time"))).getTimingDateType().setValue(preAuth.getClaimAdmissionDetails().getDischargeDate());
        claim.addItem().setSequence(itemSeq++).
                setProductOrService(new CodeableConcept(new Coding().setCode("99555").setSystem("http://terminology.hl7.org/CodeSystem/ex-USCLS").setDisplay("room type"))).
                addDetail().setSequence(detailSeq++).
                setProductOrService(new CodeableConcept(new Coding().setCode("99555").setSystem("http://terminology.hl7.org/CodeSystem/ex-USCLS").setDisplay("room type"))
                        .setText(preAuth.getClaimAdmissionDetails().getRoomType())).setCategory(new CodeableConcept(new Coding().setDisplay("Room Charges").setSystem("https://irdai.gov.in/benefit-billing-subgroup-code").setCode("101000")));
        claim.addSupportingInfo().setSequence(supportingInfoSeq++).setCode(new CodeableConcept(new Coding().setDisplay("PatientICUStay").setSystem("http://hcxprotocol.io/codes/claim-supporting-info-codes").setCode("ONS-6"))).setCategory(new CodeableConcept(new Coding().setSystem("http://hcxprotocol.io/codes/claim-supporting-info-categories").setCode("ONS").setDisplay("Period, start or end dates of aspects of the Condition"))).setValue(new BooleanType(preAuth.getClaimAdmissionDetails().isIcuStay()));

        //document master list
//        claim.addItem().setSequence(itemSeq++).setProductOrService(new CodeableConcept().setText("CostEstimation")).addDetail().setSequence(setSequence(detailSeq)).setProductOrService(new CodeableConcept().setText("CostEstimation")).getNet().setUserData("CostEstimation", preAuth.getClaimAdmissionDetails().getCostEstimation());
//        claim.addItem().setSequence(itemSeq++).setProductOrService(new CodeableConcept().setText("CostEstimation")).addDetail().setSequence(setSequence(detailSeq)).setProductOrService(new CodeableConcept().setText("CostEstimation")).getQuantity().setUserData("CostEstimation", preAuth.getClaimAdmissionDetails().getCostEstimation());
//        claim.addItem().setSequence(itemSeq++).setProductOrService(new CodeableConcept().setText("CostEstimation")).addDetail().setSequence(setSequence(detailSeq)).setProductOrService(new CodeableConcept().setText("CostEstimation")).getCategory().addCoding().setDisplay("costEstimation").setCode(preAuth.getClaimAdmissionDetails().getCostEstimation());


        // hospitalServiceType completed
        claim.addItem().
                setSequence(itemSeq++).
                setProductOrService(new CodeableConcept(new Coding().setCode("99555").setSystem("http://terminology.hl7.org/CodeSystem/ex-USCLS").setDisplay("Expense"))).getUnitPrice().setCurrency("INR").setValue(preAuth.getHospitalServiceType().getRoomTariffPerDay());

        claim.setType(new CodeableConcept(new Coding().setCode(ClaimType.INSTITUTIONAL.toCode()).setSystem("http://terminology.hl7.org/CodeSystem/claim-type")));

        claim.setPriority(new CodeableConcept(new Coding().setSystem("http://terminology.hl7.org/CodeSystem/processpriority").setCode(ProcessPriority.NORMAL.toCode())));


        //Attachment
        attachmentDTO.setServiceTypeId(preAuth.getServiceTypeId());
        attachmentDTO.setDeleted(preAuth.getClaim().isDeleted());
        attachmentDTO.setUpdatedDate(preAuth.getClaim().getUpdatedDate());
        attachmentDTO.setState(preAuth.getClaim().getState());
        attachmentDTO.setStatus(preAuth.getClaim().getStatus());
        attachmentDTO.setAge(preAuth.getClaim().getAge());
        attachmentDTO.setProductCode(preAuth.getClaim().getProductCode());
        attachmentDTO.setMedicalEventId(preAuth.getClaim().getMedicalEventId());
        attachmentDTO.setProcedureCorporateMappingId(preAuth.getClaimIllnessTreatmentDetails().getProcedureCorporateMappingId());
        attachmentDTO.setProcedureId(preAuth.getClaimIllnessTreatmentDetails().getProcedureId());
        attachmentDTO.setLeftImplant(preAuth.getClaimIllnessTreatmentDetails().getLeftImplant());
        attachmentDTO.setRightImplant(preAuth.getClaimIllnessTreatmentDetails().getRightImplant());
        attachmentDTO.setHospitalServiceTypeId(preAuth.getClaimAdmissionDetails().getHospitalServiceTypeId());
        attachmentDTO.setStayDuration(preAuth.getClaimAdmissionDetails().getStayDuration());
        attachmentDTO.setCostEstimation(preAuth.getClaimAdmissionDetails().getCostEstimation());
        attachmentDTO.setPackageAmount(preAuth.getClaimAdmissionDetails().getPackageAmount());
        attachmentDTO.setIcuStayDuration(preAuth.getClaimAdmissionDetails().getIcuStayDuration());
        attachmentDTO.setIcuServiceTypeId(preAuth.getClaimAdmissionDetails().getIcuServiceTypeId());
        attachmentDTO.setVitrayaRoomCategory(String.valueOf(preAuth.getHospitalServiceType().getVitrayaRoomCategory()));
        attachmentDTO.setInsurerRoomType(preAuth.getHospitalServiceType().getInsurerRoomType());
        attachmentDTO.setSinglePrivateAC(preAuth.getHospitalServiceType().isSinglePrivateAC());
        attachmentDTO.setServiceType(preAuth.getHospitalServiceType().getServiceType());
        attachmentDTO.setParentTableId(preAuth.getClaim().getId());
        attachmentDTO.setIllnessCategoryId(preAuth.getIllness().getIllnessCategoryId());
        attachmentDTO.setDocumentMasterList(preAuth.getDocumentMasterList());
        attachmentDTO.setIllnessName(preAuth.getIllness().getIllnessName());
        attachmentDTO.setDefaultICDCode(preAuth.getIllness().getDefaultICDCode());
        attachmentDTO.setChronicIllnessDetailsJSON(preAuth.getClaimIllnessTreatmentDetails().getChronicIllnessDetailsJSON());

        String attachmentString = new Gson().toJson(attachmentDTO);
        log.info("attachmentString{}", attachmentString);
        String encodedAttachement = Base64.getUrlEncoder().encodeToString(attachmentString.getBytes());
        claim.addSupportingInfo().setCategory(new CodeableConcept(new Coding().setSystem("http://hcxprotocol.io/codes/claim-supporting-info-categories").setDisplay("attachment.json"))).setSequence(supportingInfoSeq++).setValue(new StringType(encodedAttachement));

        Composition composition = new Composition();
        composition.setId("composition/" + UUID.randomUUID().toString());
        composition.setStatus(Composition.CompositionStatus.FINAL);
        composition.getType().addCoding().setSystem("https://www.hcx.org/document-type").setCode("HcxClaimRequest");
        composition.addAuthor().setReference("Organization/1");
        composition.setDate(new Date());
        composition.setTitle("Claim Request");
        composition.addSection().addEntry().setReference("Claim/1");


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
        bundle.addEntry().setFullUrl(coverage.getId()).setResource(coverage);
        bundle.addEntry().setFullUrl(claim.getId()).setResource(claim);

        String messageString = parser.encodeResourceToString(bundle);
        System.out.println("here is the json " + messageString);
        return messageString;
    }

   /* @Override
    public String buildClaimResponseFhirProfile(PreAuthResponse preAuthResponse) {
        PreAuthVhiResponse preAuthVhiResponse = preAuthResponse.getPreAuthResponse();

        Patient patient = new Patient();// should fetch from claim request
        patient.setId("Patient/1");

        Organization organization = new Organization(); // should fetch from claim request
        organization.setId("organization/1");
        organization.setName("Test-HOS01");

        Claim claimRequest = new Claim(); // should fetch from claim request
        claimRequest.setId("Claim/1");
        claimRequest.setUse(Claim.Use.PREAUTHORIZATION);
        claimRequest.setId("Claim/1");
        claimRequest.setCreated(new Date());
        claimRequest.setStatus(Claim.ClaimStatus.ACTIVE);
        claimRequest.setType(new CodeableConcept(new Coding().setCode(ClaimType.INSTITUTIONAL.toCode()).setSystem("http://terminology.hl7.org/CodeSystem/claim-type")));
        claimRequest.setPriority(new CodeableConcept(new Coding().setSystem("http://terminology.hl7.org/CodeSystem/processpriority").setCode(ProcessPriority.NORMAL.toCode())));
        claimRequest.setPatient(new Reference(patient.getId()));
        claimRequest.setProvider(new Reference(organization.getId()));
        claimRequest.addInsurance().setSequence(1).setFocal(true).setCoverage(new Reference("Coverage/1"));


        ClaimResponse claimResponse = new ClaimResponse();
        claimResponse.setId("ClaimResponse/1");
        claimResponse.addIdentifier().setValue(preAuthVhiResponse.getClaimNumber());
        claimResponse.setPreAuthRef(preAuthVhiResponse.getClaimNumber());
        claimResponse.setOutcome(ClaimResponse.RemittanceOutcome.COMPLETE); // no approved enum provided
        claimResponse.setDisposition(preAuthVhiResponse.getClaimStatusInString());
        claimResponse.addProcessNote().setText(preAuthVhiResponse.getQuery());
        claimResponse.addTotal().setAmount(new Money().setCurrency("INR").setValue(preAuthVhiResponse.getApprovedAmount())).setCategory(new CodeableConcept(new Coding().setCode(Adjudication.ELIGIBLE.toCode()).setSystem("http://terminology.hl7.org/CodeSystem/adjudication")));
        claimResponse.setStatus(ClaimResponse.ClaimResponseStatus.ACTIVE);
        claimResponse.setType(new CodeableConcept(new Coding().setCode(ClaimType.INSTITUTIONAL.toCode()).setSystem("http://terminology.hl7.org/CodeSystem/claim-type")));
        claimResponse.setUse(ClaimResponse.Use.PREAUTHORIZATION);
        claimResponse.setCreated(new Date());
        claimResponse.setPatient(new Reference(patient.getId()));
        claimResponse.setRequestor(new Reference(organization.getId()));
        claimResponse.setInsurer(new Reference(organization.getId()));
        claimResponse.setRequest(new Reference(claimRequest.getId()));


        Composition composition= new Composition();
        composition.setId("composition/" + UUID.randomUUID().toString());
        composition.setStatus(Composition.CompositionStatus.FINAL);
        composition.getType().addCoding().setCode("HCXClaimResponse").setSystem("https://hcx.org/document-types").setDisplay("Claim Response");
        composition.setDate(new Date());
        composition.addAuthor().setReference("Organization/1");
        composition.setTitle("Claim Response");
        composition.addSection().addEntry().setReference("ClaimResponse/1");

        FhirContext fhirctx = FhirContext.forR4();
        Bundle bundle = new Bundle();
        bundle.setId(UUID.randomUUID().toString());
        bundle.setType(Bundle.BundleType.DOCUMENT);
        bundle.getIdentifier().setSystem("https://www.tmh.in/bundle").setValue(bundle.getId());
        bundle.setTimestamp(new Date());
        bundle.addEntry().setFullUrl(composition.getId()).setResource(composition);
        bundle.addEntry().setFullUrl(claimRequest.getId()).setResource(claimRequest);
        bundle.addEntry().setFullUrl(patient.getId()).setResource(patient);
        bundle.addEntry().setFullUrl(organization.getId()).setResource(organization);
        bundle.addEntry().setFullUrl(claimResponse.getId()).setResource(claimResponse);

        IParser p = fhirctx.newJsonParser().setPrettyPrint(true);
        String messageString = p.encodeResourceToString(bundle);
        System.out.println("here is the json " + messageString);
        return messageString;
    }*/
}
