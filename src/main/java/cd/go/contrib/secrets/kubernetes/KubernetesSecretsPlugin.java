package cd.go.contrib.secrets.kubernetes;

import com.github.bdpiparva.plugin.base.dispatcher.BaseBuilder;
import com.github.bdpiparva.plugin.base.dispatcher.RequestDispatcher;
import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.exceptions.UnhandledRequestTypeException;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import cd.go.contrib.secrets.kubernetes.models.SecretConfig;
import cd.go.contrib.secrets.kubernetes.validators.CredentialValidator;

import static java.util.Collections.singletonList;

@Extension
public class KubernetesSecretsPlugin implements GoPlugin {
    public static final Logger LOG = Logger.getLoggerFor(KubernetesSecretsPlugin.class);
    private RequestDispatcher requestDispatcher;

    @Override
    public void initializeGoApplicationAccessor(GoApplicationAccessor goApplicationAccessor) {
        requestDispatcher = BaseBuilder
                .forSecrets()
                .v1()
                .icon("/kubernetes_logo.svg", "image/svg+xml")
                .configMetadata(SecretConfig.class)
                .configView("/secrets.template.html")
                .validateSecretConfig(new CredentialValidator())
                .lookup(new SecretConfigLookupExecutor())
                .build();
    }

    @Override
    public GoPluginApiResponse handle(GoPluginApiRequest request) throws UnhandledRequestTypeException {
        return requestDispatcher.dispatch(request);
    }

    @Override
    public GoPluginIdentifier pluginIdentifier() {
        return new GoPluginIdentifier("secrets", singletonList("1.0"));
    }
}
