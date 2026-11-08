package com.achmadrizkin.splitbill.application.command;

import java.util.List;

/** @param currency ISO 4217 code; {@code null} means IDR */
public record CreateGroupCommand(String name, String currency, List<String> participantNames) {
}
