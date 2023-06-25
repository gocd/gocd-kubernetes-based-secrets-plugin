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

package cd.go.contrib.secrets.kubernetes.models;

import cd.go.plugin.base.annotations.Property;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Objects;

public class SecretConfig {
    @Expose
    @SerializedName("kubernetes_secret_name")
    @Property(name = "kubernetes_secret_name", required = true)
    private String secretName;

    @Expose
    @SerializedName("kubernetes_cluster_url")
    @Property(name = "kubernetes_cluster_url", required = true)
    private String clusterUrl;

    @Expose
    @SerializedName("security_token")
    @Property(name = "security_token", required = true, secure = true)
    private String securityToken;

    @Expose
    @SerializedName("kubernetes_cluster_ca_cert")
    @Property(name = "kubernetes_cluster_ca_cert", secure = true)
    private String clusterCACertData;

    @Expose
    @SerializedName("namespace")
    @Property(name = "namespace")
    private String namespace;

    public String getSecretName() {
        return secretName;
    }

    public String getClusterUrl() {
        return clusterUrl;
    }

    public String getSecurityToken() {
        return securityToken;
    }

    public String getClusterCACertData() {
        return clusterCACertData;
    }

    public String getNamespace() {
        return namespace == null || namespace.isBlank() ? "default" : namespace;
    }

    public void setSecretName(String secretName) {
        this.secretName = secretName;
    }

    public void setClusterUrl(String clusterUrl) {
        this.clusterUrl = clusterUrl;
    }

    public void setSecurityToken(String securityToken) {
        this.securityToken = securityToken;
    }

    public void setClusterCACertData(String clusterCACertData) {
        this.clusterCACertData = clusterCACertData;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SecretConfig that = (SecretConfig) o;
        return Objects.equals(secretName, that.secretName) &&
                Objects.equals(clusterUrl, that.clusterUrl) &&
                Objects.equals(securityToken, that.securityToken) &&
                Objects.equals(clusterCACertData, that.clusterCACertData) &&
                Objects.equals(namespace, that.namespace);
    }

    public boolean hasSameTargetCluster(SecretConfig that) {
        if (this == that) return true;
        if (that == null) return false;
        return Objects.equals(clusterUrl, that.clusterUrl) &&
                Objects.equals(securityToken, that.securityToken) &&
                Objects.equals(clusterCACertData, that.clusterCACertData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(secretName, clusterUrl, securityToken, clusterCACertData, namespace);
    }
}
