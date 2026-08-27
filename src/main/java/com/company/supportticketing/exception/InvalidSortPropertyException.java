package com.company.supportticketing.exception;

import java.util.Set;

public class InvalidSortPropertyException extends RuntimeException {
    public InvalidSortPropertyException(String property, Set<String> allowedProperties) {
        super("Unsupported sort property '" + property + "'. Allowed properties: "
                + String.join(", ", allowedProperties));
    }
}
