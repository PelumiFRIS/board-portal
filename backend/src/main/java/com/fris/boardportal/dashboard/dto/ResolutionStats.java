package com.fris.boardportal.dashboard.dto;

public record ResolutionStats(long total, long open, long closed, long passed, long failed, double passRate) {
}
