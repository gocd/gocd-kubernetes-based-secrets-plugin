package cd.go.contrib.secrets.kubernetes.models;

import cd.go.plugin.base.GsonTransformer;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecretConfigTest {

    @Test
    public void shouldConsiderBlankCertAsNull() {
        final Map<String, Object> settings = new HashMap<>();
        settings.put("kubernetes_cluster_ca_cert", "   ");

        SecretConfig config = GsonTransformer.fromJson(GsonTransformer.toJson(settings), SecretConfig.class);

        assertThat(config.getClusterCACertData()).isNull();
    }
}