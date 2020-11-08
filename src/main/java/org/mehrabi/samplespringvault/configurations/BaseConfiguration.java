/*
 * Copyright (c) 2020.
 * by Mohammad Mehrabi
 */

package org.mehrabi.samplespringvault.configurations;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.vault.core.*;
import org.springframework.vault.support.VaultMount;
import org.springframework.vault.support.VaultResponse;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
public class BaseConfiguration {

    @Value("${secret.path}")
    private String secretPath;
    @Value("${secret.name}")
    private String secretName;

    private final VaultTemplate vaultTemplate;

    public BaseConfiguration(VaultTemplate vaultTemplate) {
        this.vaultTemplate = vaultTemplate;
    }


    @Primary
    @Bean
    public void getConfigs() {
        VaultTransitOperations transitOperations = vaultTemplate.opsForTransit();
        VaultSysOperations sysOperations = vaultTemplate.opsForSys();

        if (!sysOperations.getMounts().containsKey("transit/")) {

            sysOperations.mount("transit", VaultMount.create("transit"));

            transitOperations.createKey("foo-key");
        }

        // Encrypt a plain-text value
        String cipherPassText = transitOperations.encrypt("foo-key", "Secure pass");
        String cipherUserText = transitOperations.encrypt("foo-key", "Secure user");
        log.info(cipherPassText);
        log.info(cipherUserText);
        Map<String, String> data = new HashMap<>();
        data.put("password", cipherPassText);
        data.put("user", cipherUserText);

        VaultKeyValueOperations keyValue = vaultTemplate.opsForKeyValue(secretPath, VaultKeyValueOperationsSupport.KeyValueBackend.versioned());
        keyValue.put(secretName, data);




        VaultResponse response = vaultTemplate
                .opsForKeyValue(secretPath, VaultKeyValueOperationsSupport.KeyValueBackend.KV_2).get(secretName);

        if (response == null || response.getData() == null) {
            throw new VaultNullException("vault is null");
        }

        log.info(response.getData().get("user").toString());
        log.info(response.getData().get("password").toString());

        String user = transitOperations.decrypt("foo-key", response.getData().get("user").toString());
        String password = transitOperations.decrypt("foo-key", response.getData().get("password").toString());

        log.info(user);
        log.info(password);
    }

}
