# Requirements — PizzaDronz REST Service

System: PizzaDronz (Spring Boot REST service)  
Scope: REST endpoints and core validation/path logic implemented in `uk.ac.ed.inf.controllers.RestServiceController`.

## 1. Functional Requirements (FR)

### FR1 — UUID endpoint
- **FR1.1** `GET /uuid` returns HTTP 200 and a non-empty string identifier.
- **FR1.2** Response body is plain text (string).

### FR2 — Distance calculation
- **FR2.1** `POST /distanceTo` with valid positions returns HTTP 200 and a JSON number (double).
- **FR2.2** If request body or positions are missing/invalid, return HTTP 400.

### FR3 — Closeness predicate
- **FR3.1** `POST /isCloseTo` with valid positions returns HTTP 200 and a JSON boolean.
- **FR3.2** Uses threshold: returns `true` iff distance `< 0.00015`.
- **FR3.3** Invalid input returns HTTP 400.

### FR4 — Next position
- **FR4.1** `POST /nextPosition` with valid input returns HTTP 200 and JSON object with numeric `lng` and `lat`.
- **FR4.2** Angle must satisfy `0 <= angle <= 180`; otherwise HTTP 400.
- **FR4.3** Missing body or missing start position returns HTTP 400.

### FR5 — Region containment
- **FR5.1** `POST /isInRegion` returns HTTP 200 and a JSON boolean for valid input.
- **FR5.2** Region polygon must be closed (first vertex == last vertex) and have at least 4 vertices; otherwise HTTP 400.
- **FR5.3** Invalid coordinates return HTTP 400.

### FR6 — Order validation
- **FR6.1** `POST /validateOrder` always returns HTTP 200 with JSON `{orderStatus, orderValidationCode}`.
- **FR6.2** If `pizzasInOrder` is null/empty -> `orderStatus=INVALID`, `orderValidationCode=EMPTY_ORDER`.
- **FR6.3** If number of pizzas > 4 -> `INVALID`, `MAX_PIZZA_COUNT_EXCEEDED`.
- **FR6.4** If any pizza name not found in menu -> `INVALID`, `PIZZA_NOT_DEFINED`.
- **FR6.5** If pizzas come from different restaurants -> `INVALID`, `PIZZA_FROM_MULTIPLE_RESTAURANTS`.
- **FR6.6** If restaurant closed on order date -> `INVALID`, `RESTAURANT_CLOSED`.
- **FR6.7** If any pizza price doesn’t match menu -> `INVALID`, `PRICE_FOR_PIZZA_INVALID`.
- **FR6.8** If total != sum(pizzas) + 100 (delivery) -> `INVALID`, `TOTAL_INCORRECT`.
- **FR6.9** If credit card number not valid -> `INVALID`, `CARD_NUMBER_INVALID`.
- **FR6.10** If expiry invalid/expired -> `INVALID`, `EXPIRY_DATE_INVALID`.
- **FR6.11** If CVV not exactly 3 digits -> `INVALID`, `CVV_INVALID`.
- **FR6.12** Otherwise -> `VALID`, `NO_ERROR`.

### FR7 — Delivery path
- **FR7.1** `POST /calcDeliveryPath` with invalid order -> HTTP 400.
- **FR7.2** For valid order + available restaurant, returns HTTP 200 and a JSON array of `{lng, lat}` points.
- **FR7.3** If path calculation fails or returns empty -> HTTP 400.

### FR8 — Delivery path GeoJSON
- **FR8.1** `POST /calcDeliveryPathGeoJSON` with invalid order -> HTTP 400.
- **FR8.2** For valid order, returns HTTP 200 and a GeoJSON Feature:
  - `type="Feature"`
  - `geometry.type="LineString"`
  - `geometry.coordinates` is an array of `[lng, lat]`.
  - `properties.name="Delivery Path"`
- **FR8.3** Duplicates at start/end may be removed if consecutive points are `closeTo`.

---

## 2. Quality / Non-functional Requirements (NFR)

- **NFR1 Robustness:** Malformed JSON or missing required fields should produce a 4xx response (e.g., 400).
- **NFR2 Input validation:** Latitude must be in [-90, 90] and longitude in [-180, 180] for coordinate-based endpoints.
- **NFR3 Oracle strength:** Tests should assert not only HTTP status but also **exact** validation codes and response structure (supported by PIT mutation testing).

---

## 3. Assumptions / External Dependencies

- **A1 External ILP REST API:** Restaurant/menu, no-fly zones, central area come from `https://ilp-rest-2024.azurewebsites.net/`.
- **A2 Dependency impact:** Network/API availability may make some error-handling branches difficult to trigger in integration tests without stubbing/mocking.

