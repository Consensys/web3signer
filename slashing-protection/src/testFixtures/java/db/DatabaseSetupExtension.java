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
package db;

import db.DatabaseUtil.TestDatabaseInfo;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class DatabaseSetupExtension
    implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

  private static final String DB_CONTEXT_KEY = "db";

  @Override
  public void beforeEach(final ExtensionContext context) {
    final TestDatabaseInfo testDatabaseInfo = DatabaseUtil.create();
    final Handle handle = testDatabaseInfo.getJdbi().open();
    storeDbContext(context, testDatabaseInfo, handle);
  }

  @Override
  public void afterEach(final ExtensionContext context) throws Exception {
    final TestDbContext dbContext = getDbContext(context);
    dbContext.getDb().close();
    dbContext.getHandle().close();
  }

  @Override
  public boolean supportsParameter(
      final ParameterContext parameterContext, final ExtensionContext extensionContext)
      throws ParameterResolutionException {
    final Class<?> type = parameterContext.getParameter().getType();
    return type.equals(Handle.class)
        || type.equals(Jdbi.class)
        || type.equals(EmbeddedPostgres.class);
  }

  @Override
  public Object resolveParameter(
      final ParameterContext parameterContext, final ExtensionContext extensionContext)
      throws ParameterResolutionException {
    final TestDbContext dbContext = getDbContext(extensionContext);
    final Class<?> type = parameterContext.getParameter().getType();
    if (type.equals(Handle.class)) {
      return dbContext.getHandle();
    } else if (type.equals(Jdbi.class)) {
      return dbContext.getJdbi();
    } else if (type.equals(EmbeddedPostgres.class)) {
      return dbContext.getDb();
    } else {
      throw new RuntimeException("Unknown parameter type " + type);
    }
  }

  private TestDbContext getDbContext(final ExtensionContext extensionContext) {
    return (TestDbContext) extensionContext.getStore(Namespace.GLOBAL).get(DB_CONTEXT_KEY);
  }

  private void storeDbContext(
      final ExtensionContext context,
      final TestDatabaseInfo testDatabaseInfo,
      final Handle handle) {
    final TestDbContext dbContext =
        new TestDbContext(testDatabaseInfo.getDb(), testDatabaseInfo.getJdbi(), handle);
    final Store store = context.getStore(Namespace.GLOBAL);
    store.put(DB_CONTEXT_KEY, dbContext);
  }

  private static class TestDbContext {

    private final EmbeddedPostgres db;
    private final Jdbi jdbi;
    private final Handle handle;

    public TestDbContext(final EmbeddedPostgres db, final Jdbi jdbi, final Handle handle) {
      this.db = db;
      this.jdbi = jdbi;
      this.handle = handle;
    }

    public EmbeddedPostgres getDb() {
      return db;
    }

    public Jdbi getJdbi() {
      return jdbi;
    }

    public Handle getHandle() {
      return handle;
    }
  }
}
