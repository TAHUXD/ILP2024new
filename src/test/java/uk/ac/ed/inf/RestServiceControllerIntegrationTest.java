package uk.ac.ed.inf;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.closeTo;

@SpringBootTest
@AutoConfigureMockMvc
public class RestServiceControllerIntegrationTest {

        @Autowired
        private MockMvc mockMvc;
        private static final String VALID_ORDER_JSON =
                "{ \"orderNo\": \"12345\", \"orderDate\": \"2024-11-18\", \"priceTotalInPence\":1100,"
                        + "\"pizzasInOrder\":[{\"name\":\"R1: Margarita\",\"priceInPence\":1000}],"
                        + "\"creditCardInformation\":{\"creditCardNumber\":\"4485959141852684\",\"creditCardExpiry\":\"12/30\",\"cvv\":\"123\"}}";

        private static final String INVALID_ORDER_JSON =
                "{ \"orderNo\": \"12346\", \"orderDate\": \"2024-11-18\", \"priceTotalInPence\":100,"
                        + "\"pizzasInOrder\":[],"
                        + "\"creditCardInformation\":{\"creditCardNumber\":\"4485959141852684\",\"creditCardExpiry\":\"12/30\",\"cvv\":\"123\"}}";


        @Test
        void testValidateOrder_Valid() throws Exception {
                mockMvc.perform(post("/validateOrder")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_ORDER_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.orderStatus").value("VALID"))
                        .andExpect(jsonPath("$.orderValidationCode").value("NO_ERROR"));
        }

        @Test
        void testValidateOrder_Invalid() throws Exception {
                mockMvc.perform(post("/validateOrder")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(INVALID_ORDER_JSON))
                        .andExpect(status().isOk()) // Still returns 200 with error code
                        .andExpect(jsonPath("$.orderStatus").value("INVALID"))
                        .andExpect(jsonPath("$.orderValidationCode").value("EMPTY_ORDER"));
        }

        @Test
        void testValidateOrder_Invalid_ExpiryFormat() throws Exception {
        String badExpiryJson =
                "{ \"orderNo\": \"20005\", \"orderDate\": \"2024-11-18\", \"priceTotalInPence\":1100,"
                        + "\"pizzasInOrder\":[{\"name\":\"R1: Margarita\",\"priceInPence\":1000}],"
                        + "\"creditCardInformation\":{\"creditCardNumber\":\"4485959141852684\",\"creditCardExpiry\":\"13/30\",\"cvv\":\"123\"}}";

        mockMvc.perform(post("/validateOrder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badExpiryJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("INVALID"))
                .andExpect(jsonPath("$.orderValidationCode").value("EXPIRY_DATE_INVALID"));
        }

        @Test
        void testValidateOrder_Invalid_ExpiryExpired() throws Exception {
        String expiredCardJson =
                "{ \"orderNo\": \"20006\", \"orderDate\": \"2024-11-18\", \"priceTotalInPence\":1100,"
                        + "\"pizzasInOrder\":[{\"name\":\"R1: Margarita\",\"priceInPence\":1000}],"
                        + "\"creditCardInformation\":{\"creditCardNumber\":\"4485959141852684\",\"creditCardExpiry\":\"01/20\",\"cvv\":\"123\"}}";

        mockMvc.perform(post("/validateOrder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expiredCardJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("INVALID"))
                .andExpect(jsonPath("$.orderValidationCode").value("EXPIRY_DATE_INVALID"));
        }

        @Test
        void testValidateOrder_Invalid_CvvNonNumeric() throws Exception {
        String badCvvJson =
                "{ \"orderNo\": \"20007\", \"orderDate\": \"2024-11-18\", \"priceTotalInPence\":1100,"
                        + "\"pizzasInOrder\":[{\"name\":\"R1: Margarita\",\"priceInPence\":1000}],"
                        + "\"creditCardInformation\":{\"creditCardNumber\":\"4485959141852684\",\"creditCardExpiry\":\"12/30\",\"cvv\":\"abc\"}}";

        mockMvc.perform(post("/validateOrder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badCvvJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("INVALID"))
                .andExpect(jsonPath("$.orderValidationCode").value("CVV_INVALID"));
        }

        @Test
        void testCalcDeliveryPath_Valid() throws Exception {
                mockMvc.perform(post("/calcDeliveryPath")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_ORDER_JSON))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$").isArray())
                        .andExpect(jsonPath("$.length()").isNumber())
                        .andExpect(jsonPath("$[0].lng").exists())
                        .andExpect(jsonPath("$[0].lat").exists())
                        .andExpect(jsonPath("$[0].lng").isNumber())
                        .andExpect(jsonPath("$[0].lat").isNumber())
                        .andExpect(jsonPath("$[1].lng").isNumber())
                        .andExpect(jsonPath("$[1].lat").isNumber());
        }

        @Test
        void testCalcDeliveryPathGeoJSON_Valid() throws Exception {
                mockMvc.perform(post("/calcDeliveryPathGeoJSON")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_ORDER_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.type").value("Feature"))
                        .andExpect(jsonPath("$.geometry.type").value("LineString"))
                        .andExpect(jsonPath("$.properties.name").value("Delivery Path"))
                        .andExpect(jsonPath("$.geometry.coordinates").isArray())
                        .andExpect(jsonPath("$.geometry.coordinates.length()").isNumber())
                        .andExpect(jsonPath("$.geometry.coordinates[0]").isArray())
                        .andExpect(jsonPath("$.geometry.coordinates[0].length()").value(2));
        }
        
        // Stage 1
        //Invalid Order causes error 400 for path endpoint,added new test to cover controller "reject invalid" branch
        @Test
        void testCalcDeliveryPath_InvalidOrder_Returns400() throws Exception {
                mockMvc.perform(post("/calcDeliveryPath")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(INVALID_ORDER_JSON))
                        .andExpect(status().isBadRequest());
        }       

        //Boundary Test
        @Test
        void testValidateOrder_Invalid_TooManyPizzas() throws Exception {
                String tooManyPizzasJson =
                        "{ \"orderNo\": \"99999\", \"orderDate\": \"2024-11-18\", \"priceTotalInPence\":5100,"
                                + "\"pizzasInOrder\":["
                                + "{\"name\":\"R1: Margarita\",\"priceInPence\":1000},"
                                + "{\"name\":\"R1: Margarita\",\"priceInPence\":1000},"
                                + "{\"name\":\"R1: Margarita\",\"priceInPence\":1000},"
                                + "{\"name\":\"R1: Margarita\",\"priceInPence\":1000},"
                                + "{\"name\":\"R1: Margarita\",\"priceInPence\":1000}"
                                + "],"
                                + "\"creditCardInformation\":{\"creditCardNumber\":\"4485959141852684\",\"creditCardExpiry\":\"12/30\",\"cvv\":\"123\"}}";

                mockMvc.perform(post("/validateOrder")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(tooManyPizzasJson))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.orderStatus").value("INVALID"))
                        .andExpect(jsonPath("$.orderValidationCode").value("MAX_PIZZA_COUNT_EXCEEDED"));
        }

        @Test
        void testValidateOrder_Invalid_TotalIncorrect() throws Exception {
        String wrongTotalJson =
                "{ \"orderNo\": \"20001\", \"orderDate\": \"2024-11-18\", \"priceTotalInPence\":9999,"
                        + "\"pizzasInOrder\":[{\"name\":\"R1: Margarita\",\"priceInPence\":1000}],"
                        + "\"creditCardInformation\":{\"creditCardNumber\":\"4485959141852684\",\"creditCardExpiry\":\"12/30\",\"cvv\":\"123\"}}";

        mockMvc.perform(post("/validateOrder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wrongTotalJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("INVALID"))
                .andExpect(jsonPath("$.orderValidationCode").value("TOTAL_INCORRECT"));
        }

        @Test
        void testValidateOrder_Invalid_CardNumberInvalid() throws Exception {
        String badCardNumberJson =
                "{ \"orderNo\": \"20002\", \"orderDate\": \"2024-11-18\", \"priceTotalInPence\":1100,"
                        + "\"pizzasInOrder\":[{\"name\":\"R1: Margarita\",\"priceInPence\":1000}],"
                        + "\"creditCardInformation\":{\"creditCardNumber\":\"123\",\"creditCardExpiry\":\"12/30\",\"cvv\":\"123\"}}";

        mockMvc.perform(post("/validateOrder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badCardNumberJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("INVALID"))
                .andExpect(jsonPath("$.orderValidationCode").value("CARD_NUMBER_INVALID"));
        }

        @Test
        void testValidateOrder_Invalid_CvvInvalid() throws Exception {
        String badCvvJson =
                "{ \"orderNo\": \"20003\", \"orderDate\": \"2024-11-18\", \"priceTotalInPence\":1100,"
                        + "\"pizzasInOrder\":[{\"name\":\"R1: Margarita\",\"priceInPence\":1000}],"
                        + "\"creditCardInformation\":{\"creditCardNumber\":\"4485959141852684\",\"creditCardExpiry\":\"12/30\",\"cvv\":\"12\"}}";

        mockMvc.perform(post("/validateOrder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badCvvJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("INVALID"))
                .andExpect(jsonPath("$.orderValidationCode").value("CVV_INVALID"));
        }

        @Test
        void testValidateOrder_Invalid_PizzaNotDefined() throws Exception {
        String badPizzaJson =
                "{ \"orderNo\": \"20004\", \"orderDate\": \"2024-11-18\", \"priceTotalInPence\":1100,"
                        + "\"pizzasInOrder\":[{\"name\":\"SIXSEVEN\",\"priceInPence\":1000}],"
                        + "\"creditCardInformation\":{\"creditCardNumber\":\"4485959141852684\",\"creditCardExpiry\":\"12/30\",\"cvv\":\"123\"}}";

        mockMvc.perform(post("/validateOrder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badPizzaJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("INVALID"))
                .andExpect(jsonPath("$.orderValidationCode").value("PIZZA_NOT_DEFINED"));
        }

        @Test
        void testValidateOrder_WrongContentType() throws Exception {
        String json = "{ \"orderNo\": \"30001\" }";

        mockMvc.perform(post("/validateOrder")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(json))
                .andExpect(status().is4xxClientError());
        }

        @Test
        void testValidateOrder_ContentIntentionallyBroken() throws Exception {
        String broken = "{ \"orderNo\": \"30002\", "; 

        mockMvc.perform(post("/validateOrder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(broken))
                .andExpect(status().isBadRequest());
        }

        @Test
        void testValidateOrder_MissingCreditCardInfo() throws Exception {
        String missingCci =
                "{ \"orderNo\": \"30003\", \"orderDate\": \"2024-11-18\", \"priceTotalInPence\":1100,"
                        + "\"pizzasInOrder\":[{\"name\":\"R1: Margarita\",\"priceInPence\":1000}] }";

        mockMvc.perform(post("/validateOrder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(missingCci))
                .andExpect(status().is4xxClientError());
        }

        // Stage 2 onwards (becasue only recorded PIT numbers for start, stage 1, and final): Added to try increase mutation score and reduce no_coverage and kill surviving mutants
        // /uuid 
        @Test
        void testUuid_ReturnsStudentId() throws Exception {
        mockMvc.perform(get("/uuid"))
                .andExpect(status().isOk())
                .andExpect(content().string("s2172881"));
        }

        // /distanceTo
        @Test
        void testDistanceTo_Valid() throws Exception {
        String body = """
                {
                "position1": {"lng": 0.0, "lat": 0.0},
                "position2": {"lng": 3.0, "lat": 4.0}
                }
                """;
        mockMvc.perform(post("/distanceTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string("5.0")); // sqrt(3^2+4^2)
        }

        @Test
        void testDistanceTo_InvalidCoordinate_Returns400() throws Exception {
        String body = """
                {
                "position1": {"lng": 200.0, "lat": 0.0},
                "position2": {"lng": 0.0, "lat": 0.0}
                }
                """;
        mockMvc.perform(post("/distanceTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
        }

        @Test
        void testDistanceTo_BrokenJson_Returns400() throws Exception {
        String broken = "{ \"position1\": { \"lng\": 0.0 ";
        mockMvc.perform(post("/distanceTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(broken))
                .andExpect(status().isBadRequest());
        }

        @Test
        void testDistanceTo_EmptyBody_Returns400() throws Exception {
        mockMvc.perform(post("/distanceTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
        }

        // /isCloseTo
        @Test
        void testIsCloseTo_JustBelowThreshold_True() throws Exception {
        String body = """
                {
                "position1": {"lng": 0.0, "lat": 0.0},
                "position2": {"lng": 0.00014999, "lat": 0.0}
                }
                """;
        mockMvc.perform(post("/isCloseTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
        }

        @Test
        void testIsCloseTo_ExactlyThreshold_False() throws Exception {
        String body = """
                {
                "position1": {"lng": 0.0, "lat": 0.0},
                "position2": {"lng": 0.00015, "lat": 0.0}
                }
                """;
        mockMvc.perform(post("/isCloseTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
        }

        // /nextPosition
        @Test
        void testNextPosition_Angle0_Valid() throws Exception {
        String body = """
                { "start": {"lng": 1.0, "lat": 2.0}, "angle": 0 }
                """;
        mockMvc.perform(post("/nextPosition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lng").isNumber())
                .andExpect(jsonPath("$.lat").isNumber());
        }

        @Test
        void testNextPosition_InvalidAngle_Returns400() throws Exception {
        String body = """
                { "start": {"lng": 1.0, "lat": 2.0}, "angle": -1 }
                """;
        mockMvc.perform(post("/nextPosition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
        }

        // /isInRegion
        @Test
        void testIsInRegion_Inside_ReturnsTrue() throws Exception {
        String body = """
                {
                "position": {"lng": 0.5, "lat": 0.5},
                "region": {
                "name": "square",
                "vertices": [
                {"lng": 0.0, "lat": 0.0},
                {"lng": 0.0, "lat": 1.0},
                {"lng": 1.0, "lat": 1.0},
                {"lng": 1.0, "lat": 0.0},
                {"lng": 0.0, "lat": 0.0}
                ]
                }
                }
                """;
        mockMvc.perform(post("/isInRegion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
        }

        @Test
        void testIsInRegion_OpenPolygon_Returns400() throws Exception {
        String body = """
                {
                "position": {"lng": 0.5, "lat": 0.5},
                "region": {
                "name": "open",
                "vertices": [
                {"lng": 0.0, "lat": 0.0},
                {"lng": 0.0, "lat": 1.0},
                {"lng": 1.0, "lat": 1.0},
                {"lng": 1.0, "lat": 0.0}
                ]
                }
                }
                """;
        mockMvc.perform(post("/isInRegion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
}


}

