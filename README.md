# Loyalty Points Quote Service

A reactive HTTP service built with Vert.x that calculates loyalty points for flight bookings.
Integrates with external FX and promotional services to provide accurate point calculations based on
customer tier, cabin class, and promotional offers.

## 🚀 Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.8+

```bash
# Check Java version
java -version

# Check Maven version
mvn -version
```

### Build & Test

```bash
# Build project
mvn clean install

# Run all tests (22 tests)
mvn test

# Generate and view coverage report (93% branch coverage)
mvn test jacoco:report
open target/site/jacoco/index.html  # Mac
xdg-open target/site/jacoco/index.html  # Linux
start target/site/jacoco/index.html  # Windows
```

### Run the Application

**Option 1: Run with Mock Services (Recommended for Demo)**

The application requires external FX and Promo services. For demonstration purposes, use the
included demo launcher with embedded mocks:

```bash
# Compile
mvn clean compile

# Run with embedded mock services
mvn exec:java -Dexec.mainClass="me.hajk1.DemoApplication"
```

You'll see:

```
✅ Mock FX Service on http://localhost:9090
✅ Mock Promo Service on http://localhost:9091
✅ Loyalty Points Service started on http://localhost:8080
```

**Option 2: Run Tests (Best for Assessment)**

The most reliable way to verify the implementation is to run the comprehensive test suite:

```bash
mvn test
```

This executes 22 tests covering:

- Happy path scenarios (all tiers)
- Input validation
- FX service retry logic
- Promo service timeout handling
- Business rules (caps, warnings)
- Edge cases

### Testing the API

Once the application is running (via `DemoApplication`), test the endpoints:

**Example 1: Calculate points with promo code**

```bash
curl -X POST http://localhost:8080/v1/points/quote \
  -H "Content-Type: application/json" \
  -d '{
    "fareAmount": 1234.50,
    "currency": "USD",
    "cabinClass": "ECONOMY",
    "customerTier": "SILVER",
    "promoCode": "SUMMER25"
  }'
```

**Response:**

```json
{
  "basePoints": 4531,
  "tierBonus": 679,
  "promoBonus": 1302,
  "totalPoints": 6512,
  "effectiveFxRate": 3.67,
  "warnings": []
}
```

**Example 2: PLATINUM tier without promo**

```bash
curl -X POST http://localhost:8080/v1/points/quote \
  -H "Content-Type: application/json" \
  -d '{
    "fareAmount": 2000.00,
    "currency": "EUR",
    "cabinClass": "BUSINESS",
    "customerTier": "PLATINUM"
  }'
```

**Example 3: Validation error**

```bash
curl -X POST http://localhost:8080/v1/points/quote \
  -H "Content-Type: application/json" \
  -d '{
    "fareAmount": -100,
    "currency": "USD",
    "cabinClass": "ECONOMY",
    "customerTier": "SILVER"
  }'
```

**Response:**

```json
{
  "error": "Fare amount must be positive"
}
```

**Available Mock Promo Codes:**

- `SUMMER25` - 25% bonus, expires in 30 days
- `WINTER50` - 50% bonus, expires in 5 days (triggers warning)
- `MEGA100` - 100% bonus, expires in 15 days

## 📊 API

### Calculate Points

**POST** `/v1/points/quote`

**Request:**

```json
{
  "fareAmount": 1234.50,
  "currency": "USD",
  "cabinClass": "ECONOMY",
  "customerTier": "SILVER",
  "promoCode": "SUMMER25"
}
```

**Response (200):**

```json
{
  "basePoints": 4531,
  "tierBonus": 679,
  "promoBonus": 1302,
  "totalPoints": 6512,
  "effectiveFxRate": 3.67,
  "warnings": []
}
```

**Response (400) - Validation Error:**

```json
{
  "error": "Fare amount must be positive"
}
```

### Request Fields

| Field        | Type    | Required | Description                               |
|--------------|---------|----------|-------------------------------------------|
| fareAmount   | decimal | Yes      | Ticket price (must be > 0)                |
| currency     | string  | Yes      | Currency code (USD, EUR, etc.)            |
| cabinClass   | enum    | Yes      | ECONOMY, PREMIUM_ECONOMY, BUSINESS, FIRST |
| customerTier | enum    | Yes      | NONE, SILVER, GOLD, PLATINUM              |
| promoCode    | string  | No       | Optional promotional code                 |

## 🧮 How Points Are Calculated

### 1. Base Points

Convert fare to AED using live FX rate, round to nearest integer:

