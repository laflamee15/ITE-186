package lendwise.utils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lendwise.models.Borrower;
import lendwise.models.Loan;
import lendwise.models.Payment;

public class AppDataStore {
    private static final Path DATA_DIR = Paths.get("data");
    private static final Path DATA_FILE = DATA_DIR.resolve("lendwise-data.dat");
    private final String ownerEmail;

    public AppDataStore(String ownerEmail) {
        this.ownerEmail = normalizeOwnerEmail(ownerEmail);
        try {
            DatabaseManager.initializeSchema();
            migrateLegacyFileToDatabaseIfNeeded();
        } catch (SQLException ignored) {
        }
    }

    public void loadInto(List<Borrower> borrowers, List<Loan> loans, List<Payment> payments) {
        borrowers.clear();
        loans.clear();
        payments.clear();

        AppData data = loadFromDatabase();
        if (data == null) {
            data = loadLegacyFile();
        }

        borrowers.addAll(data.borrowers);
        loans.addAll(data.loans);
        payments.addAll(data.payments);
        attachLoansToBorrowers(borrowers, loans);
    }

    public void save(List<Borrower> borrowers, List<Loan> loans, List<Payment> payments) {
        if (saveToDatabase(borrowers, loans, payments)) {
            return;
        }
        saveLegacyFile(borrowers, loans, payments);
    }

