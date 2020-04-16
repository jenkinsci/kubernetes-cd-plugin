package com.microsoft.jenkins.kubernetes.credentials.provider;

import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.CredentialsConvertionException;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.microsoft.jenkins.kubernetes.credentials.KubeconfigCredentials;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.junit.Test;

import java.io.InputStream;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;

public class KubeconfigCredentialsConverterTest {
	String content = "Hello World!";

	@Test
	public void canConvert() throws Exception {
		KubeconfigCredentialsConverter convertor = new KubeconfigCredentialsConverter();
		assertThat("correct registration of valid type", convertor.canConvert("kubeconfig"), is(true));
		assertThat("incorrect type is rejected", convertor.canConvert("something"), is(false));
	}

	@Test
	public void canConvertAValidSecret() throws Exception {
		KubeconfigCredentialsConverter convertor = new KubeconfigCredentialsConverter();

		try (InputStream is = get("valid.yaml")) {
			Secret secret = Serialization.unmarshal(is, Secret.class);
			assertThat("The Secret was loaded correctly from disk", notNullValue());
			KubeconfigCredentials credential = convertor.convert(secret);
			assertThat(credential, notNullValue());
			assertThat("credential id is mapped correctly", credential.getId(), is("a-test-kubeconfig"));
			assertThat("credential description is mapped correctly", credential.getDescription(), is("kubeconfig credential from Kubernetes"));
			assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
			assertThat("credential content is mapped correctly", credential.getContent(), is(content));
		}
	}


	@Test
	public void canConvertAValidMappedSecret() throws Exception {
		KubeconfigCredentialsConverter convertor = new KubeconfigCredentialsConverter();

		try (InputStream is = get("validMapped.yaml")) {
			Secret secret = Serialization.unmarshal(is, Secret.class);
			assertThat("The Secret was loaded correctly from disk", notNullValue());
			KubeconfigCredentials credential = convertor.convert(secret);
			assertThat(credential, notNullValue());
			assertThat("credential id is mapped correctly", credential.getId(), is("another-test-kubeconfig"));
			assertThat("credential description is mapped correctly", credential.getDescription(), is("kubeconfig credential from Kubernetes"));
			assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
			assertThat("credential content is mapped correctly", credential.getContent(), is(content));
		}
	}


	@Test
	public void failsToConvertWhenDataKeyMissing() throws Exception {
		KubeconfigCredentialsConverter convertor = new KubeconfigCredentialsConverter();

		try (InputStream is = get("missingData.yaml")) {
			Secret secret = Serialization.unmarshal(is, Secret.class);
			convertor.convert(secret);
			fail("Exception should have been thrown");
		} catch (CredentialsConvertionException cex) {
			assertThat(cex.getMessage(), containsString("kubeconfig is empty"));
		}
	}


	// BASE64 Corrupt
	@Test
	public void failsToConvertWhenDataKeyCorrupt() throws Exception {
		KubeconfigCredentialsConverter convertor = new KubeconfigCredentialsConverter();

		try (InputStream is = get("corruptData.yaml")) {
			Secret secret = Serialization.unmarshal(is, Secret.class);
			convertor.convert(secret);
			fail("Exception should have been thrown");
		} catch (CredentialsConvertionException cex) {
			assertThat(cex.getMessage(), containsString("kubeconfig credential has an invalid text (must be base64 encoded UTF-8)"));
		}
	}


	@Test
	public void failsToConvertWhenDataEmpty() throws Exception {
		KubeconfigCredentialsConverter convertor = new KubeconfigCredentialsConverter();

		try (InputStream is = get("void.yaml")) {
			Secret secret = Serialization.unmarshal(is, Secret.class);
			convertor.convert(secret);
			fail("Exception should have been thrown");
		} catch (CredentialsConvertionException cex) {
			assertThat(cex.getMessage(), containsString("kubeconfig is empty"));
		}
	}


	private static final InputStream get(String resource) {

		InputStream is = KubeconfigCredentialsConverterTest.class.getResourceAsStream(resource);
		if (is == null) {
			fail("failed to load resource " + resource);
		}
		return is;
	}

}