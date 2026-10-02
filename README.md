# Fraud Detection Engine

Local-first web dashboard and REST API for transaction risk screening.

## Requirements

The project targets Java 25 LTS. Use JDK 25 to compile and run; set `JAVA_HOME` to a JDK 25 installation before running Maven if it selects an older runtime.

## Security

The application binds to loopback by default and requires no credentials for local use. A focused scan of application source, `pom.xml`, this README, and `.gitignore` found no hard-coded credential assignments matching common password, secret, token, or API-key patterns. This is not a full security audit or a scan of historical commits: the local Git repository was initialized without pre-existing history.

The `.gitignore` excludes `.env` files, build output, logs, and local Java-upgrade reports. Do not commit credentials or other secrets; supply any future secret configuration through environment variables. `.env.example` may contain placeholder values only.

The project uses Java 25 LTS and Spring Boot 4.1.1. Current runtime and framework versions do not guarantee that the application or its dependencies are vulnerability-free. Dependabot configuration is included for Maven dependencies and GitHub Actions. After publishing this repository to GitHub, enable Dependabot alerts and security updates in repository settings. Review and test generated update pull requests before merging.

OWASP Dependency-Check 13.0.0 scanned 46 dependency entries (22 unique) on 2026-10-02 and reported no vulnerabilities using its available NVD and CISA KEV data. Sonatype OSS Index was unavailable because it requires credentials. Scanners can miss vulnerabilities; this result is not a security certification or a guarantee against known or unknown issues.

The application refuses to bind to a non-loopback address. For a hosted deployment, keep it bound to `127.0.0.1` behind a maintained reverse proxy that terminates HTTPS and enforces authentication and request-rate limits. You may additionally set `FRAUD_API_KEY` to a random secret of at least 32 characters; when configured, the API requires `Authorization: Bearer <key>`. Store keys only in environment variables. The dashboard's Accesso API control holds the key only in page memory.

The browser history is memory-only and cleared on refresh or tab close. The server does not persist transaction history. Do not submit real payment data or treat automated results as a substitute for human review or regulatory compliance.

## Run

```bash
mvn spring-boot:run
```

Open the dashboard at `http://localhost:8081`. The API is available at `http://localhost:8081/api/v1/transactions`. Port 8081 keeps this app separate from other local services that may already use port 8080.

## Evaluate a transaction

```bash
curl -X POST http://localhost:8081/api/v1/transactions \
  -H 'Content-Type: application/json' \
  -d '{
    "transactionId": "tx-1001",
    "userId": "user-42",
    "amount": 6200.00,
    "currency": "EUR",
    "timestamp": "2026-10-02T10:00:00Z",
    "merchantCategory": "ELECTRONICS",
    "location": {
      "latitude": 45.4642,
      "longitude": 9.1900,
      "city": "Milan"
    }
  }'
```

An approved decision has `status: APPROVED`, a zero score, and no alert. A blocked decision has `status: BLOCKED` and includes a `FraudAlert` with a risk score and triggered rule descriptions. Invalid requests return HTTP 400 with field details.

## Rules

- `AmountThresholdRule`: flags EUR amounts above the configured `fraud.rules.amount-threshold-eur` (default 5000).
- `VelocityOrLocationRule`: flags users exceeding the configured transaction count/window or moving between coordinate locations farther than the configured distance in the configured time window.

Rule thresholds and windows are configured in `src/main/resources/application.yml`. Add another Spring `@Component` implementing `FraudRule` to extend the pipeline.

## Tests

```bash
mvn test
```

Velocity history is process-local and in-memory. Hosted production deployments require a separate security, privacy, operational, and regulatory review.