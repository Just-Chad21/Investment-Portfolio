package com.enviro.assessment.junior.chadwynprince;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack tests: HTTP request → controller → service → real JPA queries against the seeded H2 data.
 * Each test is rolled back, so every test sees the data.sql state (Thabo id 1, age 45; Grace id 2, 68; Sipho id 3, 60).
 */
// Own H2 database: @AutoConfigureMockMvc makes this a separate Spring context from other tests, and
// the default named in-memory DB outlives a context, so a shared one would already have the schema.
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:api-integration;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@Transactional
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ResultActions postJson(String url, String json) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions withdraw(long investorId, long productId, String amount) throws Exception {
        return postJson("/api/investors/%d/withdrawals".formatted(investorId),
                "{\"productId\": %d, \"amount\": %s}".formatted(productId, amount));
    }

    @Nested
    class Login {

        @Test
        void knownEmailResolvesToInvestorCaseInsensitively() throws Exception {
            postJson("/api/auth/login", "{\"email\": \"Sipho.Dlamini@EXAMPLE.com\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.investorId").value(3))
                    .andExpect(jsonPath("$.firstName").value("Sipho"));
        }

        @Test
        void unknownEmailReturns404ErrorResponse() throws Exception {
            postJson("/api/auth/login", "{\"email\": \"nobody@example.com\"}")
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.path").value("/api/auth/login"));
        }

        @Test
        void malformedEmailReturns400WithFieldError() throws Exception {
            postJson("/api/auth/login", "{\"email\": \"not-an-email\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
        }
    }

    @Nested
    class Portfolio {

        @Test
        void returnsInvestorProductsAndWithdrawalRules() throws Exception {
            mockMvc.perform(get("/api/investors/2/portfolio"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Grace"))
                    .andExpect(jsonPath("$.age").value(68))
                    .andExpect(jsonPath("$.portfolioNumber").value("PF-0002"))
                    .andExpect(jsonPath("$.products", hasSize(3)))
                    .andExpect(jsonPath("$.withdrawalRules.minRetirementAge").value(65))
                    .andExpect(jsonPath("$.withdrawalRules.maxWithdrawalRatio").value(0.9));
        }

        @Test
        void unknownInvestorReturns404() throws Exception {
            mockMvc.perform(get("/api/investors/99/portfolio"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message", containsString("99")));
        }
    }

    @Nested
    class SubmitWithdrawal {

        @Test
        void approvedWithdrawalDebitsTheStoredBalance() throws Exception {
            withdraw(1, 1, "1000.00")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("APPROVED"))
                    .andExpect(jsonPath("$.type").value("STANDARD"))
                    .andExpect(jsonPath("$.balanceAfter").value(44000.0));

            mockMvc.perform(get("/api/investors/1/portfolio"))
                    .andExpect(jsonPath("$.products[?(@.productId == 1)].balance").value(44000.0));
        }

        @Test
        void exactly90PercentIsApproved() throws Exception {
            withdraw(1, 1, "40500.00")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("APPROVED"));
        }

        @Test
        void overNinetyPercentIsRejectedAndRecordedWithoutTouchingBalance() throws Exception {
            withdraw(1, 1, "40500.01")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("REJECTED"))
                    .andExpect(jsonPath("$.rejectionReason").value("Withdrawal amount exceeds 90% of available balance"))
                    .andExpect(jsonPath("$.balanceAfter").doesNotExist());

            mockMvc.perform(get("/api/investors/1/portfolio"))
                    .andExpect(jsonPath("$.products[?(@.productId == 1)].balance").value(45000.0));
            mockMvc.perform(get("/api/investors/1/withdrawals"))
                    .andExpect(jsonPath("$", hasSize(4)))
                    .andExpect(jsonPath("$[0].amount").value(40500.01));
        }

        @Test
        void retirementProductRejectedForInvestorUnder65() throws Exception {
            withdraw(3, 7, "100.00")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("REJECTED"))
                    .andExpect(jsonPath("$.type").value("RETIREMENT"))
                    .andExpect(jsonPath("$.rejectionReason").value("Retirement withdrawals require age > 65 (investor is 60)"));
        }

        @Test
        void retirementProductApprovedForInvestorOver65() throws Exception {
            withdraw(2, 4, "1000.00")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("APPROVED"))
                    .andExpect(jsonPath("$.type").value("RETIREMENT"));
        }

        @Test
        void anotherInvestorsProductIsIndistinguishableFromMissing() throws Exception {
            withdraw(1, 7, "100.00")
                    .andExpect(status().isNotFound());
        }

        @Test
        void nonPositiveAmountReturns400WithFieldError() throws Exception {
            withdraw(1, 1, "-5")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("amount"));
        }

        @Test
        void missingBodyReturns400() throws Exception {
            mockMvc.perform(post("/api/investors/1/withdrawals").contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Request body is missing or malformed"));
        }
    }

    @Nested
    class History {

        @Test
        void unfilteredHistoryIsNewestFirst() throws Exception {
            mockMvc.perform(get("/api/investors/2/withdrawals"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(4)))
                    .andExpect(jsonPath("$[0].productName").value("Money Market"))
                    .andExpect(jsonPath("$[3].amount").value(100000.0));
        }

        @Test
        void filtersByType() throws Exception {
            mockMvc.perform(get("/api/investors/2/withdrawals").param("type", "RETIREMENT"))
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[*].type", everyItem(is("RETIREMENT"))));
        }

        @Test
        void filtersByTypeAndStatusCombined() throws Exception {
            mockMvc.perform(get("/api/investors/2/withdrawals")
                            .param("type", "RETIREMENT")
                            .param("status", "APPROVED"))
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        void toDateIncludesTheWholeDay() throws Exception {
            mockMvc.perform(get("/api/investors/2/withdrawals")
                            .param("from", "2026-09-01")
                            .param("to", "2026-09-01"))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].amount").value(50000.0));
        }

        @Test
        void fromAfterToReturns400() throws Exception {
            mockMvc.perform(get("/api/investors/2/withdrawals")
                            .param("from", "2026-09-10")
                            .param("to", "2026-09-01"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void unknownEnumValueReturns400NamingTheParameter() throws Exception {
            mockMvc.perform(get("/api/investors/2/withdrawals").param("type", "BOGUS"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid value for parameter 'type'"));
        }
    }

    @Nested
    class CsvExport {

        @Test
        void downloadsAsAttachmentWithOneRowPerNotice() throws Exception {
            mockMvc.perform(get("/api/investors/3/withdrawals/export"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("text/csv"))
                    .andExpect(header().string("Content-Disposition",
                            matchesPattern("attachment; filename=\"Sipho_Dlamini_withdrawalhistory_\\d{4}-\\d{2}-\\d{2}\\.csv\"")))
                    .andExpect(content().string(startsWith(
                            "id,productName,type,amount,status,rejectionReason,balanceAfter,requestedAt\n")))
                    .andExpect(content().string(matchesPattern("(?s)([^\\n]*\\n){5}")));
        }

        @Test
        void noMatchesStillReturnsHeaderOnlyCsv() throws Exception {
            mockMvc.perform(get("/api/investors/3/withdrawals/export")
                            .param("type", "RETIREMENT")
                            .param("status", "APPROVED"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(
                            "id,productName,type,amount,status,rejectionReason,balanceAfter,requestedAt\n"));
        }
    }
}
