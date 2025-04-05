// src/test/java/com/ses/mylifeconduit/infrastructure/encryption/TestData.java
package com.ses.mylifeconduit.infrastructure.encryption;

import java.io.Serializable;
import java.util.Objects;

/**
 * A simple serializable class for testing encryption/decryption services.
 */
public class TestData implements Serializable {
    // Required for serialization compatibility if the class evolves
    private static final long serialVersionUID = 1L;

    private final String message;
    private final int number;

    public TestData(String message, int number) {
        this.message = message;
        this.number = number;
    }

    public String getMessage() {
        return message;
    }

    public int getNumber() {
        return number;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestData testData = (TestData) o;
        return number == testData.number && Objects.equals(message, testData.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, number);
    }

    @Override
    public String toString() {
        return "TestData{" +
                "message='" + message + '\'' +
                ", number=" + number +
                '}';
    }
}