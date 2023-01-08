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

import cd.go.contrib.secrets.kubernetes.models.SecretConfig;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;

import static cd.go.contrib.secrets.kubernetes.KubernetesSecretsPlugin.LOG;
import static java.text.MessageFormat.format;

public class KubernetesClientFactory {
    private static final KubernetesClientFactory KUBERNETES_CLIENT_FACTORY = new KubernetesClientFactory();
    private KubernetesClient client;
    private SecretConfig secretConfig;

    public static KubernetesClientFactory instance() {
        return KUBERNETES_CLIENT_FACTORY;
    }

    public synchronized KubernetesClient client(SecretConfig secretConfig) {
        if (secretConfig.hasSameTargetCluster(this.secretConfig) && this.client != null) {
            LOG.debug("Using previously created client.");
            return this.client;
        }

        LOG.debug(format("Creating a new client because {0}.", (client == null) ? "client is null" : "secret configuration has changed"));
        this.secretConfig = secretConfig;
        this.client = createClientFor(secretConfig);
        LOG.debug("New client is created.");
        return this.client;
    }

    private KubernetesClient createClientFor(SecretConfig secretConfig) {
        final ConfigBuilder configBuilder = new ConfigBuilder()
                .withOauthToken(secretConfig.getSecurityToken())
                .withMasterUrl(secretConfig.getClusterUrl())
                .withCaCertData(secretConfig.getClusterCACertData());

        return new KubernetesClientBuilder().withConfig(configBuilder.build()).build();
    }
}
