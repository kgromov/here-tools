package com.pefrormance.analyzer.model;

public enum Product {
    LC("LC", "LIVE_CACHE"),
    FB("FB", "LIVE_CACHE"),
    THREE_D("3D", "THREE_D"),
    WOM("WOM", "WOM");

    private final String name;
    private final String tableName;

    Product(String name, String tableName) {
        this.name = name;
        this.tableName = tableName;
    }

    public String getTableName() {
        return tableName;
    }

    public static Product getProductByName(String name) {
        switch (name.toUpperCase()) {
            case "LC":
                return LC;
            case "FB":
                return FB;
            case "3D":
                return THREE_D;
            case "WOM":
                return WOM;
            default:
                throw new IllegalArgumentException("Unknown product = " + name);
        }
    }
}
