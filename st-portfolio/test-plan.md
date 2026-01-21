# Test Plan — PizzaDronz REST Service (ILP CW)
 
**System under test:** PizzaDronz (Spring Boot REST service)  
**Scope:** Controller-level behaviour and core validation/geometry logic, using HTTP-level integration tests via MockMvc.  
**Primary test file:** `src/test/java/uk/ac/ed/inf/RestServiceControllerIntegrationTest.java`  
**Instrumentation:** JaCoCo (structural coverage), PIT (mutation/fault-based adequacy)

## 1. Objectives

This test plan aims to provide evidence that the REST service:

1. Implements the endpoint contracts (status codes, response shapes/types).
2. Correctly validates orders and returns the appropriate validation codes.
3. Handles invalid inputs robustly (bad JSON, missing fields, invalid coordinates/angles).
4. Implements core geometry helpers correctly (distance calculation, closeness threshold, region inclusion).
5. Improves adequacy through coverage and mutation feedback (JaCoCo + PIT).

Success means:
- All planned tests are automated and pass reliably.
- Coverage reports are generated and used to guide additional tests.
- Mutation testing identifies weak or missing oracles and is improved iteratively.

## 2. Test Scope and Levels

### 2.1 In Scope
- **API / Controller integration tests** using MockMvc (real Spring MVC wiring).
- **Functional & negative testing** of endpoint behaviour.
- **Boundary testing** around key thresholds and input ranges.
- **Robustness testing** for malformed inputs.
- **Structural coverage** (JaCoCo) and **fault-based adequacy** (PIT).

### 2.2 Out of Scope / Constraints
- Full end-to-end testing with real external ILP REST service behaviour beyond basic calls.
- Exhaustive performance/load testing.
- Complete security testing (e.g., abuse, DoS, injection patterns) beyond basic robustness.
- Deterministic testing of branches that require external service failures (documented under limitations/LO4).

## 3. Test Environment and Tooling

- **Frameworks:** JUnit 5, Spring Boot Test, MockMvc  
- **Build:** Maven  
- **Coverage:** JaCoCo report (`target/site/jacoco/index.html`)  
- **Mutation:** PIT report (`target/pit-reports/**/index.html`)  
- **CI:** GitLab pipeline runs tests and produces artifacts (JaCoCo + PIT)

## 4. Test Strategy (Planned Approach)

### 4.1 Endpoint Contract Tests (Happy Path)
Goal: For valid requests, endpoints return HTTP 200 and the expected data shape.

Planned:
- `/validateOrder`: valid order returns `{orderStatus: VALID, orderValidationCode: NO_ERROR}`
- `/calcDeliveryPath`: returns JSON array of `{lng, lat}` objects
- `/calcDeliveryPathGeoJSON`: returns GeoJSON Feature with LineString coordinates
- `/uuid`: returns exact student identifier string
- `/distanceTo`: returns correct numeric distance
- `/nextPosition`: returns `{lng, lat}` object
- `/isInRegion`: returns boolean `"true"`/`"false"`

### 4.2 Negative Tests (Invalid Requests)
Goal: Invalid inputs produce correct status code (200 with INVALID payload for validation endpoint, 400 for malformed/invalid geometry inputs where applicable).

Planned examples:
- `/validateOrder`: empty pizzas array → INVALID with `EMPTY_ORDER`
- invalid card number → `CARD_NUMBER_INVALID`
- invalid expiry format or expired expiry → `EXPIRY_DATE_INVALID`
- invalid/non-numeric CVV → `CVV_INVALID`
- pizza not defined → `PIZZA_NOT_DEFINED`
- total incorrect → `TOTAL_INCORRECT`
- too many pizzas (>4) → `MAX_PIZZA_COUNT_EXCEEDED`

Also:
- `/calcDeliveryPath` rejects invalid orders with HTTP 400.

### 4.3 Boundary Tests (Thresholds and Input Ranges)
Goal: Kill boundary mutants and ensure correct behaviour at edges.

