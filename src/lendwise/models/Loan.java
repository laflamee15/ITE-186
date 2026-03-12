package lendwise.models;

import java.time.LocalDate;

public class Loan {
    private String id;
    private String borrowerId;
    private double principalAmount;
    private double interestRateAnnual;
    private int termMonths;
    private LocalDate startDate;
    private String status; // e.g., PENDING, ACTIVE, PAID, OVERDUE

    public Loan() {
    }

    public Loan(
            String id,
            String borrowerId,
            double principalAmount,
            double interestRateAnnual,
            int termMonths,
            LocalDate startDate,
            String status
    ) {
        this.id = id;
        this.borrowerId = borrowerId;
        this.principalAmount = principalAmount;
        this.interestRateAnnual = interestRateAnnual;
        this.termMonths = termMonths;
        this.startDate = startDate;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBorrowerId() {
        return borrowerId;
    }

    public void setBorrowerId(String borrowerId) {
        this.borrowerId = borrowerId;
    }

    public double getPrincipalAmount() {
        return principalAmount;
    }

    public void setPrincipalAmount(double principalAmount) {
        this.principalAmount = principalAmount;
    }

    public double getInterestRateAnnual() {
        return interestRateAnnual;
    }

    public void setInterestRateAnnual(double interestRateAnnual) {
        this.interestRateAnnual = interestRateAnnual;
    }

    public int getTermMonths() {
        return termMonths;
    }

    public void setTermMonths(int termMonths) {
        this.termMonths = termMonths;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

