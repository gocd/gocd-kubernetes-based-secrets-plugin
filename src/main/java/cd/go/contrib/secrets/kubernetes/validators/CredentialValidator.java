package cd.go.contrib.secrets.kubernetes.validators;

import cd.go.contrib.secrets.kubernetes.KubernetesClientFactory;
import cd.go.contrib.secrets.kubernetes.models.SecretConfig;
import cd.go.plugin.base.GsonTransformer;
import cd.go.plugin.base.validation.ValidationResult;
import cd.go.plugin.base.validation.Validator;
import com.thoughtworks.go.plugin.api.logging.Logger;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.Map;

public class CredentialValidator implements Validator {
    private static final Logger LOGGER = Logger.getLoggerFor(CredentialValidator.class);
    private KubernetesClientFactory kubernetesClientFactory;

    public CredentialValidator(KubernetesClientFactory kubernetesClientFactory) {
        this.kubernetesClientFactory = kubernetesClientFactory;
    }

    @Override
    public ValidationResult validate(Map<String, String> requestBody) {
        ValidationResult validationResult = new ValidationResult();

        SecretConfig secretConfig = GsonTransformer.fromJson(GsonTransformer.toJson(requestBody), SecretConfig.class);
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
        }

        return validationResult;
    }
}
