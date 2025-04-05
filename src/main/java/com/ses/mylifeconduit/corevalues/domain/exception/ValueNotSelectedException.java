package com.ses.mylifeconduit.corevalues.domain.exception;

import com.ses.mylifeconduit.corevalues.domain.vo.CoreValueId;

public final class ValueNotSelectedException extends CoreValuesException {
    public ValueNotSelectedException(CoreValueId coreValueId) {
        super("Cannot deselect core value: Value with ID " + coreValueId.toString() + " is not currently selected.");
    }
}