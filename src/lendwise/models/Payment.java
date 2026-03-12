package lendwise.models;

import java.time.LocalDate;

public class Payment {
    private String id;
    private String loanId;
    private double amount;
    private LocalDate paymentDate;
    private String method; // CASH, GCASH, BANK, etc.

    public Payment() {
    }

    public Payment(String id, String loanId, double amount, LocalDate paymentDate, String method) {
        this.id = id;
        this.loanId = loanId;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.method = method;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLoanId() {
        return loanId;
    }

    public void setLoanId(String loanId) {
        this.loanId = loanId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }
}

