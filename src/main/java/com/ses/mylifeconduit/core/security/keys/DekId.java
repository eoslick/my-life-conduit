// In package com.ses.mylifeconduit.core.security.keys (or similar)
package com.ses.mylifeconduit.core.security.keys;

import com.ses.mylifeconduit.core.ddd.EntityId;
import java.util.Objects;
import java.util.UUID;

public record DekId(UUID value) implements EntityId {
    public DekId {
        Objects.requireNonNull(value, "DekId value cannot be null");
    }
    public static DekId generate() {
        return new DekId(UUID.randomUUID());
    }
    public static DekId fromString(String uuidString) {
        Objects.requireNonNull(uuidString, "UUID string cannot be null");
        return new DekId(UUID.fromString(uuidString));
    }
    @Override public String toString() { return value.toString(); }
}