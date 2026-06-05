package com.bhagwat.scm.api;

import com.bhagwat.scm.config.VaultProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;


import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class VaultClientImpl implements VaultClient {

    private final VaultTemplate vaultTemplate;
    private final ObjectMapper objectMapper;
    private final VaultProperties vaultProperties;

    public VaultClientImpl(
            VaultTemplate vaultTemplate,
            ObjectMapper objectMapper,
            VaultProperties vaultProperties) {

        this.vaultTemplate = vaultTemplate;
        this.objectMapper = objectMapper;
        this.vaultProperties = vaultProperties;
    }

    /* ---------------- KV ---------------- */

    @Override
    public Map<String, Object> read(String path) {

        VaultResponse response = vaultTemplate.read(path);

        return response != null ? response.getData() : null;
    }

    @Override
    public <T> T read(String path, Class<T> type) {

        Map<String, Object> data = read(path);

        return objectMapper.convertValue(data, type);
    }

    @Override
    public void write(String path, Object data) {

        Map<String, Object> map =
                objectMapper.convertValue(data, Map.class);

        vaultTemplate.write(path, map);
    }

    @Override
    public void delete(String path) {

        vaultTemplate.delete(path);
    }

    /* ---------------- Transit ---------------- */

    @Override
    public String encrypt(String keyName, String plainText) {

        String path = vaultProperties.getTransitKeyPath()
                + "/encrypt/" + keyName;

        String encoded =
                Base64.getEncoder().encodeToString(plainText.getBytes());

        Map<String, Object> request = new HashMap<>();
        request.put("plaintext", encoded);

        VaultResponse response = vaultTemplate.write(path, request);

        return (String) response.getData().get("ciphertext");
    }

    @Override
    public String decrypt(String keyName, String cipherText) {

        String path = vaultProperties.getTransitKeyPath()
                + "/decrypt/" + keyName;

        Map<String, Object> request = new HashMap<>();
        request.put("ciphertext", cipherText);

        VaultResponse response = vaultTemplate.write(path, request);

        String plain =
                (String) response.getData().get("plaintext");

        return new String(Base64.getDecoder().decode(plain));
    }

    @Override
    public String sign(String keyName, String data) {

        String path = vaultProperties.getTransitKeyPath()
                + "/sign/" + keyName;

        String encoded =
                Base64.getEncoder().encodeToString(data.getBytes());

        Map<String, Object> request = new HashMap<>();
        request.put("input", encoded);

        VaultResponse response = vaultTemplate.write(path, request);

        return (String) response.getData().get("signature");
    }

    @Override
    public String readTransitKey(String keyName) {

        String path = vaultProperties.getTransitKeyPath()
                + "/keys/" + keyName;

        VaultResponse response = vaultTemplate.read(path);

        return response != null ? response.getData().toString() : null;
    }
}
