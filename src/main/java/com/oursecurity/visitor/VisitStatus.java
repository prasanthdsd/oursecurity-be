package com.oursecurity.visitor;

public enum VisitStatus {
    /** Resident invited the visitor in advance; visitor has a pass code. */
    PRE_APPROVED,
    /** Guard logged a walk-in visitor; waiting for a resident of the flat to respond. */
    PENDING_APPROVAL,
    /** Resident approved a walk-in; guard can check the visitor in. */
    APPROVED,
    DENIED,
    CHECKED_IN,
    CHECKED_OUT,
    CANCELLED
}
