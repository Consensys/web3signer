/*
 * Copyright 2024 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.web3signer.keystorage.hashicorp.config;

import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;

/**
 * Configuration options for the Vault Kubernetes auth method.
 *
 * <p>When using Kubernetes auth, the pod's service-account JWT is read from {@code
 * serviceAccountTokenPath} and exchanged for a short-lived Vault client token via {@code POST
 * /v1/auth/<authPath>/login}.
 */
public class KubernetesAuthOptions {

  /** Default path for the Kubernetes service-account JWT token injected by the kubelet. */
  public static final Path DEFAULT_SERVICE_ACCOUNT_TOKEN_PATH =
      Path.of("/var/run/secrets/kubernetes.io/serviceaccount/token");

  /** Default Vault auth mount path for the Kubernetes auth method. */
  public static final String DEFAULT_AUTH_PATH = "kubernetes";

  private final String kubernetesRole;
  private final Path serviceAccountTokenPath;
  private final String authPath;

  /**
   * @param kubernetesRole the Vault role name bound to this Kubernetes service account (required)
   * @param serviceAccountTokenPath path to the Kubernetes service-account JWT file; if {@code
   *     null}, defaults to {@link #DEFAULT_SERVICE_ACCOUNT_TOKEN_PATH}
   * @param authPath Vault auth mount path; if {@code null}, defaults to {@link #DEFAULT_AUTH_PATH}
   */
  public KubernetesAuthOptions(
      final String kubernetesRole, final Path serviceAccountTokenPath, final String authPath) {
    this.kubernetesRole = kubernetesRole;
    this.serviceAccountTokenPath =
        serviceAccountTokenPath != null
            ? serviceAccountTokenPath
            : DEFAULT_SERVICE_ACCOUNT_TOKEN_PATH;

    String processedAuthPath =
        (authPath != null && !authPath.isBlank()) ? authPath.strip() : DEFAULT_AUTH_PATH;

    processedAuthPath = StringUtils.strip(processedAuthPath, "/");

    this.authPath = processedAuthPath;

    for (final String segment : this.authPath.split("/", -1)) {
      if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
        throw new IllegalArgumentException(
            "kubernetesAuthPath must not contain '.' or '..' or empty path segments");
      }
    }
  }

  /** The Vault role name bound to this pod's Kubernetes service account. */
  public String getKubernetesRole() {
    return kubernetesRole;
  }

  /**
   * Path to the Kubernetes service-account JWT file, defaults to {@link
   * #DEFAULT_SERVICE_ACCOUNT_TOKEN_PATH}.
   */
  public Path getServiceAccountTokenPath() {
    return serviceAccountTokenPath;
  }

  /**
   * Vault auth mount path for the Kubernetes auth method, defaults to {@link #DEFAULT_AUTH_PATH}.
   */
  public String getAuthPath() {
    return authPath;
  }
}
