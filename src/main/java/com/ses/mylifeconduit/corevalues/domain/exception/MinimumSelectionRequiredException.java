package com.ses.mylifeconduit.corevalues.domain.exception;

public final class MinimumSelectionRequiredException extends CoreValuesException {
    public MinimumSelectionRequiredException(int minimum) {
        super("Cannot deselect core value: Minimum selection of " + minimum + " required.");
    }
}