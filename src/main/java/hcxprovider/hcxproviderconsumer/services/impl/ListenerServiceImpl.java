package hcxprovider.hcxproviderconsumer.services.impl;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import hcxprovider.hcxproviderconsumer.dto.*;
import hcxprovider.hcxproviderconsumer.enums.Status;
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
import io.hcxprotocol.utils.JSONUtils;
import io.hcxprotocol.utils.Operations;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.ContactPoint.ContactPointSystem;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.codesystems.ClaimType;
import org.hl7.fhir.r4.model.codesystems.ProcessPriority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

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
        try (InputStream inputStream = getClass().getResourceAsStream("/keys/vitraya-mock-provider-private-key.pem");
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String privateKey = reader.lines()
                    .collect(Collectors.joining(System.lineSeparator()));
            config.put("protocolBasePath", "http://staging-hcx.swasth.app/api/v0.7");
            config.put("authBasePath", authBasePath);
            config.put("participantCode",participantCode);
            config.put("username", username);
            config.put("password",password);
            config.put("encryptionPrivateKey", privateKey);
            config.put("igUrl", igUrl);
        }
        catch (Exception e) {
        log.error("exception in loading file", e);
        }
        return config;
    }
    @Override
    public boolean hcxGenerateRequest(Message msg) throws Exception {
        String payload = null;
        CoverageEligibilityRequest coverageEligibilityRequest = new CoverageEligibilityRequest();
        ClaimRequest claimRequest = new ClaimRequest();
        PreAuthRequest preAuthRequest = new PreAuthRequest();
        String reqType = msg.getMessageType();
        Operations operation = null;
        HCXIntegrator.init(setConfig());
        Map<String, Object> output = new HashMap<>();
        Map<String, Object> responseObject = new HashMap<>();
        HCXOutgoingRequest hcxOutgoingRequest = new HCXOutgoingRequest();
        Boolean response = false;
        if (reqType.equalsIgnoreCase(Constants.COVERAGE_ELIGIBILITY)) {
            try {
                coverageEligibilityRequest = coverageEligibilityRequestRepo.findCoverageEligibilityRequestById(msg.getReferenceId());
                log.info("CoverageEligibility:{}", coverageEligibilityRequest);
            }
            catch(Exception e){
                log.error("error in fetching coverage request", e);
            }
            operation = Operations.COVERAGE_ELIGIBILITY_CHECK;
        } else if (reqType.equalsIgnoreCase(Constants.CLAIM)) {
            try {
                claimRequest = claimRequestRepo.findClaimRequestById(msg.getReferenceId());
                log.info("ClaimReq {}", claimRequest);
            }
            catch(Exception e){
                log.error("error in fetching claim request", e);
            }
            operation = Operations.CLAIM_SUBMIT;
        } else if (reqType.equalsIgnoreCase(Constants.PRE_AUTH)) {

            try {
                preAuthRequest = preAuthRequestRepo.findPreAuthRequestById(msg.getReferenceId());
                log.info("PreAuthReq:{}", preAuthRequest);
            }
            catch(Exception e){
                log.error("error in fetching preAuth request", e);
            }
            operation = Operations.PRE_AUTH_SUBMIT;
            payload = buildClaimFhirProfile(preAuthRequest);
            response = hcxOutgoingRequest.generate(payload, operation, recipientCode, output);
            /*Map<String, Object> hcxerror = new HashMap<>();
            Map<String, Object> hcxheaders = new HashMap<>();
            Map<String, Object> hcxresponse = new HashMap<>();
            if(!hcxOutgoingRequest.createHeader(recipientCode,null,null,hcxheaders)){
                output.putAll(hcxerror);
            } else if (!hcxOutgoingRequest.encryptPayload(hcxheaders, payload, output)) {
                output.putAll(hcxerror);
            }else {
                response = hcxOutgoingRequest.initializeHCXCall(JSONUtils.serialize(output), operation, hcxresponse);
                output.putAll(hcxresponse);
            }*/
            log.info("response {} ",response);
            log.info("{}",output);
            responseObject = (Map<String, Object>) output.get("responseObj");
            String crid = (String) responseObject.get("correlation_id");
            preAuthRequest.setCorrelationId(crid);
            preAuthRequest.setStatus(Status.PROCESSED);
            preAuthRequestRepo.save(preAuthRequest);
            log.info("responseObj {} ",responseObject);
            log.info("correlation id"+crid);
        }
        return response;
    }


    @Override
    public boolean vhiGenerateResponse(Message msg)  {
        String resType = msg.getMessageType();
        PreAuthResponse vhiResponse = new PreAuthResponse();
        Operations operation = null;
        if (resType.equalsIgnoreCase(Constants.PRE_AUTH_RESPONSE)) {
            try {
                vhiResponse = preAuthResponseRepo.findPreAuthResponseById(msg.getReferenceId());
                log.info("PreAuthResponse:{}", vhiResponse);
                PreAuthVhiResponse preAuthVhiResponse = buildVhiClaimProfile(vhiResponse.getFhirPayload());
                vhiResponse.setPreAuthResponse(preAuthVhiResponse);
                preAuthResponseRepo.save(vhiResponse);
            }
            catch (Exception e ){
                e.printStackTrace();
            }

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
            if(resourceType.equalsIgnoreCase(Constants.CLAIM_RESOURCE)) {
                Claim claimRequest = (Claim) entryComponent.getResource();
                result.setHospitalReferenceId(Long.valueOf(claimRequest.getIdentifier().get(0).getValue()));
            }
        }
        log.info("vhi result{}", new Gson().toJson(result));
        log.info("Parsed Claim Response from Fhir {}", claimResponse.getDisposition());
        return result;
    }
    public int setSequence(int seq){
        seq = 1;
        return seq;
    }
    @Override
    public String buildClaimFhirProfile(PreAuthRequest preAuthRequest) throws ParseException {
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
        coverage.getType().setText(preAuth.getClaim().getPolicyType().toString()).addCoding().setCode("HIP").setDisplay("health insurance plan policy").setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode");
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
        //claim.addIdentifier().setSystem("https://www.gicofIndia.in/policies").setValue(String.valueOf(preAuth.getClaimIllnessTreatmentDetails().getClaimId()));
        claim.addDiagnosis().setSequence(diagnosisSeq++).getDiagnosisReference().setReference("Condition/1");
        claim.addSupportingInfo().setSequence(supportingInfoSeq++).setCode(new CodeableConcept(new Coding().setSystem("http://hcxprotocol.io/codes/claim-supporting-info-codes").setCode("TRD-3").setDisplay("Surgical Management"))).setCategory(new CodeableConcept(new Coding().setSystem("http://hcxprotocol.io/codes/claim-supporting-info-categories").setCode("TRD").setDisplay("Treatment detail"))).setValue(new StringType(preAuth.getClaimIllnessTreatmentDetails().getLineOfTreatmentDetails()));


        claim.addProcedure().setSequence(procedureSeq++).getProcedureReference().setReference("Procedure/1");
        claim.addCareTeam().setSequence(careSeq++).getProvider().setReference("Practitioner/1");

        //claim admission details

       claim.addSupportingInfo().setSequence(supportingInfoSeq++).setCategory(new CodeableConcept(new Coding().setSystem("http://hcxprotocol.io/codes/claim-supporting-info-categories").setCode("ONS").setDisplay("Period, start or end dates of aspects of the Condition"))).setCode(new CodeableConcept(new Coding().setSystem("http://hcxprotocol.io/codes/claim-supporting-info-codes").setCode("ONS-1").setDisplay("Admission date -Discharge date"))).getTimingPeriod().setStart(preAuth.getClaimAdmissionDetails().getAdmissionDate()).setEnd(preAuth.getClaimAdmissionDetails().getDischargeDate());

       //claim.addSupportingInfo().setSequence(supportingInfoSeq++).setCategory(new CodeableConcept(new Coding().setSystem("http://hcxprotocol.io/codes/claim-supporting-info-categories").setCode("ONS").setDisplay("Period, start or end dates of aspects of the Condition"))).setCode(new CodeableConcept(new Coding().setSystem("http://hcxprotocol.io/codes/claim-supporting-info-codes").setCode("ONS-2").setDisplay("Discharge start-discharge end time"))).getTimingDateType().setValue(preAuth.getClaimAdmissionDetails().getDischargeDate());
       claim.addSupportingInfo().setSequence(supportingInfoSeq++).setCode(new CodeableConcept(new Coding().setDisplay("PatientICUStay").setSystem("http://hcxprotocol.io/codes/claim-supporting-info-codes").setCode("ONS-6"))).setCategory(new CodeableConcept(new Coding().setSystem("http://hcxprotocol.io/codes/claim-supporting-info-categories").setCode("ONS").setDisplay("Period, start or end dates of aspects of the Condition"))).setValue(new BooleanType(preAuth.getClaimAdmissionDetails().isIcuStay()));


        // hospitalServiceType completed
        claim.addItem().
                setSequence(itemSeq++).
                setProductOrService(new CodeableConcept(new Coding().setCode("99555").setSystem("http://terminology.hl7.org/CodeSystem/ex-USCLS").setDisplay("Expense"))).getUnitPrice().setCurrency("INR").setValue(preAuth.getHospitalServiceType().getRoomTariffPerDay());

        claim.setType(new CodeableConcept(new Coding().setCode(ClaimType.INSTITUTIONAL.toCode()).setSystem("http://terminology.hl7.org/CodeSystem/claim-type").setDisplay("Institutional")));

        claim.setPriority(new CodeableConcept(new Coding().setSystem("http://terminology.hl7.org/CodeSystem/processpriority").setCode(ProcessPriority.NORMAL.toCode()).setDisplay("Normal")));


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
        attachmentDTO.setPolicyInceptionDate(preAuth.getClaim().getPolicyInceptionDate());
        attachmentDTO.setChronicIllnessDetailsJSON(preAuth.getClaimIllnessTreatmentDetails().getChronicIllnessDetailsJSON());
        attachmentDTO.setProcedureCode(preAuth.getProcedureMethod().getProcedureCode());
        attachmentDTO.setRoomType(preAuth.getClaimAdmissionDetails().getRoomType());



        String attachmentString = new Gson().toJson(attachmentDTO);
        log.info("attachmentString{}", attachmentString);
        String encodedAttachement = Base64.getUrlEncoder().encodeToString(attachmentString.getBytes());
        claim.addSupportingInfo().setCode(new CodeableConcept(new Coding().setCode("INF-1").setDisplay("additional info related to claim").setSystem("http://hcxprotocol.io/codes/claim-supporting-info-codes"))).setCategory(new CodeableConcept(new Coding().setSystem("http://hcxprotocol.io/codes/claim-supporting-info-categories").setCode("INF").setDisplay("additional info related to claim"))).setSequence(supportingInfoSeq++).setValue(new StringType(encodedAttachement));

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
        log.info("fhir json "+messageString);
        return messageString;
    }

}
