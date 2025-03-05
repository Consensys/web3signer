# Localstack for AWS services testing

The AWS related unit, integration and acceptance tests can be tested against localstack.
Use following command to start localstack services.

```bash
docker compose up
```

Use `CTRL-C` or run `docker compose down` from the same directory but from another shell instance.

Export following environment variables before running Web3Signer tests.

```bash 
export RW_AWS_ACCESS_KEY_ID=test
export RW_AWS_SECRET_ACCESS_KEY=test
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_ACCESS_KEY_ID_TEST2=test2
export AWS_SECRET_ACCESS_KEY_TEST2=test2
export AWS_REGION=us-east-2
export AWS_ENDPOINT_OVERRIDE=http://127.0.0.1:4566
```

To import above in IntelliJ IDEA Run configurations:
```
RW_AWS_ACCESS_KEY_ID=test
RW_AWS_SECRET_ACCESS_KEY=test
AWS_ACCESS_KEY_ID=test
AWS_SECRET_ACCESS_KEY=test
AWS_REGION=us-east-2
AWS_ACCESS_KEY_ID_TEST2=test2
AWS_SECRET_ACCESS_KEY_TEST2=test2
AWS_ENDPOINT_OVERRIDE=http://127.0.0.1:4566
```

Run gradle tests.
```bash

```