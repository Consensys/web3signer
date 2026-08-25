/*
 * Copyright 2026 ConsenSys AG.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class KubernetesAuthOptionsTest {

  @Test
  void constructsSuccessfullyWithValidParameters() {
    final KubernetesAuthOptions options =
        new KubernetesAuthOptions("my-role", Path.of("/tmp/token"), "auth-path");
    assertThat(options.getKubernetesRole()).isEqualTo("my-role");
    assertThat(options.getServiceAccountTokenPath()).isEqualTo(Path.of("/tmp/token"));
    assertThat(options.getAuthPath()).isEqualTo("auth-path");
  }

  @Test
  void usesDefaultsWhenOptionalParametersAreNull() {
    final KubernetesAuthOptions options = new KubernetesAuthOptions("my-role", null, null);
    assertThat(options.getKubernetesRole()).isEqualTo("my-role");
    assertThat(options.getServiceAccountTokenPath())
        .isEqualTo(KubernetesAuthOptions.DEFAULT_SERVICE_ACCOUNT_TOKEN_PATH);
    assertThat(options.getAuthPath()).isEqualTo(KubernetesAuthOptions.DEFAULT_AUTH_PATH);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        ".",
        "..",
        "auth/../path",
        "auth/./path",
        "../auth",
        "./auth",
        "auth/..",
        "auth/."
      })
  void throwsExceptionWhenAuthPathContainsDotOrDotDot(final String invalidAuthPath) {
    assertThatThrownBy(() -> new KubernetesAuthOptions("my-role", null, invalidAuthPath))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("kubernetesAuthPath must not contain '.' or '..' or empty path segments");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"/kubernetes", "kubernetes/", "/kubernetes/", "//kubernetes", "kubernetes//"})
  void stripsLeadingAndTrailingSlashesFromAuthPath(final String authPathWithSlashes) {
    final KubernetesAuthOptions options =
        new KubernetesAuthOptions("my-role", null, authPathWithSlashes);
    assertThat(options.getAuthPath()).isEqualTo("kubernetes");
  }

  @ParameterizedTest
  @ValueSource(strings = {"kubernetes//login", "auth//path"})
  void throwsExceptionWhenAuthPathContainsEmptySegments(final String invalidAuthPath) {
    assertThatThrownBy(() -> new KubernetesAuthOptions("my-role", null, invalidAuthPath))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("kubernetesAuthPath must not contain '.' or '..' or empty path segments");
  }
}
