package com.ses.mylifeconduit.corevalues.domain.exception;

import com.ses.mylifeconduit.corevalues.domain.vo.CoreValueText;

public final class CustomValueTextAlreadyExistsException extends CoreValuesException {
    public CustomValueTextAlreadyExistsException(CoreValueText coreValueText) {
        super("Cannot add custom value: Text '" + coreValueText.value() + "' already exists in custom values.");
    }
}