```
basePoints = round(fareAmount * fxRate)
Example: round(1234.50 * 3.67) = 4531
```

### 2. Tier Bonus

Apply customer tier multiplier, truncate decimals:

```
NONE: 0% | SILVER: 15% | GOLD: 30% | PLATINUM: 50%

tierBonus = floor(basePoints * tierMultiplier)
Example: floor(4531 * 0.15) = 679
```

### 3. Promo Bonus

Apply to combined base + tier, truncate decimals:

```
promoBonus = floor((basePoints + tierBonus) * promoPercent / 100)
Example: floor(5210 * 0.25) = 1302
```

### 4. Total & Cap

```
total = min(basePoints + tierBonus + promoBonus, 50000)
Example: min(6512, 50000) = 6512
```

### Warnings

- `PROMO_EXPIRES_SOON`: Promo expires within 7 days

## 🏗️ Architecture

Using **hexagonal architecture** (ports & adapters) for testability:

```
HTTP Layer (Vert.x)
    ↓
Domain Layer (Business Logic)
    ↓
Infrastructure Layer (External Services)
```

### Project Structure

```
src/
├── main/java/me/hajk1/
│   ├── Application.java                          # Main Vert.x verticle
│   ├── DemoApplication.java                      # Demo launcher with mocks
│   ├── domain/
│   │   ├── model/                                # Domain models (POJOs)
│   │   │   ├── PointsQuoteRequest.java
│   │   │   ├── PointsQuoteResponse.java
│   │   │   ├── CustomerTier.java                # Enum with multipliers
│   │   │   ├── CabinClass.java
│   │   │   ├── PromoDetails.java
│   │   │   └── FxRateResponse.java
│   │   ├── ports/                                # Interfaces (hexagonal)
│   │   │   ├── PointsCalculationService.java
│   │   │   ├── FxRateService.java
│   │   │   └── PromoService.java
│   │   └── service/                              # Business logic
│   │       ├── PointsCalculationServiceImpl.java
│   │       └── ValidationException.java
│   └── infrastructure/
│       ├── http/
│       │   └── PointsQuoteHandler.java           # HTTP request handler
│       ├── client/
│       │   ├── HttpFxRateService.java            # FX client with retry
│       │   └── HttpPromoService.java             # Promo client with timeout
│       └── config/
│           └── JacksonConfig.java                # JSON configuration
└── test/java/me/hajk1/
    └── component/                                 # Component tests
        ├── ComponentTestBase.java                 # WireMock base class
        ├── PointsQuoteComponentTest.java
        ├── ValidationComponentTest.java
        ├── FxServiceResilienceTest.java
        ├── PromoServiceResilienceTest.java
        └── EdgeCaseComponentTest.java
```

**Why this design?**

- Business logic testable without HTTP
- Easy to swap implementations (mock ↔ real services)
- Clear separation of concerns

**Key files:**

- `PointsCalculationServiceImpl` - Core business logic
- `HttpFxRateService` - FX rate client with retry
- `HttpPromoService` - Promo client with timeout
- `PointsQuoteHandler` - HTTP request handling
- `Application` - Main verticle (production)
- `DemoApplication` - Demo launcher with embedded mocks

## 🛡️ Resilience

### FX Service (Critical)

- **Retry**: Up to 3 attempts on 5xx errors
- **Failure**: Returns 500 after exhausting retries
- **Why**: Can't calculate points without exchange rate

### Promo Service (Non-Critical)

- **Timeout**: 2 seconds
- **Failure**: Continues with `promoBonus: 0`
- **Why**: Promo is nice-to-have, not essential

## 🧪 Testing

### Test Coverage: 93% Branch Coverage

```bash
# Run all tests (22 tests)
mvn test

# Run specific test
mvn test -Dtest=FxServiceResilienceTest

# Generate coverage report
mvn test jacoco:report
```

### Test Organization

```
component/
├── PointsQuoteComponentTest      (Happy path & business rules)
├── ValidationComponentTest       (Input validation)
├── FxServiceResilienceTest       (Retry logic)
├── PromoServiceResilienceTest    (Timeout & fallback)
└── EdgeCaseComponentTest         (Edge cases)
```

**Testing approach:**

- TDD (test-first development)
- WireMock for stubbing external services
- JUnit 5 + AssertJ
- Component tests (full HTTP flow)

### Key Test Scenarios

