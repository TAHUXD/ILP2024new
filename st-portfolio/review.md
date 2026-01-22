**Scope of review:** `uk.ac.ed.inf.controllers.RestServiceController` and `src/test/java/uk/ac/ed/inf/RestServiceControllerIntegrationTest`  

## 1) Review Criteria (Checklist)

### A. Correctness & Requirements Alignment
- Endpoints follow expected contract.
- Validation behaviour matches documented requirements.
- Domain rules appear enforced consistently (e.g., max pizza count, credit card validation).

### B. Robustness & Error Handling
- Null handling / missing fields behaviour is consistent.
- Bad input produces controlled failures.
- External API failures handled safely (restaurants/noFlyZones/centralArea).

### C. Maintainability & Readability
- Methods are cohesive.
- Naming makes intent obvious.
- Avoid duplicated logic / magic numbers without explanation.

### D. Test Quality & Oracle Strength
- Tests assert meaningful outputs, not just “200 OK”.
- Boundary cases included.
- Tests are stable and deterministic.

### E. Test Coverage & Fault Detection Evidence
- JaCoCo line/branch coverage indicates broad execution.
- PIT mutation score indicates fault detection, not just execution.
- Remaining “SURVIVED” / “NO_COVERAGE” mutants are understood and justified.

## 2) Findings (Issues / Risks Identified)

### Finding 1 — External REST dependency reduces determinism
**Location:** `RestServiceController#getRestaurants`, `getNoFlyZones`, `getCentralArea`  
**Issue:** These methods call an external ILP REST service via `RestTemplate`. When the remote service is slow/unavailable, tests involving endpoints that depend on these calls can fail or behave inconsistently.  
**Risk:** Flaky CI runs, hard-to-reproduce failures, and reduced reliability of integration tests and mutation execution.    
**Possible Improvements:**
- Inject `RestTemplate` and mock it in tests, or use Spring `@MockBean`.
- Add timeouts/retry/backoff or safer fallback behaviour.

### Finding 2 — Magic numbers are not explained in controller logic
**Location:** `isCloseTo` uses threshold `0.00015`; `calculateNextPosition` step is `0.00015`  
**Issue:** The step size appears to be a domain rule but is embedded as a constant without a named constant or comment.  
**Risk:** Future maintainers may change it incorrectly or inconsistently.  
**Possible Improvements:**
- Extract to named constants like `DRONE_STEP = 0.00015` and document why.

### Finding 3 — Input validation boundary inconsistency for `nextPosition`
**Location:** `/nextPosition` validation: `angle >= 0` and `angle <= 180`  
**Issue:** The code only accepts angles 0–180, but typical drone movement models may permit 0–360 which I dont quite if spec says so.
**Risk:** Misalignment with requirements and spec, tests might not cover expected range if spec differs.    
**Possible Improvements:**
- Confirm requirement and adjust validation if needed.
- Add documentation explaining the accepted range and why.


### Finding 4 — Some tests rely mainly on “structure checks” rather than strong semantic assertions
**Location:** `testCalcDeliveryPath_Valid` and `testCalcDeliveryPathGeoJSON_Valid`  
**Issue:** Tests assert JSON structure/types and presence of fields, but do not fully verify domain correctness (path avoids no-fly zones, stays in central area once entered).  
**Risk:** A faulty algorithm could still pass if it returns “well-shaped” JSON.    
**Possible Improvements:**
- Add stronger oracles: verify no-fly zone intersection, verify central area rule, verify start/end are correct.
- Consider property-based checks over the returned path.


### Finding 5 — Mutation score still limited by equivalent/unreachable mutants
**Location:** PIT report, surviving/no-coverage mutants in controller helper logic  
**Issue:** Some mutations are likely equivalent as in boundary changes might not be reachable due to step size granularity or not coverable without controlling external responses.  
**Risk:** Mutation score may plateau even with good tests.  
**Possible Improvements (not implemented):**
- Record examples of equivalent or unreachable mutants explicitly in `mutation-analysis.md`.
- Refactor to enable dependency injection for external calls so mutants become reachable.

### Finding 6 — Limited direct unit testing of internal helper methods
**Location:** private helper methods (geometry, validation helpers)  
**Issue:** Many behaviours are tested through endpoints (integration-style), but helper logic is not isolated with unit tests.  
**Risk:** Harder to pinpoint failures; slower feedback, some edge cases may remain uncovered.   
**Possible Improvements (not implemented):**
- Add unit tests for helper logic if refactored into separate components.
