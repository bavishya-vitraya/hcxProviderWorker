package hcxprovider.hcxproviderconsumer.repository;

import hcxprovider.hcxproviderconsumer.model.CoverageEligibilityResponse;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CoverageEligibilityResponseRepo extends MongoRepository<CoverageEligibilityResponse,String> {
    CoverageEligibilityResponse findCoverageEligibilityResponseById(String id);
}
