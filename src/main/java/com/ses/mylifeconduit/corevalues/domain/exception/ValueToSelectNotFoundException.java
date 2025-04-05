package com.ses.mylifeconduit.corevalues.domain.exception;

import com.ses.mylifeconduit.corevalues.domain.vo.CoreValueId;

public final class ValueToSelectNotFoundException extends CoreValuesException {
    public ValueToSelectNotFoundException(CoreValueId coreValueId) {
        super("Cannot select core value: Value with ID " + coreValueId.toString() + " not found (neither system nor custom).");
    }
}