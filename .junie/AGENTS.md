# ByPass Transers — Development Guide

## Build & Configuration

- **Java**: Project targets Java 17 (`<release>17</release>` in pom.xml). The system may have JDK 21 installed; compilation works via `--release 17` flag.
- **Build**: Maven wrapper — run `mvnw.cmd compile` (Windows) or `./mvnw compile` (Linux/Mac).
- **Lombok caveat**: Lombok is declared as a dependency but **does not work** with the current JDK 21 + forked compiler setup (`NoSuchFieldException: TypeTag :: UNKNOWN`). Do **not** use Lombok annotations (`@Getter`, `@Setter`, etc.) — write manual getters/setters instead. If Lombok support is needed, either downgrade to JDK 17 or upgrade Lombok to a JDK 21-compatible version (1.18.30+) and add `annotationProcessorPaths` to `maven-compiler-plugin`.
- **Database**: PostgreSQL is required for runtime. Connection is configured in `src/main/resources/application.properties` (default: `localhost:5432/bypass_records`).
- **Flyway**: Database migrations live in `src/main/resources/db/migration/` using versioned naming (`V1__description.sql`, `V2__...`).
- **Profiles**: `dev` profile (`application-dev.properties`) enables debug logging, disables template caching, and relaxes session cookie security for local HTTP access.

## Testing

### Setup

- Test config: `src/test/resources/application.properties` — currently points to the **same PostgreSQL** instance as production. There is no separate test profile using H2.
- H2 is available as a test-scoped dependency in `pom.xml` but is **not configured**. To enable H2-based tests, create `src/test/resources/application-test.properties` with:
  ```properties
  spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL
  spring.datasource.driver-class-name=org.h2.Driver
  spring.jpa.hibernate.ddl-auto=create-drop
  spring.flyway.enabled=false
  ```
  Then annotate test classes with `@ActiveProfiles("test")`.

### Running Tests

```bash
# Run all tests
mvnw.cmd test

# Run a specific test class
mvnw.cmd test -Dtest=WalletModelTest

# Run a specific test method
mvnw.cmd test -Dtest=WalletModelTest#debit_reducesBalance
```

### Writing Tests

- **Prefer pure unit tests** (no `@SpringBootTest`) when testing model/domain logic — they run in <1 second vs 10+ seconds for Spring context tests.
- Test location: `src/test/java/com/bypass/bypasstransers/`
- Existing integration tests (`WalletTransactionServiceTest`, `RegistrationIntegrationTest`, etc.) use `@SpringBootTest` with `@ActiveProfiles("dev")` and require a running PostgreSQL instance.

**Example — pure unit test for Wallet model:**

```java
package com.bypass.bypasstransers;

import com.bypass.bypasstransers.model.Wallet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class WalletModelTest {

    private Wallet wallet;

    @BeforeEach
    void setUp() {
        wallet = new Wallet();
        wallet.setBalance(new BigDecimal("500.00"));
    }

    @Test
    void debit_reducesBalance() {
        wallet.debit(new BigDecimal("200.00"));
        assertEquals(0, new BigDecimal("300.00").compareTo(wallet.getBalance()));
    }

    @Test
    void debit_insufficientBalance_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> wallet.debit(new BigDecimal("999.00")));
    }

    @Test
    void credit_increasesBalance() {
        wallet.credit(new BigDecimal("100.00"));
        assertEquals(0, new BigDecimal("600.00").compareTo(wallet.getBalance()));
    }
}
```

## Code Style

- **No Lombok** — use manual getters/setters (see Build section above).
- Models use JPA annotations directly on fields (no XML mappings).
- `BigDecimal` for all monetary values — never use `double`/`float`.
- Enums in `com.bypass.bypasstransers.enums` — `Currency`, `Role`, `Permission`, `TransactionType`, `TransactionStatus`, `SyncStatus`.
- Exception handling: custom exceptions (`InsufficientBalanceException`, `AccountNotFoundException`) caught by `GlobalExceptionHandler` which redirects with flash attributes.
- Controllers return Thymeleaf view names or redirects (server-side rendered, not REST API).
- Security: Spring Security with `CustomUserDetailsService`; `SecurityService.getCurrentUser()` provides the authenticated user.
- The compiler plugin uses `<fork>true</fork>` with `-J-Duser.language=en` to force English compiler messages.
