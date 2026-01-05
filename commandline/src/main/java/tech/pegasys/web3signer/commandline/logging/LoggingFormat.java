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
package tech.pegasys.web3signer.commandline.logging;

public enum LoggingFormat {
  ECS("classpath:EcsLayout.json"),
  GCP("classpath:GcpLayout.json"),
  GELF("classpath:GelfLayout.json"),
  LOGSTASH("classpath:LogstashJsonEventLayoutV1.json"),
  PLAIN(null);

  private final String eventTemplateUri;

  LoggingFormat(final String eventTemplateUri) {
    this.eventTemplateUri = eventTemplateUri;
  }

  public String getEventTemplateUri() {
    return eventTemplateUri;
  }

  public boolean isJson() {
    return eventTemplateUri != null;
  }
}
