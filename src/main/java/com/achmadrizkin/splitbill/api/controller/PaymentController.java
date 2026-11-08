package com.achmadrizkin.splitbill.api.controller;

import com.achmadrizkin.splitbill.api.dto.Requests.RecordPaymentRequest;
import com.achmadrizkin.splitbill.api.dto.Responses.PaymentResponse;
import com.achmadrizkin.splitbill.application.PaymentService;
import com.achmadrizkin.splitbill.application.view.Views.IdempotentResult;
import com.achmadrizkin.splitbill.application.view.Views.PaymentView;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/groups/{groupId}/payments")
@Tag(name = "Payments", description = "Record settle-up payments between participants")
public class PaymentController {

    private final PaymentService payments;

    public PaymentController(PaymentService payments) {
        this.payments = payments;
    }

    @PostMapping
    @Operation(summary = "Record that one participant paid another. Supports Idempotency-Key.")
    public ResponseEntity<PaymentResponse> record(
            @PathVariable UUID groupId,
            @RequestHeader(name = ApiHeaders.IDEMPOTENCY_KEY, required = false) String idempotencyKey,
            @Valid @RequestBody RecordPaymentRequest request) {
        IdempotentResult<PaymentView> result = payments.record(groupId, request.toCommand(), idempotencyKey);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(result.value().id()).toUri();
        return ResponseEntity.created(location)
                .header(ApiHeaders.IDEMPOTENT_REPLAYED, String.valueOf(result.replayed()))
                .body(PaymentResponse.from(result.value()));
    }

    @GetMapping
    @Operation(summary = "List recorded payments")
    public List<PaymentResponse> list(@PathVariable UUID groupId) {
        return payments.list(groupId).stream().map(PaymentResponse::from).toList();
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get one payment")
    public PaymentResponse get(@PathVariable UUID groupId, @PathVariable UUID paymentId) {
        return PaymentResponse.from(payments.get(groupId, paymentId));
    }
}
