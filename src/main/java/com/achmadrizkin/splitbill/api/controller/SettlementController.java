package com.achmadrizkin.splitbill.api.controller;

import com.achmadrizkin.splitbill.api.dto.Responses.BalanceResponse;
import com.achmadrizkin.splitbill.api.dto.Responses.SettlementResponse;
import com.achmadrizkin.splitbill.application.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/groups/{groupId}")
@Tag(name = "Settlement", description = "Balances and who-owes-whom")
public class SettlementController {

    private final SettlementService settlements;

    public SettlementController(SettlementService settlements) {
        this.settlements = settlements;
    }

    @GetMapping("/balances")
    @Operation(summary = "Net balance per participant (positive = is owed money)")
    public List<BalanceResponse> balances(@PathVariable UUID groupId) {
        return settlements.balances(groupId).stream().map(BalanceResponse::from).toList();
    }

    @GetMapping("/settlement")
    @Operation(summary = "Settlement summary: minimal transfers, category totals and service charge")
    public SettlementResponse settlement(@PathVariable UUID groupId) {
        return SettlementResponse.from(settlements.settle(groupId));
    }
}
