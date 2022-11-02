package hcxprovider.hcxproviderconsumer.repository;

import hcxprovider.hcxproviderconsumer.model.ClaimRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ClaimRequestRepo extends MongoRepository<ClaimRequest,String> {
    ClaimRequest findClaimRequestById(String id);
}
