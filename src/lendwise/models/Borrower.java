package lendwise.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Borrower implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String contactNumber;
    private String address;
    private List<Loan> loans;

    public Borrower() {
        this.loans = new ArrayList<>();
    }

    public Borrower(String id, String name, String contactNumber, String address) {
        this.id = id;
        this.name = name;
        this.contactNumber = contactNumber;
        this.address = address;
        this.loans = new ArrayList<>();
    }

    public Borrower(String id, String name, String contactNumber, String address, List<Loan> loans) {
        this.id = id;
        this.name = name;
        this.contactNumber = contactNumber;
        this.address = address;
        this.loans = loans == null ? new ArrayList<>() : loans;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // Kept for compatibility with existing UI ("Full Name" column)
    public String getFullName() {
        return name;
    }

    public void setFullName(String fullName) {
        this.name = fullName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<Loan> getLoans() {
        if (loans == null) {
            loans = new ArrayList<>();
        }
        return loans;
    }

    public void setLoans(List<Loan> loans) {
        this.loans = loans == null ? new ArrayList<>() : loans;
    }

    public void addLoan(Loan loan) {
        if (loan == null) {
            return;
        }
        getLoans().add(loan);
    }

    public void removeLoan(Loan loan) {
        if (loan == null || loans == null) {
            return;
        }
        loans.remove(loan);
    }
}

