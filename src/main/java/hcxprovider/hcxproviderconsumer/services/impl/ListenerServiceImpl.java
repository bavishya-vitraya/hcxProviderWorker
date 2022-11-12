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
        String payload;
        CoverageEligibilityRequest coverageEligibilityRequest = new CoverageEligibilityRequest();
        ClaimRequest claimRequest = new ClaimRequest();
        PreAuthRequest preAuthRequest = new PreAuthRequest();
        String reqType = msg.getRequestType();

        Operations operation = Operations.PRE_AUTH_SUBMIT;
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
        //  Boolean response = hcxOutgoingRequest.generate(payload, operation, recipientCode, output);
//        log.info(String.valueOf(response));
//        log.info("{}", output);
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
        entererPractitioner.addIdentifier().setValue(preAuth.getClaim().getCreatorId().toString());

        Practitioner carePractitioner = new Practitioner();
        carePractitioner.setId("Practioner/2");
        carePractitioner.addName().addGiven(preAuth.getClaimIllnessTreatmentDetails().getDoctorsDetails());
        carePractitioner.addQualification().getCode().addCoding().setCode(preAuth.getClaimIllnessTreatmentDetails().getDoctorsDetails());

        Organization organization = new Organization();
        organization.setId("Organization/1");
        organization.addIdentifier().setValue(preAuth.getClaim().getHospitalId().toString());

        // organization.addContact().setPurpose(preAuth.getClaim().getCityName());26

        Organization organizationInsurer = new Organization();
        organizationInsurer.setId("Organization/2");
        organizationInsurer.addIdentifier().setValue(preAuth.getClaim().getInsuranceAgencyId().toString());

        Patient patient = new Patient();
        patient.addIdentifier().setValue(preAuth.getClaim().getHospitalPatientId());
        patient.setId("Patient/1");
        patient.setBirthDate(new Date(preAuth.getClaim().getDob()));
        // patient.getGenderElement().setValueAsString(preAuth.getClaim().getGender());
        patient.addName().addGiven(preAuth.getClaim().getPatientName());
        patient.addTelecom().setValue(preAuth.getClaim().getPatient_mobile_no());
        patient.addContact().addTelecom().setSystem(ContactPointSystem.PHONE).setValue(preAuth.getClaim().getAttendent_mobile_no());
        patient.addContact().addTelecom().setSystem(ContactPointSystem.EMAIL).setValue(preAuth.getClaim().getPatient_email_id());
        //47

        Coverage insuranceCoverage = new Coverage();
        insuranceCoverage.setId("Coverage/1");
        insuranceCoverage.setSubscriberId(preAuth.getClaim().getMedicalCardId());
        insuranceCoverage.setPolicyHolder(new Reference("Patient/1"));
        insuranceCoverage.addIdentifier().setValue(preAuth.getClaim().getPolicyNumber());
        // coverage.setType() 23
        //25
        insuranceCoverage.getPeriod().setEnd(new Date(preAuth.getClaim().getPolicyEndDate()));
        insuranceCoverage.getPeriod().setStart(new Date(preAuth.getClaim().getPolicyStartDate()));


        Coverage coverage = new Coverage();
        coverage.addClass_().setValue(preAuth.getClaim().getPolicyName());
        // coverage37

        Meta meta = new Meta();
        meta.setProfile(Collections.singletonList(new CanonicalType(preAuth.getClaim().getMetadata())));


        Condition condition = new Condition();
        condition.setId("Condition/1");
        condition.getCode().setText(preAuth.getClaimIllnessTreatmentDetails().getChronicIllnessDetails());
        condition.setRecordedDate(new Date(preAuth.getClaimIllnessTreatmentDetails().getDateOfDiagnosis()));

        Procedure procedure = new Procedure();
        procedure.setId("Procedure/1");
        procedure.addFocalDevice().getAction().addCoding().setCode(preAuth.getClaimIllnessTreatmentDetails().getLeftImplant().toString()).setSystem("LeftImplant");
        procedure.addFocalDevice().getAction().addCoding().setCode(preAuth.getClaimIllnessTreatmentDetails().getRightImplant().toString()).setSystem("RightImplant");


        Claim claim = new Claim();
        claim.setUse(Claim.Use.PREAUTHORIZATION);
        claim.setId("Claim/1");
        claim.addIdentifier().setSystem("ClaimId").setValue(preAuth.getClaim().getId().toString());
        claim.setEnterer(new Reference("Practitioner/id"));
        claim.setCreated(new Date(preAuth.getClaim().getCreatedDate()));
        claim.setStatus(Claim.ClaimStatus.CANCELLED);
        claim.setProvider(new Reference("Organization/1"));
        claim.setPatient(new Reference("Patient/1"));
        claim.setInsurer(new Reference("Organization/2"));
        claim.addInsurance().setCoverage(new Reference("Coverage/1"));
        claim.setMeta(meta);
        claim.addIdentifier().setSystem("claimIllnessTreatmentDetails").setValue(preAuth.getClaimIllnessTreatmentDetails().getClaimId().toString());
        claim.addDiagnosis().getDiagnosisReference().setReference("Condition/1");
        claim.addSupportingInfo().getCategory().addCoding().setCode(preAuth.getClaim().getPolicyInceptionDate());
        claim.addSupportingInfo().getTimingDateType().setValue(new Date(preAuth.getClaim().getPolicyInceptionDate()));
        claim.addSupportingInfo().getCategory().addCoding().setSystem("INF");
        claim.addSupportingInfo().getCode().addCoding().setSystem("INF-1");
        claim.addProcedure().getProcedureReference().setReference("Procedure/1");
        claim.addCareTeam().getProvider().setReference("Practioner/2");
        //67

        //37(3rd)
        Composition composition = new Composition();
        composition.setId(UUID.randomUUID().toString());
        composition.setStatus(Composition.CompositionStatus.FINAL);
        composition.getType().addCoding().setSystem("https://www.hcx.org/document-type");
        composition.getType().addCoding().setCode("HcxClaimRequest");
        composition.addAuthor().setReference("Organization/1");
        composition.setDate(new Date());
        composition.setTitle("Claim Request");
        composition.addSection().addEntry().setReference("Claim/1");

        FhirContext fhirctx = FhirContext.forR4();
        Bundle bundle = new Bundle();
        bundle.setId(UUID.randomUUID().toString());
        bundle.setType(Bundle.BundleType.DOCUMENT);
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
        //log.info("Document bundle: {}", ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(document));
        return "success";
    }

    @Override
    public String buildVhiClaimProfile(PreAuthResponse preAuthResponse) {
        return null;
    }
}
