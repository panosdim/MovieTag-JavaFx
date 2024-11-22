package com.panosdim.movietag;

public enum StatusType {
    INFO("-fx-text-fill: black;"),
    WARNING("-fx-text-fill: orange;"),
    ERROR("-fx-text-fill: red;");

    private final String style;

    StatusType(String style) {
        this.style = style;
    }

    public String getStyle() {
        return style;
    }
}