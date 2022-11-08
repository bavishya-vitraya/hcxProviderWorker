package hcxprovider.hcxproviderconsumer.repository;

import hcxprovider.hcxproviderconsumer.model.ClaimResponse;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ClaimResponseRepo extends MongoRepository<ClaimResponse,String> {
    ClaimResponse findClaimResponseById(String id);
}
