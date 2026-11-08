package com.achmadrizkin.splitbill.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests through the real HTTP layer, services, JPA and Flyway schema (H2).
 * Every test creates its own group, so tests are independent.
 */
@SpringBootTest(properties = "app.github-username=achmadrizkin")
@AutoConfigureMockMvc
class SplitBillApiIntegrationTest {

    private static final String GROUPS = "/api/v1/groups";

    @Autowired
    private MockMvc mvc;

    private String groupId;
    private String andi;
    private String budi;
    private String citra;

    @BeforeEach
    void createBaliTripGroup() throws Exception {
        String body = mvc.perform(post(GROUPS).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Bali Trip", "currency": "IDR", "participants": ["Andi", "Budi", "Citra"]}
                        """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("http://localhost/api/v1/groups/")))
                .andExpect(jsonPath("$.currency").value("IDR"))
                .andExpect(jsonPath("$.participants[*].name", containsInAnyOrder("Andi", "Budi", "Citra")))
                .andReturn().getResponse().getContentAsString();

        groupId = JsonPath.read(body, "$.id");
        andi = JsonPath.read(body, "$.participants[0].id");
        budi = JsonPath.read(body, "$.participants[1].id");
        citra = JsonPath.read(body, "$.participants[2].id");
    }

    private ResultActions addExpense(String json) throws Exception {
        return mvc.perform(post(GROUPS + "/{id}/expenses", groupId)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions addExpense(String json, String idempotencyKey) throws Exception {
        return mvc.perform(post(GROUPS + "/{id}/expenses", groupId)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions recordPayment(String from, String to, String amount) throws Exception {
        return mvc.perform(post(GROUPS + "/{id}/payments", groupId).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"from_participant_id": "%s", "to_participant_id": "%s", "amount": "%s"}
                        """.formatted(from, to, amount)));
    }

    private void addBaliTripExpenses() throws Exception {
        addExpense("""
                {"description": "Hotel", "category": "ACCOMMODATION", "amount": "1500000", "paid_by": "%s"}
                """.formatted(andi)).andExpect(status().isCreated());
        addExpense("""
                {"description": "Seafood dinner", "category": "FOOD", "amount": 300000, "paid_by": "%s",
                 "split_type": "EQUAL"}
                """.formatted(budi)).andExpect(status().isCreated());
        addExpense("""
                {"description": "Taxi", "category": "TRANSPORT", "amount": "100000", "paid_by": "%s",
                 "split_type": "EXACT",
                 "splits": [{"participant_id": "%s", "value": "50000"}, {"participant_id": "%s", "value": "50000"}]}
                """.formatted(citra, andi, budi)).andExpect(status().isCreated());
    }

    @Nested
    @DisplayName("settlement")
    class Settlement {

        @Test
        void baliTripSettlementMatchesHandCalculation() throws Exception {
            addBaliTripExpenses();

            mvc.perform(get(GROUPS + "/{id}/settlement", groupId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currency").value("IDR"))
                    .andExpect(jsonPath("$.total_expenses").value("1900000.00"))
                    .andExpect(jsonPath("$.service_charge_pct").value(9))
                    .andExpect(jsonPath("$.service_charge_amount").value("171000.00"))
                    .andExpect(jsonPath("$.balances[0].net").value("850000.00"))
                    .andExpect(jsonPath("$.balances[1].net").value("-350000.00"))
                    .andExpect(jsonPath("$.balances[2].net").value("-500000.00"))
                    .andExpect(jsonPath("$.transaction_count").value(2))
                    .andExpect(jsonPath("$.settled").value(false))
                    .andExpect(jsonPath("$.transfers[0].from_name").value("Citra"))
                    .andExpect(jsonPath("$.transfers[0].to_name").value("Andi"))
                    .andExpect(jsonPath("$.transfers[0].amount").value("500000.00"))
                    .andExpect(jsonPath("$.transfers[1].from_name").value("Budi"))
                    .andExpect(jsonPath("$.transfers[1].amount").value("350000.00"))
                    .andExpect(jsonPath("$.category_summary[0].category").value("ACCOMMODATION"))
                    .andExpect(jsonPath("$.category_summary[0].total").value("1500000.00"))
                    .andExpect(jsonPath("$.service_charge_breakdown[*].amount",
                            containsInAnyOrder("58500.00", "58500.00", "54000.00")));
        }

        @Test
        void emptyGroupIsAlreadySettled() throws Exception {
            mvc.perform(get(GROUPS + "/{id}/settlement", groupId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total_expenses").value("0.00"))
                    .andExpect(jsonPath("$.service_charge_amount").value("0.00"))
                    .andExpect(jsonPath("$.transfers", hasSize(0)))
                    .andExpect(jsonPath("$.settled").value(true));
        }

        @Test
        void balancesEndpointExposesPerParticipantPositions() throws Exception {
            addBaliTripExpenses();

            mvc.perform(get(GROUPS + "/{id}/balances", groupId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[0].paid").value("1500000.00"))
                    .andExpect(jsonPath("$[0].share").value("650000.00"));
        }
    }

    @Nested
    @DisplayName("expenses")
    class Expenses {

        @Test
        void equalSplitNeverLosesTheRemainderCent() throws Exception {
            addExpense("""
                    {"description": "Snacks", "amount": "100000", "paid_by": "%s"}
                    """.formatted(andi))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.category").value("OTHER"))
                    .andExpect(jsonPath("$.split_type").value("EQUAL"))
                    .andExpect(jsonPath("$.shares[*].amount", containsInAnyOrder("33333.34", "33333.33", "33333.33")));
        }

        @Test
        void percentageSplitIsSupported() throws Exception {
            addExpense("""
                    {"description": "Villa", "amount": "1000000", "paid_by": "%s", "split_type": "PERCENTAGE",
                     "splits": [{"participant_id": "%s", "value": 50}, {"participant_id": "%s", "value": 50}]}
                    """.formatted(andi, budi, citra))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.shares[0].amount").value("500000.00"));
        }

        @Test
        void exactSplitMustAddUpToTotal() throws Exception {
            addExpense("""
                    {"description": "Taxi", "amount": "100000", "paid_by": "%s", "split_type": "EXACT",
                     "splits": [{"participant_id": "%s", "value": "60000"}]}
                    """.formatted(andi, budi))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error_code").value("SPLIT_SUM_MISMATCH"));
        }

        @Test
        void amountWithMoreDecimalsThanCurrencyAllowsIsRejected() throws Exception {
            addExpense("""
                    {"description": "Coffee", "amount": "10.005", "paid_by": "%s"}
                    """.formatted(andi))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error_code").value("AMOUNT_SCALE_EXCEEDED"));
        }

        @Test
        void payerMustBelongToTheGroup() throws Exception {
            addExpense("""
                    {"description": "Coffee", "amount": "10", "paid_by": "%s"}
                    """.formatted(UUID.randomUUID()))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error_code").value("PARTICIPANT_NOT_IN_GROUP"));
        }

        @Test
        void invalidRequestReturnsFieldErrorsAsProblemDetail() throws Exception {
            addExpense("""
                    {"amount": "-5"}
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(header().string("Content-Type", startsWith("application/problem+json")))
                    .andExpect(jsonPath("$.error_code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.errors[*].field", hasItems("description", "amount", "paid_by")));
        }

        @Test
        void unknownJsonFieldsAreRejected() throws Exception {
            addExpense("""
                    {"description": "Coffee", "ammount": "10", "paid_by": "%s"}
                    """.formatted(andi))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_code").value("MALFORMED_REQUEST"));
        }

        @Test
        void voidedExpenseNoLongerCountsAndCannotBeVoidedTwice() throws Exception {
            String body = addExpense("""
                    {"description": "Duplicate hotel", "amount": "1500000", "paid_by": "%s"}
                    """.formatted(andi)).andReturn().getResponse().getContentAsString();
            String expenseId = JsonPath.read(body, "$.id");

            mvc.perform(post(GROUPS + "/{g}/expenses/{e}/void", groupId, expenseId)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"reason\": \"Entered twice\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("VOIDED"))
                    .andExpect(jsonPath("$.void_reason").value("Entered twice"));

            mvc.perform(get(GROUPS + "/{id}/settlement", groupId))
                    .andExpect(jsonPath("$.total_expenses").value("0.00"))
                    .andExpect(jsonPath("$.settled").value(true));

            mvc.perform(post(GROUPS + "/{g}/expenses/{e}/void", groupId, expenseId)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"reason\": \"again\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error_code").value("EXPENSE_ALREADY_VOIDED"));
        }

        @Test
        void expensesCanBeFilteredByCategory() throws Exception {
            addBaliTripExpenses();

            mvc.perform(get(GROUPS + "/{id}/expenses", groupId).param("category", "FOOD"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].description").value("Seafood dinner"));
        }
    }

    @Nested
    @DisplayName("idempotency")
    class Idempotency {

        private final String hotel = """
                {"description": "Hotel", "amount": "1500000", "paid_by": "%s"}
                """;

        @Test
        void retryWithSameKeyReturnsOriginalExpenseInsteadOfCreatingADuplicate() throws Exception {
            String key = UUID.randomUUID().toString();
            String first = addExpense(hotel.formatted(andi), key)
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Idempotent-Replayed", "false"))
                    .andReturn().getResponse().getContentAsString();

            addExpense(hotel.formatted(andi), key)
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Idempotent-Replayed", "true"))
                    .andExpect(jsonPath("$.id").value((String) JsonPath.read(first, "$.id")));

            mvc.perform(get(GROUPS + "/{id}/expenses", groupId)).andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        void sameKeyWithDifferentPayloadIsRejected() throws Exception {
            String key = UUID.randomUUID().toString();
            addExpense(hotel.formatted(andi), key).andExpect(status().isCreated());

            addExpense(hotel.formatted(budi), key)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error_code").value("IDEMPOTENCY_KEY_REUSED"));
        }
    }

    @Nested
    @DisplayName("payments")
    class Payments {

        @Test
        void recordedPaymentReducesOutstandingTransfers() throws Exception {
            addBaliTripExpenses();

            recordPayment(budi, andi, "350000")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.amount").value("350000.00"));

            mvc.perform(get(GROUPS + "/{id}/settlement", groupId))
                    .andExpect(jsonPath("$.transaction_count").value(1))
                    .andExpect(jsonPath("$.transfers[0].from_name").value("Citra"))
                    .andExpect(jsonPath("$.balances[1].net").value("0.00"))
                    // service charge is on expenses, payments don't change it
                    .andExpect(jsonPath("$.service_charge_amount").value("171000.00"));

            mvc.perform(get(GROUPS + "/{id}/payments", groupId)).andExpect(jsonPath("$", hasSize(1)));
        }

        @Test
        void overpaymentIsRejected() throws Exception {
            addBaliTripExpenses();

            recordPayment(citra, andi, "600000")
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error_code").value("INVALID_PAYMENT"));
        }

        @Test
        void participantWhoIsOwedMoneyCannotPay() throws Exception {
            addBaliTripExpenses();

            recordPayment(andi, budi, "1000")
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error_code").value("INVALID_PAYMENT"));
        }

        @Test
        void cannotPayYourself() throws Exception {
            recordPayment(andi, andi, "1000")
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.error_code").value("INVALID_PAYMENT"));
        }
    }

    @Nested
    @DisplayName("groups")
    class Groups {

        @Test
        void unknownGroupIs404() throws Exception {
            mvc.perform(get(GROUPS + "/{id}/settlement", UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error_code").value("GROUP_NOT_FOUND"));
        }

        @Test
        void malformedIdIs400() throws Exception {
            mvc.perform(get(GROUPS + "/{id}", "not-a-uuid")).andExpect(status().isBadRequest());
        }

        @Test
        void participantNamesAreUniqueCaseInsensitive() throws Exception {
            mvc.perform(post(GROUPS + "/{id}/participants", groupId)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"andi\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error_code").value("DUPLICATE_PARTICIPANT"));
        }

        @Test
        void participantCanJoinLater() throws Exception {
            mvc.perform(post(GROUPS + "/{id}/participants", groupId)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Dewi\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Dewi"));

            mvc.perform(get(GROUPS + "/{id}", groupId)).andExpect(jsonPath("$.participants", hasSize(4)));
        }

        @Test
        void unsupportedCurrencyIsRejected() throws Exception {
            mvc.perform(post(GROUPS).contentType(MediaType.APPLICATION_JSON).content("""
                            {"name": "Trip", "currency": "XYZ", "participants": ["A"]}
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_code").value("INVALID_CURRENCY"));
        }

        @Test
        void currencyDefaultsToIdr() throws Exception {
            mvc.perform(post(GROUPS).contentType(MediaType.APPLICATION_JSON).content("""
                            {"name": "Lunch", "participants": ["A", "B"]}
                            """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.currency").value("IDR"));
        }
    }

    @Test
    void auditTrailRecordsEveryChange() throws Exception {
        addBaliTripExpenses();
        recordPayment(budi, andi, "100000").andExpect(status().isCreated());

        mvc.perform(get(GROUPS + "/{id}/audit-events", groupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[*].action", hasItems("GROUP_CREATED", "EXPENSE_ADDED", "PAYMENT_RECORDED")));
    }
}
