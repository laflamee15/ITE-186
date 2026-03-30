package lendwise.services;

import lendwise.models.Loan;

public class LoanCalculator {

    /**
     * Computes the total payable amount using the simple interest formula:
     * A = P(1 + r * t)
     *
     * @param principal           principal loan amount (P)
     * @param annualInterestRate  annual interest rate in percent (e.g. 10 for 10%)
     * @param termMonths          loan term in months
     * @return total payable amount (A)
     */
    public double computeTotalPayable(double principal, double annualInterestRate, int termMonths) {
        if (principal <= 0 || termMonths <= 0) {
            return 0.0;
        }

        double r = annualInterestRate / 100.0;      // convert percent to decimal
        double tYears = termMonths / 12.0;          // convert months to years

        return principal * (1 + r * tYears);
    }

    /**
     * Overload for Loan model.
     */
    public double computeTotalPayable(Loan loan) {
        if (loan == null) {
            return 0.0;
        }
        return computeTotalPayable(
                loan.getPrincipalAmount(),
                loan.getInterestRateAnnual(),
                loan.getOriginalTermMonths()
        );
    }

    /**
     * Computes the equal monthly installment based on simple interest total.
     *
     * @param principal           principal loan amount
     * @param annualInterestRate  annual interest rate in percent
     * @param termMonths          loan term in months
     * @return monthly installment amount
     */
    public double computeMonthlyInstallment(double principal, double annualInterestRate, int termMonths) {
        if (termMonths <= 0) {
            return 0.0;
        }
        double total = computeTotalPayable(principal, annualInterestRate, termMonths);
        return total / termMonths;
    }

    /**
     * Overload for Loan model.
     */
    public double computeMonthlyInstallment(Loan loan) {
        if (loan == null) {
            return 0.0;
        }
        return computeMonthlyInstallment(
                loan.getPrincipalAmount(),
                loan.getInterestRateAnnual(),
                loan.getOriginalTermMonths()
        );
    }

    /**
     * Computes the remaining balance based on the total payable and the amount already paid.
     *
     * @param principal           principal loan amount
     * @param annualInterestRate  annual interest rate in percent
     * @param termMonths          loan term in months
     * @param amountPaid          total amount already paid
     * @return remaining balance (never negative)
     */
    public double computeRemainingBalance(double principal, double annualInterestRate, int termMonths, double amountPaid) {
        double total = computeTotalPayable(principal, annualInterestRate, termMonths);
        double remaining = total - amountPaid;
        return remaining < 0 ? 0.0 : remaining;
    }

    /**
     * Overload for Loan model.
     */
    public double computeRemainingBalance(Loan loan, double amountPaid) {
        if (loan == null) {
            return 0.0;
        }
        String status = loan.getStatus() == null ? "" : loan.getStatus().trim().toUpperCase();
        if ("PAID".equals(status)) {
            return 0.0;
        }
        return computeRemainingBalance(
            loan.getPrincipalAmount(),
            loan.getInterestRateAnnual(),
            loan.getOriginalTermMonths(),
            amountPaid
        );
    }
}