Planned boundaries:
- `/isCloseTo` threshold: **distance < 0.00015 is true**, **distance == 0.00015 is false**
- `/nextPosition` angle validation: valid lower bound (0), invalid negative angle (-1)
- coordinate validation for `/distanceTo`: longitude out of range (e.g., 200) should be rejected
- region validation for `/isInRegion`: polygon must be closed (first vertex equals last)

### 4.4 Robustness / Malformed Input
Goal: Show safe failure modes (HTTP 4xx) and no server crashes.

Planned robustness checks:
- Wrong Content-Type (e.g., text/plain to JSON endpoint)
- Intentionally broken JSON payload
- Empty request body
- Missing required fields (e.g., missing credit card information)

### 4.5 Coverage-Driven Improvement (JaCoCo + PIT)
Goal: Use coverage metrics to guide additional tests.

Planned approach:
1. Run JaCoCo to detect unexecuted lines/branches.
2. Run PIT to identify:
   - `NO_COVERAGE` (no tests execute code)
   - `SURVIVED` (code executed but faults not detected → weak oracles)
3. Add tests targeting:
   - Missing endpoint coverage (controller methods not hit)
   - Boundary conditions and stronger assertions to kill survivors
4. Re-run PIT and JaCoCo and record improvement.

## 5. Planned Test Cases

This section lists key requirements/behaviours and the planned tests to cover them.
(After implementation, each item is backed by automated tests in the repository.)

### 5.1 `/validateOrder`
- Valid order → VALID + NO_ERROR  
- Empty pizzas array → INVALID + EMPTY_ORDER  
- Too many pizzas (>4) → INVALID + MAX_PIZZA_COUNT_EXCEEDED  
- Total incorrect → INVALID + TOTAL_INCORRECT  
- Invalid card number → INVALID + CARD_NUMBER_INVALID  
- CVV too short → INVALID + CVV_INVALID  
- CVV non-numeric → INVALID + CVV_INVALID  
- Expiry invalid format or impossible month → INVALID + EXPIRY_DATE_INVALID  
- Expired card → INVALID + EXPIRY_DATE_INVALID  
- Pizza name not defined in menus → INVALID + PIZZA_NOT_DEFINED  
- Wrong content type → 4xx  
- Broken JSON → 400  
- Missing required nested object (creditCardInformation) → 4xx  

### 5.2 `/calcDeliveryPath` + `/calcDeliveryPathGeoJSON`
- Valid order → 200 with correct output structure  
- Invalid order rejected → 400  

### 5.3 `/uuid`
- Returns student identifier string  

### 5.4 `/distanceTo`
- Valid coordinates return expected distance  
- Invalid coordinate (lng out of range) → 400  
- Broken JSON → 400  
- Empty body → 400  

### 5.5 `/isCloseTo`
- Just below threshold → true  
- Exactly threshold → false  

### 5.6 `/nextPosition`
- Angle 0 (valid) returns coordinate object  
- Invalid angle (-1) → 400  

### 5.7 `/isInRegion`
- Closed polygon and inside point → true  
- Open polygon (not closed) → 400  

## 6. Test Data Design

- **Order fixtures**: a “valid order” and multiple “invalid order variants” to trigger specific validation codes.
- **Geometry fixtures**:
  - Distance sanity check: (0,0) to (3,4) → 5.0
  - Threshold distances around 0.00015 for closeness
  - Region as a closed square polygon and an open polygon variant
  - Angle boundary examples: 0 (valid), -1 (invalid)
- **Robustness fixtures**: broken JSON strings, empty body payloads, wrong Content-Type.

## 7. Reporting and Evidence

Artifacts generated from Maven runs and CI:
- Unit/integration test results (Surefire)
- JaCoCo HTML report: `target/site/jacoco/index.html`
- PIT HTML report: `target/pit-reports/**/index.html`
- CI pipeline run evidence: GitLab pipeline logs + downloadable artifacts

## 8. Maintenance

The plan will evolve iteratively:
- When PIT highlights weaknesses, add targeted tests and stronger assertions.
- When JaCoCo shows missed branches, add complementary negative/boundary tests.
- Keep test names descriptive and maintain Arrange–Act–Assert structure.
