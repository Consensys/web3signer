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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicInteger;

import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import org.jdbi.v3.testing.JdbiRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
public class DbTransactionRetryerTest {
  @Rule public JdbiRule db = JdbiRule.h2();

  @Mock private HandleCallback<Boolean, Exception> handleCallback;

  @Test
  public void retryIfTransactionFailure() throws Exception {
    final AtomicInteger transactionTries = new AtomicInteger();
    when(handleCallback.withHandle(any()))
        .thenAnswer(
            (Answer<Boolean>)
                invocation -> {
                  transactionTries.incrementAndGet();
                  if (transactionTries.get() == 1) {
                    throw new UnableToCreateStatementException(null, null, null);
                  } else {
                    return true;
                  }
                });

    final DbTransactionRetryer dbTransactionRetryer =
        new DbTransactionRetryer(db.getJdbi(), 1, 0, 1);
    dbTransactionRetryer.handleWithTransactionRetry(
        TransactionIsolationLevel.READ_COMMITTED, handleCallback);
    assertThat(transactionTries.get()).isEqualTo(2);
  }

  @Test
  public void retriesUpToMaxRetries() throws Exception {
    final AtomicInteger transactionTries = new AtomicInteger();
    when(handleCallback.withHandle(any()))
        .thenAnswer(
            (Answer<Boolean>)
                invocation -> {
                  transactionTries.incrementAndGet();
                  throw new UnableToCreateStatementException(null, null, null);
                });

    final DbTransactionRetryer dbTransactionRetryer =
        new DbTransactionRetryer(db.getJdbi(), 3, 0, 1);
    assertThatThrownBy(
            () ->
                dbTransactionRetryer.handleWithTransactionRetry(
                    TransactionIsolationLevel.READ_COMMITTED, handleCallback))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Transaction max retries 3 reached. Not retrying transaction");
    assertThat(transactionTries.get()).isEqualTo(4); // initial + 3 retries
  }

  @Test
  public void doesNotRetryIfFailedNonStatementException() throws Exception {
    when(handleCallback.withHandle(any())).thenThrow(new IllegalStateException());

    final DbTransactionRetryer dbTransactionRetryer =
        new DbTransactionRetryer(db.getJdbi(), 1, 0, 1);
    assertThatThrownBy(
            () ->
                dbTransactionRetryer.handleWithTransactionRetry(
                    TransactionIsolationLevel.READ_COMMITTED, handleCallback))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Unable to retry transaction");
    verify(handleCallback, atMostOnce()).withHandle(any());
  }
}
