package com.microsoft.jenkins.kubernetes.credentials.provider;

import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.CredentialsConvertionException;
import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.SecretToCredentialConverter;
import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.SecretUtils;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.google.common.io.BaseEncoding;
import com.microsoft.jenkins.kubernetes.credentials.KubeconfigCredentials;
import io.fabric8.kubernetes.api.model.Secret;
import org.jenkinsci.plugins.variant.OptionalExtension;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;

/**
 * SecretToCredentialConvertor that converts {@link KubeconfigCredentials}.
 */

@OptionalExtension(requirePlugins = {"kubernetes-credentials-provider"})
public class KubeconfigCredentialsConverter extends SecretToCredentialConverter {
    @Override
    public boolean canConvert(String type) {
        return "kubeconfig".equals(type);
    }

    @Override
    public KubeconfigCredentials convert(Secret secret) throws CredentialsConvertionException {
        String textBase64 = SecretUtils.getNonNullSecretData(secret, "kubeconfig", "kubeconfig is empty");
        String secretText;
        try {
            secretText = new String(BaseEncoding.base64().decode(textBase64), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            throw new CredentialsConvertionException(
                    "kubeconfig credential has an invalid text (must be base64 encoded UTF-8)", ex);
        }
        return new KubeconfigCredentials(
                CredentialsScope.GLOBAL,
                SecretUtils.getCredentialId(secret),
                SecretUtils.getCredentialDescription(secret), new KubeconfigCredentials.KubeconfigSource() {
            @Nonnull
            @Override
            public String getContent() {
                return secretText;
            }
        });
    }
}
