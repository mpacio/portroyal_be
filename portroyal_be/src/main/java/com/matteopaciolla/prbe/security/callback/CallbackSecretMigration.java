package com.matteopaciolla.prbe.security.callback;

import com.matteopaciolla.prbe.model.entity.CallbackEntity;
import com.matteopaciolla.prbe.model.entity.CallbackEntityId;
import com.matteopaciolla.prbe.model.entity.CallbackSecretEntity;
import com.matteopaciolla.prbe.repository.CallbackRepository;
import com.matteopaciolla.prbe.repository.CallbackSecretRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CallbackSecretMigration implements ApplicationRunner {

    private final CallbackRepository callbackRepository;
    private final CallbackSecretRepository callbackSecretRepository;
    private final CallbackSecretCrypto callbackSecretCrypto;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (CallbackSecretEntity storedSecret : callbackSecretRepository.findAll()) {
            if (callbackSecretCrypto.needsReEncryption(storedSecret.getEncryptedSecret())) {
                storedSecret.setEncryptedSecret(callbackSecretCrypto.encrypt(
                        callbackSecretCrypto.decrypt(storedSecret.getEncryptedSecret())));
                callbackSecretRepository.save(storedSecret);
            }
        }

        for (CallbackEntity callback : callbackRepository.findAll()) {
            if (callback.getSecret() == null || callback.getSecret().isBlank()) {
                continue;
            }
            CallbackEntityId id = new CallbackEntityId(callback.getMatchKeyCode(), callback.getUserId());
            if (callbackSecretRepository.findById(id).isEmpty()) {
                callbackSecretRepository.save(new CallbackSecretEntity(
                        callback.getMatchKeyCode(),
                        callback.getUserId(),
                        callbackSecretCrypto.encrypt(callback.getSecret())));
            }
            callback.setSecret(null);
            callbackRepository.save(callback);
        }
    }
}
