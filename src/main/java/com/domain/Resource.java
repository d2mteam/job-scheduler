package com.domain;

public record Resource(String type, String id) {
    @Override
    public String toString() {
        return type + ":" + id;
    }

    public static Resource of(String type, Object id) {
        return new Resource(type, String.valueOf(id));
    }
}