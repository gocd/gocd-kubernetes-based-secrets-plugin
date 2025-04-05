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

import java.util.concurrent.atomic.AtomicInteger;

import static cd.go.contrib.secrets.kubernetes.KubernetesSecretsPlugin.LOG;
import static java.text.MessageFormat.format;

public class KubernetesClientFactory {
    private static final KubernetesClientFactory KUBERNETES_CLIENT_FACTORY = new KubernetesClientFactory();
    private CachedClient client;

    public static KubernetesClientFactory instance() {
        return KUBERNETES_CLIENT_FACTORY;
    }

    public synchronized CachedClient client(SecretConfig secretConfig) {
        if (this.client != null && secretConfig.hasSameTargetCluster(this.client.secretConfig)) {
            LOG.debug("Using previously created client.");
            this.client.leases.incrementAndGet();
            return this.client;
        }

        LOG.debug(format("Creating a new client because {0}.", (client == null) ? "client is null" : "secret configuration has changed"));
        clearOutExistingClient();
        this.client = createClientFor(secretConfig);
        LOG.debug("New client is created.");
        return this.client;
    }

    private CachedClient createClientFor(SecretConfig secretConfig) {
        final ConfigBuilder configBuilder = new ConfigBuilder()
                .withOauthToken(secretConfig.getSecurityToken())
                .withMasterUrl(secretConfig.getClusterUrl())
                .withCaCertData(secretConfig.getClusterCACertData());

        return new CachedClient(new KubernetesClientBuilder().withConfig(configBuilder.build()).build(), secretConfig);
    }

    private synchronized void clearOutExistingClient() {
        if (this.client != null) {
            this.client.closeIfUnused();
            this.client = null;
        }
    }

    public class CachedClient implements AutoCloseable {

        private final KubernetesClient client;
        private final SecretConfig secretConfig;
        private final AtomicInteger leases = new AtomicInteger(1);
        private volatile boolean closed;

        CachedClient(KubernetesClient client, SecretConfig secretConfig) {
            this.client = client;
            this.secretConfig = secretConfig;
        }

        public KubernetesClient get() {
            return client;
        }

        public int leases() {
            return leases.get();
        }

        public boolean isClosed() {
            return closed;
        }

        @Override
        public void close() {
            releaseLease();
        }

        private void releaseLease() {
            // Close the client only if it is not the same as the one we have cached
            if (leases.decrementAndGet() == 0 && this != KubernetesClientFactory.this.client && !closed) {
                closeUnderlyingClient();
            }
        }

        public void closeIfUnused() {
            if (leases() == 0 && !closed) {
                closeUnderlyingClient();
            }
        }

        private void closeUnderlyingClient() {
            LOG.debug("Terminating existing kubernetes client...");
            client.close();
            closed = true;
        }
    }
}
