package com.veeva.vault.test;

import com.veeva.vault.custom.test.classes.TestClass;
import com.veeva.vault.vapil.api.client.VaultClient;
import com.veeva.vault.vapil.api.request.TestRequest;

import java.util.Objects;

public class Runner {
    public static void main(String[] args) {
        //Create client from a settings file (resources/vault.json)
        VaultClient vaultClient = getVaultClientFromSettings();

        vaultClient.newRequest(TestRequest.class)
                .setHeaderReferenceId(TestClass.TEST_A)
                .test(TestClass.TEST_A, 1);

        vaultClient.newRequest(TestRequest.class)
                .setHeaderReferenceId(TestClass.TEST_B)
                .test(TestClass.TEST_B, 1);
    }

    static VaultClient getVaultClientFromSettings() {
        try {
            String vapilSettings = new String(Objects.requireNonNull(ClassLoader.getSystemResourceAsStream("vault.json")).readAllBytes());
            return VaultClient
                    .newClientBuilderFromSettings(vapilSettings)
                    .withVaultClientId("vault-java-sdk-test")
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}