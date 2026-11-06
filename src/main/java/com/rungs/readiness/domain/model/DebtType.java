package com.rungs.readiness.domain.model;

/**
 * The kind of a consumer debt. There is deliberately no mortgage type: a mortgage is only ever the separate
 * {@link FinancialSnapshot#mortgage()} component.
 */
public enum DebtType {

    /** A loan taken out for education. */
    STUDENT_LOAN,

    /** A loan secured by a vehicle. */
    AUTO,

    /** A revolving credit card balance. */
    CREDIT_CARD,

    /** An unpaid medical bill. */
    MEDICAL,

    /** An unsecured personal loan. */
    PERSONAL,

    /** Any debt that fits none of the other types. */
    OTHER
}
