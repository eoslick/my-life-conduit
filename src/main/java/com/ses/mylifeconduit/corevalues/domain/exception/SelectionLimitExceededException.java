
package com.ses.mylifeconduit.corevalues.domain.exception;

public final class SelectionLimitExceededException extends CoreValuesException {
    public SelectionLimitExceededException(int limit) {
        super("Cannot select core value: Selection limit of " + limit + " already reached.");
    }
}