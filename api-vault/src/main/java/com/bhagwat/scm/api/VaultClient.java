package com.bhagwat.scm.api;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface VaultClient {

    /* KV Operations */

    Map<String, Object> read(String path);

    <T> T read(String path, Class<T> type);

    void write(String path, Object data);

    void delete(String path);


    /* Transit Operations */

    String encrypt(String keyName, String plainText);

    String decrypt(String keyName, String cipherText);

    String sign(String keyName, String data);

    String readTransitKey(String keyName);
}
