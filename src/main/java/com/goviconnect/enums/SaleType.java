package com.goviconnect.enums;

public enum SaleType {
    RETAIL("Retail Only"),
    WHOLESALE("Wholesale Only"),
    BOTH("Retail & Wholesale");

    private final String displayName;

    SaleType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
