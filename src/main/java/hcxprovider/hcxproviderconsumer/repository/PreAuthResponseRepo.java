package hcxprovider.hcxproviderconsumer.repository;

import hcxprovider.hcxproviderconsumer.model.PreAuthResponse;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PreAuthResponseRepo extends MongoRepository<PreAuthResponse,String> {
    PreAuthResponse findPreAuthResponseById(String id);
}
