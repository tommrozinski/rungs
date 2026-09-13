package com.rungs.readiness.adapter.in.web.view;

/**
 * One row of the debts table (027 REQ-002, REQ-004): its label, type, balance, rate, and minimum payment fields, and
 * where its "Remove" button posts.
 *
 * @param label the debt's name field
 * @param typeId the type select's element id
 * @param typeName the type select's form field name ({@code debts[N].type})
 * @param typeValue the currently selected {@code DebtType} name, or blank when none is selected
 * @param balance the balance field
 * @param rate the annual interest rate field
 * @param minimumPayment the minimum payment field
 * @param removeAction where this row's "Remove" button posts ({@code /snapshot/debts/remove/N})
 */
public record DebtRowView(FieldView label, String typeId, String typeName, String typeValue, FieldView balance,
        FieldView rate, FieldView minimumPayment, String removeAction) {
}
