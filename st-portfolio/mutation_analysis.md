# Mutation Analysis (PIT)

**Tool:** PIT (pitest)  
**Primary target class:** `uk.ac.ed.inf.controllers.RestServiceController`  
**Primary test suite:** `RestServiceControllerIntegrationTest`  
**Reports:**
- Before (archived): `https://gitlab.com/TAHUXD/st-coursework/-/blob/main/st-portfolio/figures/JaCoCoReportBefore` 
- After Stage 1 (archived): `https://gitlab.com/TAHUXD/st-coursework/-/blob/main/st-portfolio/figures/JaCoCoReportBefore2`
- After Stage 4 (latest): `https://gitlab.com/TAHUXD/st-coursework/-/blob/main/st-portfolio/figures/JaCoCoReportAfter` 

## 1) Why mutation score improved (what changed)

Mutation testing measures whether tests detect faults, not just execute lines.

### Baseline (Before)
- Mutation score was 39%

## Stage 1 — 39% → 44%: improving oracles via validation + negative tests

After the initial PIT baseline (39% mutation score), I first focused on strengthening the fault-detection ability of the test suite for `/validateOrder` and one controller branch in `/calcDeliveryPath`. The key change was moving from “endpoint returns 200” style checks to precise oracles that assert the *exact* `orderStatus` and *exact* `orderValidationCode` for distinct failure modes.

### Tests added in this stage (examples)
These tests improved mutation score by killing mutants that previously survived because tests did not assert specific outcomes.

- **Expiry validation (format + expired)**
  - `testValidateOrder_Invalid_ExpiryFormat`
  - `testValidateOrder_Invalid_ExpiryExpired`
  - These target `isValidExpiryDate(...)` logic and kill mutants that:
    - negate conditionals inside expiry checks
    - replace boolean returns (true/false)
    - weaken regex/date parsing behaviour

- **CVV validation (non-numeric and short CVV)**
  - `testValidateOrder_Invalid_CvvNonNumeric`
  - `testValidateOrder_Invalid_CvvInvalid`
  - These kill mutants that:
    - remove/negate CVV format checks
    - replace return values that would incorrectly accept invalid CVVs

- **Order constraints + calculation checks**
  - `testValidateOrder_Invalid_TooManyPizzas` → asserts `MAX_PIZZA_COUNT_EXCEEDED`
  - `testValidateOrder_Invalid_TotalIncorrect` → asserts `TOTAL_INCORRECT`
  - These kill mutants around:
    - boundary checks (e.g., `> 4` ↔ `>= 4`)
    - arithmetic changes in total calculation (addition/subtraction mutants)

- **Card number validity**
  - `testValidateOrder_Invalid_CardNumberInvalid` → asserts `CARD_NUMBER_INVALID`
  - Kills mutants that weaken Luhn/format checks.

- **Pizza definition constraint**
  - `testValidateOrder_Invalid_PizzaNotDefined` → asserts `PIZZA_NOT_DEFINED`
  - Helps kill mutants that incorrectly accept unknown pizzas.

- **Robustness tests for malformed input**
  - `testValidateOrder_WrongContentType`
  - `testValidateOrder_ContentIntentionallyBroken`
  - `testValidateOrder_MissingCreditCardInfo`
  - These primarily increase structural coverage and kill some input-handling mutants by ensuring invalid requests yield 4xx.

- **Controller-level invalid-order branch**
  - `testCalcDeliveryPath_InvalidOrder_Returns400`
  - Covers the controller branch:
    - `if (validationResult.getOrderStatus() != VALID) return 400`
  - Kills mutants that negate this conditional or incorrectly return success.

### Why this stage improved PIT
This stage improved mutation score mainly because it introduced stronger oracles:
- Instead of only asserting HTTP 200, tests assert exact semantics (`orderValidationCode`).
- Many mutants create subtle behavioural differences (wrong validation code, wrong boolean) that are invisible to weak assertions but are caught by these precise checks.

### Result
- Mutation score improved from roughly **39% → ~44%** after these tests were added.
- At this point, a large portion of remaining mutants were still either **NO_COVERAGE** or **SURVIVED**, so the next stage focused on executing previously untested endpoints and branches.

### Stage 2 — Add coverage for NO_COVERAGE areas
Goal: execute controller methods that were never reached.

Actions (examples):
- Added endpoint tests for:
  - `GET /uuid` → `testUuid_ReturnsStudentId`
  - `POST /distanceTo` (valid + invalid + broken JSON + empty body)
  - `POST /isCloseTo` (threshold behaviour)
  - `POST /nextPosition` (valid + invalid angle)
  - `POST /isInRegion` (inside + invalid open polygon)

Effect:
- Reduced **NO_COVERAGE** mutants substantially.
- Increased line/branch coverage, but some mutants still survived because assertions were not strong enough yet.

### Stage 3 — Strengthen oracles (make tests fault-detecting)
Goal: kill mutants that change logic but don’t change “status code” outcomes.

