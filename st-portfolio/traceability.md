## 5. Planned Test Cases (Traceability)

This section lists key requirements/behaviours and the planned tests to cover them.
(After implementation, each item is backed by automated tests in the repository.)

### 5.1 `/validateOrder`
- Valid order → VALID + NO_ERROR  
  **Test:** `testValidateOrder_Valid`
- Empty pizzas array → INVALID + EMPTY_ORDER  
  **Test:** `testValidateOrder_Invalid`
- Too many pizzas (>4) → INVALID + MAX_PIZZA_COUNT_EXCEEDED  
  **Test:** `testValidateOrder_Invalid_TooManyPizzas`
- Total incorrect → INVALID + TOTAL_INCORRECT  
  **Test:** `testValidateOrder_Invalid_TotalIncorrect`
- Invalid card number → INVALID + CARD_NUMBER_INVALID  
  **Test:** `testValidateOrder_Invalid_CardNumberInvalid`
- CVV too short → INVALID + CVV_INVALID  
  **Test:** `testValidateOrder_Invalid_CvvInvalid`
- CVV non-numeric → INVALID + CVV_INVALID  
  **Test:** `testValidateOrder_Invalid_CvvNonNumeric`
- Expiry invalid format or impossible month → INVALID + EXPIRY_DATE_INVALID  
  **Test:** `testValidateOrder_Invalid_ExpiryFormat`
- Expired card → INVALID + EXPIRY_DATE_INVALID  
  **Test:** `testValidateOrder_Invalid_ExpiryExpired`
- Pizza name not defined in menus → INVALID + PIZZA_NOT_DEFINED  
  **Test:** `testValidateOrder_Invalid_PizzaNotDefined`
- Wrong content type → 4xx  
  **Test:** `testValidateOrder_WrongContentType`
- Broken JSON → 400  
  **Test:** `testValidateOrder_ContentIntentionallyBroken`
- Missing required nested object (creditCardInformation) → 4xx  
  **Test:** `testValidateOrder_MissingCreditCardInfo`

### 5.2 `/calcDeliveryPath` + `/calcDeliveryPathGeoJSON`
- Valid order → 200 with correct output structure  
  **Tests:** `testCalcDeliveryPath_Valid`, `testCalcDeliveryPathGeoJSON_Valid`
- Invalid order rejected → 400  
  **Test:** `testCalcDeliveryPath_InvalidOrder_Returns400`

### 5.3 `/uuid`
- Returns student identifier string  
  **Test:** `testUuid_ReturnsStudentId`

### 5.4 `/distanceTo`
- Valid coordinates return expected distance  
  **Test:** `testDistanceTo_Valid`
- Invalid coordinate (lng out of range) → 400  
  **Test:** `testDistanceTo_InvalidCoordinate_Returns400`
- Broken JSON → 400  
  **Test:** `testDistanceTo_BrokenJson_Returns400`
- Empty body → 400  
  **Test:** `testDistanceTo_EmptyBody_Returns400`

### 5.5 `/isCloseTo`
- Just below threshold → true  
  **Test:** `testIsCloseTo_JustBelowThreshold_True`
- Exactly threshold → false  
  **Test:** `testIsCloseTo_ExactlyThreshold_False`

### 5.6 `/nextPosition`
- Angle 0 (valid) returns coordinate object  
  **Test:** `testNextPosition_Angle0_Valid`
- Invalid angle (-1) → 400  
  **Test:** `testNextPosition_InvalidAngle_Returns400`

### 5.7 `/isInRegion`
- Closed polygon and inside point → true  
  **Test:** `testIsInRegion_Inside_ReturnsTrue`
- Open polygon (not closed) → 400  
  **Test:** `testIsInRegion_OpenPolygon_Returns400`