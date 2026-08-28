package com.fris.boardportal.common;

public final class CsvSupport {

    private CsvSupport() {
    }

    public static String escapeField(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
