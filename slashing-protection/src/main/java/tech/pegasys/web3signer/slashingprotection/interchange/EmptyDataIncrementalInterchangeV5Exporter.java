/*
 * Copyright 2022 ConsenSys AG.
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
package tech.pegasys.web3signer.slashingprotection.interchange;

import static tech.pegasys.web3signer.slashingprotection.interchange.InterchangeV5Exporter.FORMAT_VERSION;

import tech.pegasys.web3signer.slashingprotection.interchange.model.Metadata;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.json.JsonMapper;

/** Only export metadata in JSON format without gvr and slashing protection data. */
public class EmptyDataIncrementalInterchangeV5Exporter implements IncrementalExporter {
  private final JsonGenerator jsonGenerator;
  private static final JsonMapper JSON_MAPPER = new InterchangeJsonProvider().getJsonMapper();

  public EmptyDataIncrementalInterchangeV5Exporter(final OutputStream outputStream)
      throws UncheckedIOException {
    try {
      jsonGenerator = JSON_MAPPER.getFactory().createGenerator(outputStream);
      jsonGenerator.writeStartObject();

      jsonGenerator.writeFieldName("metadata");
      JSON_MAPPER.writeValue(jsonGenerator, new Metadata(FORMAT_VERSION, null));

      jsonGenerator.writeArrayFieldStart("data");
      jsonGenerator.writeEndArray();

      jsonGenerator.writeEndObject();
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void export(final String publicKey) {}

  @Override
  public void finalise() {}

  @Override
  public void close() throws IOException {
    jsonGenerator.close();
  }
}
