/*
 * Copyright 2020 ConsenSys AG.
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
package tech.pegasys.web3signer.slashingprotection;

import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.StatementException;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;

public class DbTransactionRetryer {
  private static final Logger LOG = LogManager.getLogger();
  private final Random random = new Random();
  private final Jdbi jdbi;
  private final int maxRetries;
  private final int retryMs;

  public DbTransactionRetryer(final Jdbi jdbi, final int maxRetries, final int retryMs) {
    this.jdbi = jdbi;
    this.maxRetries = maxRetries;
    this.retryMs = retryMs;
  }

  public <R, X extends Exception> R handleWithTransactionRetry(
      final TransactionIsolationLevel level, HandleCallback<R, X> callback) {
    for (int i = 0; i <= maxRetries; i++) {
      try {
        return jdbi.inTransaction(level, callback);
      } catch (Exception e) {
        if (e instanceof StatementException) {
          LOG.debug("Transaction failed. Retry #{}", i);
          retrySleep();
        } else {
          throw new IllegalStateException("Unable to retry transaction", e);
        }
      }
    }
    throw new IllegalStateException(
        "Transaction max retries " + maxRetries + " reached. Not retrying transaction");
  }

  private void retrySleep() {
    try {
      final int jitter = random.nextInt(50);
      Thread.sleep(retryMs + jitter);
    } catch (InterruptedException ie) {
      throw new IllegalStateException("Transaction retry thread sleep interrupted", ie);
    }
  }
}
