package com.paymentgateway.enums;

public enum PaymentStatus {
    CREATED,
    INITIATED,
    PROCESSING,
    SUCCESS,
    FAILED,
    EXPIRED;

    public boolean canTransitionTo(PaymentStatus next) {
        return switch (this) {
            case CREATED    -> next == INITIATED;
            case INITIATED  -> next == PROCESSING
                    || next == FAILED
                    || next == EXPIRED;
            case PROCESSING -> next == SUCCESS
                    || next == FAILED;
            case SUCCESS, FAILED, EXPIRED -> false;
        };
    }

    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED || this == EXPIRED;
    }
}