Actions (examples):
- For `/validateOrder`, asserted exact `orderStatus` and exact `orderValidationCode` for each failure case:
  - `EMPTY_ORDER`, `TOTAL_INCORRECT`, `CARD_NUMBER_INVALID`, `CVV_INVALID`,
    `EXPIRY_DATE_INVALID`, `MAX_PIZZA_COUNT_EXCEEDED`, `PIZZA_NOT_DEFINED`, etc.
- For `/calcDeliveryPath`, asserted **400** for invalid orders (covers “reject invalid order” branch).
- For `/distanceTo`, asserted numeric correctness (`5.0` for (0,0)→(3,4)).

Effect:
- Killed mutants that would otherwise “look fine” if we only checked HTTP 200.

### Stage 4 — Boundary tests
Goal: kill ConditionalsBoundaryMutator / NegateConditionals mutants around thresholds.

Actions:
- `/isCloseTo` boundary pair:
  - distance just below 0.00015 → `true`
  - distance exactly 0.00015 → `false`

Effect:
- Killed mutants such as:
  - `<` ↔ `<=`
  - negated conditionals

## 2) Examples of “Killed” mutants (what specifically got better)

### Example A — Boundary mutation killed (isCloseTo)
**Mutant type:** changed conditional boundary / negated conditional  
**What it changes:** `< 0.00015` to `<= 0.00015` or flips logic  
**Test that kills it:**
- `testIsCloseTo_ExactlyThreshold_False`
- `testIsCloseTo_JustBelowThreshold_True`
**Reason:** boundary pair forces observable difference exactly at the threshold.

### Example B — Return-value mutation killed (/uuid)
**Mutant type:** replaced return value with `""`  
**Test that kills it:**
- `testUuid_ReturnsStudentId`
**Reason:** exact string assertion detects incorrect return.

### Example C — Input validation mutation killed (/distanceTo)
**Mutant type:** negated conditional / removed coordinate checks  
**Test that kills it:**
- `testDistanceTo_InvalidCoordinate_Returns400`
**Reason:** invalid longitude should be rejected; negating the check would incorrectly accept.

## 3) Survived mutants I did NOT fix

Mutation testing can produce survivors that are:
1) **Equivalent** (mutation doesn’t change observable behaviour)
2) **Unreachable** via public endpoints with valid constraints
3) Tied to **external dependencies** (hard to deterministically trigger without mocks)

Below are representative examples and justification.

### 3.1 Equivalent mutants (observable behaviour unchanged)
**Example pattern:**
- Changes like `>=` to `>` or `==` to `<=` where the boundary is never hit by inputs we can realistically produce through the endpoint, or produces no observable difference given constraints.

**Why not fixed:**
- Adding tests may be impossible or meaningless if the program output cannot differ.
- Equivalent mutants are a known limitation of mutation testing.

### 3.2 External API dependency branches (hard to control deterministically)
**Where it happens:**
- `getRestaurants()`, `getNoFlyZones()`, `getCentralArea()` call the external ILP REST service using `RestTemplate`.

**Common surviving/NO_COVERAGE patterns:**
- “removed call to printStackTrace”
- Exception/failure branches when remote service is down / returns unexpected data.

**Why not fixed (current approach):**
- In MockMvc integration tests, the controller calls real external endpoints.
- Forcing those exception branches reliably would require:
  - mocking/stubbing the RestTemplate like injecting a mock RestTemplate or using WireMock
  - running tests with network fault injection,
  which increases complexity and reduces reproducibility.

**Mitigation:**
- Documented as a limitation in LO4.  
- Prioritised mutations in core pure logic (validation + geometry) where deterministic tests are feasible.

### 3.3 Survivors that require deeper oracle design (time/ROI trade-off)
**Where it happens:**
- Path planning logic (`calculatePath`, `isValidMove`, geometry intersection helpers)

**Why not fully fixed:**
- Many mutants would require *very specific path-level assertions, e.g.:
  - asserting the path never leaves central area after entry,
  - asserting the path never crosses any no-fly zone edges,
  - asserting minimality/optimality constraints.
- This is doable, but it is a larger test-design effort compared to the highest-impact wins already achieved (raising mutation from 39% to 64%).

**Planned future improvement (if time):**
- Add property-style assertions to path output:
  - every segment respects `isValidMove`,
  - no segment intersects no-fly zones,
  - once inside central area, never leaves.

## 4) Summary: what PIT taught me

- **Coverage is necessary but not sufficient.**  
  Adding tests that simply execute code improves JaCoCo, but PIT only improves when assertions detect incorrect behaviour.

- **Mutation improvements came from two main factors:**
  1) Covering previously untested endpoints/methods (reduce NO_COVERAGE).
  2) Writing stronger oracles, especially for invalid inputs and boundary conditions.

- **Remaining survivors are mostly acceptable limitations** due to:
  - equivalent mutants,
  - unreachable states without redesign,
  - external REST API failure branches that are difficult to reliably trigger without stubbing.
