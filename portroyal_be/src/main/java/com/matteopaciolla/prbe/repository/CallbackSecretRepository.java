package com.matteopaciolla.prbe.repository;

import com.matteopaciolla.prbe.model.entity.CallbackEntityId;
import com.matteopaciolla.prbe.model.entity.CallbackSecretEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CallbackSecretRepository extends JpaRepository<CallbackSecretEntity, CallbackEntityId> {
}
