/*
 * Copyright 2022 Thoughtworks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cd.go.contrib.secrets.kubernetes;

import cd.go.contrib.secrets.kubernetes.annotations.JsonSource;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.request.DefaultGoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.Mock;

import java.util.Map;

import static cd.go.plugin.base.ResourceReader.readResource;
import static cd.go.plugin.base.ResourceReader.readResourceBytes;
import static java.util.Base64.getDecoder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

class KubernetesSecretsPluginTest {
    private KubernetesSecretsPlugin kubernetesSecretsPlugin;

    @Mock
    KubernetesClientFactory kubernetesClientFactory;

    @Mock
    KubernetesClient kubernetesClient;

    @BeforeEach
    void setUp() {
        openMocks(this);
        kubernetesSecretsPlugin = new KubernetesSecretsPlugin(kubernetesClientFactory);
        kubernetesSecretsPlugin.initializeGoApplicationAccessor(mock(GoApplicationAccessor.class));
        when(kubernetesClientFactory.client(any())).thenReturn(kubernetesClient);
    }

    @Test
    void shouldReturnPluginIdentifier() {
        assertThat(kubernetesSecretsPlugin.pluginIdentifier()).isNotNull();
        assertThat(kubernetesSecretsPlugin.pluginIdentifier().getExtension()).isEqualTo("secrets");
        assertThat(kubernetesSecretsPlugin.pluginIdentifier().getSupportedExtensionVersions())
                .contains("1.0");
    }

    @ParameterizedTest
    @JsonSource(jsonFiles = "/secret-config-metadata.json")
    void shouldReturnConfigMetadata(String expectedJson) throws JSONException {
        final DefaultGoPluginApiRequest request = request("go.cd.secrets.secrets-config.get-metadata");

        final GoPluginApiResponse response = kubernetesSecretsPlugin.handle(request);

        assertThat(response.responseCode()).isEqualTo(200);
        assertEquals(expectedJson, response.responseBody(), true);
    }

    @Test
    void shouldReturnIcon() {
        final DefaultGoPluginApiRequest request = request("go.cd.secrets.get-icon");

        final GoPluginApiResponse response = kubernetesSecretsPlugin.handle(request);

        Map<String, String> responseBody = toMap(response.responseBody());

        assertThat(response.responseCode()).isEqualTo(200);
        assertThat(responseBody.size()).isEqualTo(2);
        assertThat(responseBody.get("content_type")).isEqualTo("image/svg+xml");
        assertThat(getDecoder().decode(responseBody.get("data"))).isEqualTo(readResourceBytes("/kubernetes_logo.svg"));
    }

    @Test
    void shouldReturnSecretConfigView() {
        final DefaultGoPluginApiRequest request = request("go.cd.secrets.secrets-config.get-view");

        final GoPluginApiResponse response = kubernetesSecretsPlugin.handle(request);

        Map<String, String> responseBody = toMap(response.responseBody());

        assertThat(response.responseCode()).isEqualTo(200);
        assertThat(responseBody.size()).isEqualTo(1);
        assertThat(responseBody.get("template")).isEqualTo(readResource("/secrets.template.html"));
    }

    @Nested
    class ValidateSecretConfig {
        private String requestName;

        @Mock
        private MixedOperation<Secret, SecretList, Resource<Secret>> secrets;
        @Mock
        private Resource<Secret> resource;

        @BeforeEach
        void setUp() {
            openMocks(this);
            requestName = "go.cd.secrets.secrets-config.validate";

            when(kubernetesClient.secrets()).thenReturn(secrets);
            when(secrets.inNamespace(any())).thenReturn(secrets);
            when(secrets.withName(any())).thenReturn(resource);
            when(resource.get()).thenReturn(new Secret());
        }

        @ParameterizedTest
        @JsonSource(jsonFiles = {
                "/secret-config-with-unknown-fields.json",
                "/unknown-fields-error.json"
        })
        void shouldFailIfHasUnknownFields(String requestBody, String expected) throws JSONException {
            final DefaultGoPluginApiRequest request = request(requestName);
            request.setRequestBody(requestBody);

            final GoPluginApiResponse response = kubernetesSecretsPlugin.handle(request);

            assertThat(response.responseCode()).isEqualTo(200);
            assertEquals(expected, response.responseBody(), true);
        }

        @ParameterizedTest
        @JsonSource(jsonFiles = {
                "/secret-config-with-missing-required-fields.json",
                "/missing-fields-error.json"
        })
        void shouldFailIfRequiredFieldsAreMissingInRequestBody(String requestBody, String expected) throws JSONException {
            final DefaultGoPluginApiRequest request = request(requestName);
            request.setRequestBody(requestBody);

            final GoPluginApiResponse response = kubernetesSecretsPlugin.handle(request);

            assertThat(response.responseCode()).isEqualTo(200);
            assertEquals(expected, response.responseBody(), true);
        }

        @ParameterizedTest
        @JsonSource(jsonFiles = "/secret-config.json")
        void shouldPassIfRequestIsValid(String requestBody) throws JSONException {
            final DefaultGoPluginApiRequest request = request(requestName);
            request.setRequestBody(requestBody);

            final GoPluginApiResponse response = kubernetesSecretsPlugin.handle(request);

            assertThat(response.responseCode()).isEqualTo(200);
            assertEquals("[]", response.responseBody(), true);
        }

        @ParameterizedTest
        @JsonSource(jsonFiles = {
                "/secret-config.json",
                "/non-existing-secret-config.json"
        })
        void shouldFailWhenNoSecretExistsWithTheSpecifiedName(String requestBody, String expected) throws JSONException {
            when(resource.get()).thenReturn(null);
            final DefaultGoPluginApiRequest request = request(requestName);
            request.setRequestBody(requestBody);

            final GoPluginApiResponse response = kubernetesSecretsPlugin.handle(request);

            assertThat(response.responseCode()).isEqualTo(200);
            assertEquals(expected, response.responseBody(), true);
        }

        @ParameterizedTest
        @JsonSource(jsonFiles = {
                "/secret-config.json",
                "/invalid-secret-config.json"
        })
        void shouldFailWhenProvidedSecretConfigurationsAreInvalid(String requestBody, String expected) throws JSONException {
            when(kubernetesClient.secrets()).thenThrow(new RuntimeException("Boom!"));
            final DefaultGoPluginApiRequest request = request(requestName);
            request.setRequestBody(requestBody);

            final GoPluginApiResponse response = kubernetesSecretsPlugin.handle(request);

            assertThat(response.responseCode()).isEqualTo(200);
            assertEquals(expected, response.responseBody(), true);
        }
    }

    private Map<String, String> toMap(String response) {
        return new Gson().fromJson(response, new TypeToken<Map<String, String>>() {
        }.getType());
    }

    private DefaultGoPluginApiRequest request(String requestName) {
        return new DefaultGoPluginApiRequest("secrets", "1.0", requestName);
    }
}
