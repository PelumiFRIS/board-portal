package com.fris.boardportal.resource.dto;

import com.fris.boardportal.resource.ResourceCategory;

public record UpdateResourceRequest(ResourceCategory category, String title, String body) {
}