✅ All customer tiers (NONE, SILVER, GOLD, PLATINUM)  
✅ Input validation (negative fare, null fields, blank strings)  
✅ FX retry (3 attempts, eventual success/failure)  
✅ Promo timeout & graceful degradation  
✅ 50,000 point cap  
✅ Promo expiry warnings  
✅ Edge cases (empty promo, zero bonus, exact cap)

## 🛠️ Development Notes

### Challenges Encountered

**Challenge 1: Floating-Point Precision Errors**

Initial implementation used `double` for calculations, causing precision errors:

```java
// ❌ WRONG
double basePoints = 1234.50 * 3.67;  // 4530.614999999998 (not 4530.615!)

// ✅ CORRECT
BigDecimal basePoints = BigDecimal.valueOf(1234.50)
    .multiply(BigDecimal.valueOf(3.67))
    .setScale(0, RoundingMode.HALF_UP);  // 4531 (exact)
```

Tests were failing because 4530.614999... rounded down to 4530 instead of up to 4531. Took me a
while to realize floating-point arithmetic was the culprit.

**Challenge 2: Retry Logic Double-Firing**

Originally used both `.compose()` and `.recover()` blocks, causing retries to fire twice per
failure (15 calls instead of 3). WireMock's `.verify()` caught this.

**Solution**: Handle all retry logic in `.compose()` only.

**Challenge 3: WireMock Port Binding**

Tried using port 0 (random) for tests, but had timing issues retrieving the actual port. Fixed
port (8888) is simpler and more reliable.

**Challenge 4: WireMock Scenario States**

Getting WireMock scenarios to work for retry testing took trial and error. The key insight:
`whenScenarioStateIs()` lets you simulate different responses across sequential requests.

### Design Decisions

**Hexagonal Architecture**  
Chose ports & adapters specifically for testability. Can test business logic without HTTP, test HTTP
without external services.

**Vert.x over Spring Boot**  
Wanted to learn reactive programming. Vert.x is lightweight and perfect for I/O-bound services.
Trade-off: steeper learning curve for async testing.

**Graceful Degradation for Promo**  
Promo is non-critical, so we continue without it on failure. Improves availability.

**No Graceful Degradation for FX**  
FX is critical - can't calculate without exchange rate. Retry 3x, then fail hard.

### Time Investment

- Project setup: ~20 min
- Domain models: ~30 min
- Service implementation: ~1 hour (including BigDecimal debugging)
- HTTP layer: ~45 min
- Component tests: ~1 hour (WireMock setup)
- Resilience tests: ~1.5 hours (retry scenarios were tricky!)
- Edge cases: ~30 min
- Documentation: ~1 hour
- **Total: ~7 hours** (including learning Vert.x)

**Biggest time sink**: WireMock retry scenarios  
**Most satisfying**: Achieving 93% coverage

### What I'd Do Differently

- Start with BigDecimal (would save debugging time)
- Read WireMock docs sooner
- Simpler test organization (nested classes were hard to navigate)

### Key Learnings

1. **BigDecimal is non-negotiable** for financial calculations
2. **WireMock scenarios are powerful** once you understand the state machine
3. **Vert.x async testing requires discipline** - always call `testContext.completeNow()`
4. **Coverage ≠ Quality** - 93% is great, but missing 7% might be important

## 🔧 Technology Stack

- **Runtime**: Java 17
- **Framework**: Vert.x 4.5.0 (reactive, non-blocking)
- **JSON**: Jackson 2.15.3
- **Testing**: JUnit 5, AssertJ, WireMock
- **Code Quality**: Lombok, SLF4J, JaCoCo
- **Build**: Maven

## ⚙️ Configuration

Configuration via JSON passed to Vert.x:

```json
{
  "http.port": 8080,
  "fx.service.url": "http://fx-service:8080",
  "promo.service.url": "http://promo-service:8080",
  "fx.retry.maxAttempts": 3,
  "promo.timeout.ms": 2000
}
```

## 🐛 Troubleshooting

**Tests fail with "Connection refused"**
→ Check WireMock is starting in `ComponentTestBase`

**Mathematical precision errors**
→ Use BigDecimal with explicit rounding modes, never `double`

**Coverage below 80%**
→ Ensure `lombok.config` has `lombok.addLombokGeneratedAnnotation = true`

**Tests hang**
→ Make sure you call `testContext.completeNow()` in all async tests

## 📄 License

MIT License - See [LICENSE](LICENSE) file

This is a technical assessment project demonstrating Java backend engineering and TDD skills.

## 📞 Contact

For questions about this implementation, feel free to reach out!