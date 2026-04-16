package lendwise.services;

import lendwise.models.Loan;

public class LoanCalculator {
    private static final double EPSILON = 0.000001;

    /**
     * Computes the total payable amount using a fixed monthly amortization schedule.
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

        double installment = computeMonthlyInstallment(principal, annualInterestRate, termMonths);
        return installment * termMonths;
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
     * Computes the equal monthly installment using a standard amortization formula.
     *
     * @param principal           principal loan amount
     * @param annualInterestRate  annual interest rate in percent
     * @param termMonths          loan term in months
     * @return monthly installment amount
     */
    public double computeMonthlyInstallment(double principal, double annualInterestRate, int termMonths) {
        if (principal <= 0 || termMonths <= 0) {
            return 0.0;
        }

        double monthlyRate = monthlyRate(annualInterestRate);
        if (Math.abs(monthlyRate) < EPSILON) {
            return principal / termMonths;
        }

        double growth = Math.pow(1 + monthlyRate, termMonths);
        return principal * monthlyRate * growth / (growth - 1);
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
     * Computes the remaining balance from the amortized total payable.
     *
     * @param principal           principal loan amount
     * @param annualInterestRate  annual interest rate in percent
     * @param termMonths          loan term in months
     * @param amountPaid          total amount already paid
     * @return remaining balance (never negative)
     */
    public double computeRemainingBalance(double principal, double annualInterestRate, int termMonths, double amountPaid) {
        if (principal <= 0 || termMonths <= 0) {
            return 0.0;
        }

        double totalPayable = computeTotalPayable(principal, annualInterestRate, termMonths);
        double remaining = totalPayable - amountPaid;
        return remaining < EPSILON ? 0.0 : remaining;
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

    public int computeCompletedInstallments(double principal, double annualInterestRate, int termMonths, double amountPaid) {
        if (principal <= 0 || termMonths <= 0 || amountPaid <= 0) {
            return 0;
        }

        double installment = computeMonthlyInstallment(principal, annualInterestRate, termMonths);
        if (installment <= 0.0) {
            return 0;
        }

        int completed = (int) Math.floor((amountPaid + EPSILON) / installment);
        if (completed < 0) {
            return 0;
        }
        return Math.min(termMonths, completed);
    }

    public int computeCompletedInstallments(Loan loan, double amountPaid) {
        if (loan == null) {
            return 0;
        }
        return computeCompletedInstallments(
            loan.getPrincipalAmount(),
            loan.getInterestRateAnnual(),
            loan.getOriginalTermMonths(),
            amountPaid
        );
    }

    public int computeRemainingTermMonths(Loan loan, double amountPaid) {
        if (loan == null) {
            return 0;
        }
        int originalTermMonths = loan.getOriginalTermMonths();
        if (originalTermMonths <= 0) {
            return 0;
        }
        if ("PAID".equalsIgnoreCase(loan.getStatus())) {
            return 0;
        }
        int completedInstallments = computeCompletedInstallments(loan, amountPaid);
        return Math.max(0, originalTermMonths - completedInstallments);
    }

    public double computeCurrentInstallmentDue(Loan loan, double amountPaid) {
        if (loan == null) {
            return 0.0;
        }

        double remaining = computeRemainingBalance(loan, amountPaid);
        if (remaining <= 0.0) {
            return 0.0;
        }

        double installment = computeMonthlyInstallment(loan);
        if (installment <= 0.0) {
            return remaining;
        }

        int completedInstallments = computeCompletedInstallments(loan, amountPaid);
        double paidTowardCurrentInstallment = Math.max(0.0, amountPaid - (completedInstallments * installment));
        double installmentDue = installment - paidTowardCurrentInstallment;
        if (installmentDue < EPSILON) {
            installmentDue = installment;
        }
        return Math.min(remaining, installmentDue);
    }

    private double monthlyRate(double annualInterestRate) {
        return annualInterestRate / 100.0 / 12.0;
    }
}
