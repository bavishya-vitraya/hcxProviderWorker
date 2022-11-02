package hcxprovider.hcxproviderconsumer.repository;


import hcxprovider.hcxproviderconsumer.model.CoverageEligibilityRequest;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface CoverageEligibilityRequestRepo extends MongoRepository<CoverageEligibilityRequest,String> {
    CoverageEligibilityRequest findCoverageEligibilityRequestById(String id);
}
