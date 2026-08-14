package com.fris.boardportal.actionitem.dto;

import com.fris.boardportal.actionitem.ActionItemStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateActionItemStatusRequest(@NotNull ActionItemStatus status) {
}