    private boolean saveToDatabase(List<Borrower> borrowers, List<Loan> loans, List<Payment> payments) {
        try (Connection connection = DatabaseManager.openConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement deletePayments = connection.prepareStatement("DELETE FROM payments WHERE owner_email = ?");
                     PreparedStatement deleteLoans = connection.prepareStatement("DELETE FROM loans WHERE owner_email = ?");
                     PreparedStatement deleteBorrowers = connection.prepareStatement("DELETE FROM borrowers WHERE owner_email = ?")) {
                    deletePayments.setString(1, ownerEmail);
                    deleteLoans.setString(1, ownerEmail);
                    deleteBorrowers.setString(1, ownerEmail);
                    deletePayments.executeUpdate();
                    deleteLoans.executeUpdate();
                    deleteBorrowers.executeUpdate();
                }

                try (PreparedStatement borrowerStatement = connection.prepareStatement(
                    "INSERT INTO borrowers(id, name, gmail, address, owner_email) VALUES (?, ?, ?, ?, ?)"
                )) {
                    for (Borrower borrower : borrowers == null ? List.<Borrower>of() : borrowers) {
                        if (borrower == null) {
                            continue;
                        }
                        borrowerStatement.setString(1, borrower.getId());
                        borrowerStatement.setString(2, borrower.getFullName());
                        borrowerStatement.setString(3, borrower.getGmail());
                        borrowerStatement.setString(4, borrower.getAddress());
                        borrowerStatement.setString(5, ownerEmail);
                        borrowerStatement.addBatch();
                    }
                    borrowerStatement.executeBatch();
                }

                try (PreparedStatement loanStatement = connection.prepareStatement(
                    """
                    INSERT INTO loans(
                        id, borrower_id, principal_amount, interest_rate_annual,
                        original_term_months, term_months, start_date, collector_name, status, owner_email
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """
                )) {
                    for (Loan loan : loans == null ? List.<Loan>of() : loans) {
                        if (loan == null) {
                            continue;
                        }
                        loanStatement.setString(1, loan.getId());
                        loanStatement.setString(2, loan.getBorrowerId());
                        loanStatement.setDouble(3, loan.getPrincipalAmount());
                        loanStatement.setDouble(4, loan.getInterestRateAnnual());
                        loanStatement.setInt(5, loan.getOriginalTermMonths());
                        loanStatement.setInt(6, loan.getTermMonths());
                        loanStatement.setString(7, loan.getStartDate() == null ? null : loan.getStartDate().toString());
                        loanStatement.setString(8, loan.getCollectorName());
                        loanStatement.setString(9, loan.getStatus());
                        loanStatement.setString(10, ownerEmail);
                        loanStatement.addBatch();
                    }
                    loanStatement.executeBatch();
                }

                try (PreparedStatement paymentStatement = connection.prepareStatement(
                    "INSERT INTO payments(id, loan_id, amount, payment_date, method, owner_email) VALUES (?, ?, ?, ?, ?, ?)"
                )) {
                    for (Payment payment : payments == null ? List.<Payment>of() : payments) {
                        if (payment == null) {
                            continue;
                        }
                        paymentStatement.setString(1, payment.getId());
                        paymentStatement.setString(2, payment.getLoanId());
                        paymentStatement.setDouble(3, payment.getAmount());
                        paymentStatement.setString(4, payment.getPaymentDate() == null ? null : payment.getPaymentDate().toString());
                        paymentStatement.setString(5, payment.getMethod());
                        paymentStatement.setString(6, ownerEmail);
                        paymentStatement.addBatch();
                    }
                    paymentStatement.executeBatch();
                }

                connection.commit();
                return true;
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ignored) {
        }
        return false;
    }

    private void saveLegacyFile(List<Borrower> borrowers, List<Loan> loans, List<Payment> payments) {
        try {
            Files.createDirectories(DATA_DIR);
            try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(DATA_FILE))) {
                output.writeObject(new AppData(borrowers, loans, payments));
            }
        } catch (IOException ignored) {
        }
    }

    private AppData loadFromDatabase() {
        if (!DatabaseManager.isReady()) {
            return null;
        }

        try (Connection connection = DatabaseManager.openConnection()) {
            ArrayList<Borrower> borrowers = new ArrayList<>();
            ArrayList<Loan> loans = new ArrayList<>();
            ArrayList<Payment> payments = new ArrayList<>();

            try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, name, gmail, address FROM borrowers WHERE owner_email = ? ORDER BY id"
            )) {
                statement.setString(1, ownerEmail);
                try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    borrowers.add(new Borrower(
                        resultSet.getString("id"),
                        resultSet.getString("name"),
                        resultSet.getString("gmail"),
                        resultSet.getString("address")
                    ));
                }
            }
            }

            try (PreparedStatement statement = connection.prepareStatement(
                """
                SELECT id, borrower_id, principal_amount, interest_rate_annual,
                       original_term_months, term_months, start_date, collector_name, status
                FROM loans WHERE owner_email = ? ORDER BY id
                """
            )) {
                statement.setString(1, ownerEmail);
                try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Loan loan = new Loan(
                        resultSet.getString("id"),
                        resultSet.getString("borrower_id"),
                        resultSet.getDouble("principal_amount"),
                        resultSet.getDouble("interest_rate_annual"),
                        resultSet.getInt("original_term_months"),
                        parseDate(resultSet.getString("start_date")),
                        resultSet.getString("collector_name"),
                        resultSet.getString("status")
                    );
                    loan.setOriginalTermMonths(resultSet.getInt("original_term_months"));
                    loan.setTermMonths(resultSet.getInt("term_months"));
                    loans.add(loan);
                }
            }
            }

            try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, loan_id, amount, payment_date, method FROM payments WHERE owner_email = ? ORDER BY id"
            )) {
                statement.setString(1, ownerEmail);
                try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    payments.add(new Payment(
                        resultSet.getString("id"),
                        resultSet.getString("loan_id"),
                        resultSet.getDouble("amount"),
                        parseDate(resultSet.getString("payment_date")),
                        resultSet.getString("method")
                    ));
                }
            }
            }

            return new AppData(borrowers, loans, payments);
        } catch (SQLException ignored) {
        }
        return null;
    }

    private AppData loadLegacyFile() {
        if (!Files.exists(DATA_FILE)) {
            return new AppData();
        }
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(DATA_FILE))) {
            Object stored = input.readObject();
            if (stored instanceof AppData appData) {
                return appData;
            }
        } catch (IOException | ClassNotFoundException ignored) {
        }
        return new AppData();
    }

    private void migrateLegacyFileToDatabaseIfNeeded() throws SQLException {
        if (!normalizeOwnerEmail(ownerEmail).isEmpty() && claimUnownedDatabaseRows()) {
            return;
        }

        AppData databaseData = loadFromDatabase();
        if (databaseData != null && (!databaseData.borrowers.isEmpty() || !databaseData.loans.isEmpty() || !databaseData.payments.isEmpty())) {
            return;
        }

        AppData legacyData = loadLegacyFile();
        if (legacyData.borrowers.isEmpty() && legacyData.loans.isEmpty() && legacyData.payments.isEmpty()) {
            return;
        }

        saveToDatabase(legacyData.borrowers, legacyData.loans, legacyData.payments);
    }

    private boolean claimUnownedDatabaseRows() throws SQLException {
        try (Connection connection = DatabaseManager.openConnection()) {
            int ownedBorrowers = countRows(connection, "borrowers", "owner_email = ?", ownerEmail);
            int ownedLoans = countRows(connection, "loans", "owner_email = ?", ownerEmail);
            int ownedPayments = countRows(connection, "payments", "owner_email = ?", ownerEmail);
            if (ownedBorrowers > 0 || ownedLoans > 0 || ownedPayments > 0) {
                return true;
            }

            int unownedBorrowers = countRows(connection, "borrowers", "owner_email IS NULL OR TRIM(owner_email) = ''");
            int unownedLoans = countRows(connection, "loans", "owner_email IS NULL OR TRIM(owner_email) = ''");
            int unownedPayments = countRows(connection, "payments", "owner_email IS NULL OR TRIM(owner_email) = ''");
            if (unownedBorrowers == 0 && unownedLoans == 0 && unownedPayments == 0) {
                return false;
            }

            connection.setAutoCommit(false);
            try {
                updateOwner(connection, "borrowers", ownerEmail);
                updateOwner(connection, "loans", ownerEmail);
                updateOwner(connection, "payments", ownerEmail);
                connection.commit();
                return true;
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private int countRows(Connection connection, String tableName, String whereClause, String... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "SELECT COUNT(*) FROM " + tableName + " WHERE " + whereClause
        )) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setString(i + 1, parameters[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    private void updateOwner(Connection connection, String tableName, String nextOwnerEmail) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            "UPDATE " + tableName + " SET owner_email = ? WHERE owner_email IS NULL OR TRIM(owner_email) = ''"
        )) {
            statement.setString(1, nextOwnerEmail);
            statement.executeUpdate();
        }
    }

    private void attachLoansToBorrowers(List<Borrower> borrowers, List<Loan> loans) {
        Map<String, Borrower> borrowersById = new LinkedHashMap<>();
        for (Borrower borrower : borrowers) {
            if (borrower == null) {
                continue;
            }
            borrower.setLoans(new ArrayList<>());
            borrowersById.put(borrower.getId(), borrower);
        }

        for (Loan loan : loans) {
            if (loan == null) {
                continue;
            }
            Borrower borrower = borrowersById.get(loan.getBorrowerId());
            if (borrower != null) {
                borrower.addLoan(loan);
            }
        }
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(value.trim());
    }

    private String normalizeOwnerEmail(String email) {
        return email == null ? "" : email.trim();
    }

    private static final class AppData implements Serializable {
        private static final long serialVersionUID = 1L;

        private final ArrayList<Borrower> borrowers;
        private final ArrayList<Loan> loans;
        private final ArrayList<Payment> payments;

        private AppData() {
            this.borrowers = new ArrayList<>();
            this.loans = new ArrayList<>();
            this.payments = new ArrayList<>();
        }

        private AppData(List<Borrower> borrowers, List<Loan> loans, List<Payment> payments) {
            this.borrowers = new ArrayList<>(borrowers == null ? List.of() : borrowers);
            this.loans = new ArrayList<>(loans == null ? List.of() : loans);
            this.payments = new ArrayList<>(payments == null ? List.of() : payments);
        }
    }
}
