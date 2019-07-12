package cd.go.contrib.secrets.kubernetes.models;

import com.google.gson.annotations.Expose;

public class Secret {
    @Expose
    private String key;
    @Expose
    private String value;

    Secret(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
