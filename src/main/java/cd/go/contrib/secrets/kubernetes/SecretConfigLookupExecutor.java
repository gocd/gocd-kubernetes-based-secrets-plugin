package cd.go.contrib.secrets.kubernetes;

import cd.go.contrib.secrets.kubernetes.models.SecretConfig;
import cd.go.contrib.secrets.kubernetes.models.Secrets;
import cd.go.contrib.secrets.kubernetes.request.SecretConfigRequest;
import com.github.bdpiparva.plugin.base.executors.secrets.LookupExecutor;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static com.github.bdpiparva.plugin.base.GsonTransformer.fromJson;
import static com.github.bdpiparva.plugin.base.GsonTransformer.toJson;
import static java.util.Collections.singletonMap;

public class SecretConfigLookupExecutor extends LookupExecutor<SecretConfigRequest> {
    @Override
    protected GoPluginApiResponse execute(SecretConfigRequest request) {
        SecretConfig secretConfig = request.getConfiguration();
        KubernetesClient client = KubernetesClientFactory.instance().client(secretConfig);
        try {
            List<String> secretIds = request.getKeys();
            if (secretIds == null || secretIds.isEmpty()) {
                return DefaultGoPluginApiResponse.badRequest("No secret key provided!!!");
            }

            Secret kubernetesSecret = client.secrets()
                    .inNamespace(secretConfig.getNamespace())
                    .withName(secretConfig.getSecretName())
                    .get();

            final Secrets secrets = new Secrets();
            ArrayList<String> missingSecretIds = new ArrayList<>();
            for (String secretId : secretIds) {
                if (kubernetesSecret.getData().containsKey(secretId)) {
                    String value = new String(Base64.getDecoder().decode(kubernetesSecret.getData().get(secretId)));
                    secrets.add(secretId, value);
                } else {
                    missingSecretIds.add(secretId);
                }
            }

            if (!missingSecretIds.isEmpty()) {
                return DefaultGoPluginApiResponse.error(toJson(singletonMap("message", String.format("Failed to lookup secrets from Kubernetes Secrets. Data keys '%s' does not exists in the '%s' kubernetes secret.", String.join(", ", missingSecretIds), secretConfig.getSecretName()))));
            }

            return DefaultGoPluginApiResponse.success(toJson(secrets));
        } catch (Exception e) {
            LOGGER.error("Failed to lookup secret from Kubernetes Secret.", e);
            return DefaultGoPluginApiResponse.error(toJson(singletonMap("message", "Failed to lookup secrets from Kubernetes Secret. See logs for more information.")));
        } finally {
            client.close();
        }
    }

    @Override
    protected SecretConfigRequest parseRequest(String body) {
        return fromJson(body, SecretConfigRequest.class);
    }
}
