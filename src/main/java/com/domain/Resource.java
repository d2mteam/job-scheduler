package com.domain;

import java.util.Objects;

public final class Resource implements Comparable<Resource> {
    private final String id;

    public Resource(String id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    public String id() {
        return id;
    }

    @Override
    public int compareTo(Resource other) {
        return this.id.compareTo(other.id);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Resource resource)) {
            return false;
        }
        return id.equals(resource.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Resource{" + "id='" + id + '\'' + '}';
    }
}
