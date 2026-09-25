package com.matteopaciolla.prbe.repository;

import com.matteopaciolla.prbe.model.entity.ConfigPropertyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConfigPropertyRepository extends JpaRepository<ConfigPropertyEntity, Integer> {

    List<ConfigPropertyEntity> findByConfigId(Integer configId);
    List<ConfigPropertyEntity> findByConfigName(String configName);
}
