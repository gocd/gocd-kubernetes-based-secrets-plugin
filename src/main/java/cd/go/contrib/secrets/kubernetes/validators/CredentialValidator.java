package cd.go.contrib.secrets.kubernetes.validators;

import cd.go.contrib.secrets.kubernetes.KubernetesClientFactory;
import cd.go.contrib.secrets.kubernetes.models.SecretConfig;
import com.github.bdpiparva.plugin.base.validation.ValidationResult;
import com.github.bdpiparva.plugin.base.validation.Validator;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.Map;

import static com.github.bdpiparva.plugin.base.executors.Executor.GSON;
import static com.github.bdpiparva.plugin.base.executors.Executor.LOGGER;

public class CredentialValidator implements Validator {
    private KubernetesClientFactory kubernetesClientFactory;

    public CredentialValidator(KubernetesClientFactory kubernetesClientFactory) {
        this.kubernetesClientFactory = kubernetesClientFactory;
    }

    @Override
    public ValidationResult validate(Map<String, String> requestBody) {
        ValidationResult validationResult = new ValidationResult();

        SecretConfig secretConfig = GSON.fromJson(GSON.toJson(requestBody), SecretConfig.class);
        KubernetesClient client = kubernetesClientFactory.client(secretConfig);

        try {
            Secret secret = client.secrets().inNamespace(secretConfig.getNamespace()).withName(secretConfig.getSecretName()).get();
            if (secret == null) {
                validationResult.add("kubernetes_secret_name", "Specified Kubernetes secret does not exists.");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to verify connection.", e);
            String errorMessage = "Could not read specified secret. Either the connection with kubernetes cluster could not be established or the kubernetes secret does not exists.";
            validationResult.add("kubernetes_secret_name", errorMessage);
            validationResult.add("kubernetes_cluster_url", errorMessage);
        } finally {
            client.close();
        }

        return validationResult;
    }
}
