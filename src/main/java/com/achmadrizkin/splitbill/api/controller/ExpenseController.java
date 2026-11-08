package com.achmadrizkin.splitbill.api.controller;

import com.achmadrizkin.splitbill.api.dto.Requests.AddExpenseRequest;
import com.achmadrizkin.splitbill.api.dto.Requests.VoidExpenseRequest;
import com.achmadrizkin.splitbill.api.dto.Responses.ExpenseResponse;
import com.achmadrizkin.splitbill.application.ExpenseService;
import com.achmadrizkin.splitbill.application.view.Views.ExpenseView;
import com.achmadrizkin.splitbill.application.view.Views.IdempotentResult;
import com.achmadrizkin.splitbill.domain.expense.ExpenseCategory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/groups/{groupId}/expenses")
@Tag(name = "Expenses", description = "Record, list and void shared expenses")
public class ExpenseController {

    private final ExpenseService expenses;

    public ExpenseController(ExpenseService expenses) {
        this.expenses = expenses;
    }

    @PostMapping
    @Operation(summary = "Add an expense (EQUAL, EXACT, PERCENTAGE or SHARES split). Supports Idempotency-Key.")
    public ResponseEntity<ExpenseResponse> add(
            @PathVariable UUID groupId,
            @RequestHeader(name = ApiHeaders.IDEMPOTENCY_KEY, required = false) String idempotencyKey,
            @Valid @RequestBody AddExpenseRequest request) {
        IdempotentResult<ExpenseView> result = expenses.add(groupId, request.toCommand(), idempotencyKey);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(result.value().id()).toUri();
        return ResponseEntity.created(location)
                .header(ApiHeaders.IDEMPOTENT_REPLAYED, String.valueOf(result.replayed()))
                .body(ExpenseResponse.from(result.value()));
    }

    @GetMapping
    @Operation(summary = "List expenses (including voided), optionally filtered by category")
    public List<ExpenseResponse> list(@PathVariable UUID groupId,
                                      @RequestParam(required = false) ExpenseCategory category) {
        return expenses.list(groupId, category).stream().map(ExpenseResponse::from).toList();
    }

    @GetMapping("/{expenseId}")
    @Operation(summary = "Get one expense with its per-participant shares")
    public ExpenseResponse get(@PathVariable UUID groupId, @PathVariable UUID expenseId) {
        return ExpenseResponse.from(expenses.get(groupId, expenseId));
    }

    @PostMapping("/{expenseId}/void")
    @Operation(summary = "Void an expense. Expenses are never deleted, so the audit trail stays intact.")
    public ExpenseResponse voidExpense(@PathVariable UUID groupId, @PathVariable UUID expenseId,
                                       @Valid @RequestBody VoidExpenseRequest request) {
        return ExpenseResponse.from(expenses.voidExpense(groupId, expenseId, request.reason()));
    }
}
