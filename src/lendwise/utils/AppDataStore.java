package lendwise.utils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lendwise.models.Borrower;
import lendwise.models.Loan;
import lendwise.models.Payment;

public class AppDataStore {
    private static final Path DATA_DIR = Paths.get("data");
    private static final Path DATA_FILE = DATA_DIR.resolve("lendwise-data.dat");
    private static final Path DATABASE_FILE = DATA_DIR.resolve("lendwise.db");
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
        normalizePaymentLoanReferences(loans, payments);
        attachLoansToBorrowers(borrowers, loans);
    }

    public void loadBorrowerViewInto(String borrowerEmail, List<Borrower> borrowers, List<Loan> loans, List<Payment> payments) {
        borrowers.clear();
        loans.clear();
        payments.clear();

        AppData data = loadBorrowerViewFromDatabase(borrowerEmail);
        if (data == null) {
            return;
        }

        borrowers.addAll(data.borrowers);
        loans.addAll(data.loans);
        payments.addAll(data.payments);
        normalizePaymentLoanReferences(loans, payments);
        attachLoansToBorrowers(borrowers, loans);
    }

    public void save(List<Borrower> borrowers, List<Loan> loans, List<Payment> payments) {
        if (saveToDatabase(borrowers, loans, payments)) {
            return;
        }
        saveLegacyFile(borrowers, loans, payments);
    }

    private boolean saveToDatabase(List<Borrower> borrowers, List<Loan> loans, List<Payment> payments) {
        List<Borrower> safeBorrowers = sanitizeBorrowers(borrowers);
        List<Loan> safeLoans = sanitizeLoans(safeBorrowers, loans);
        List<Payment> safePayments = sanitizePayments(safeLoans, payments);

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
                    "INSERT INTO borrowers(id, name, gmail, address, linked_account_email, government_id_photo_path, owner_email) VALUES (?, ?, ?, ?, ?, ?, ?)"
                )) {
                    for (Borrower borrower : safeBorrowers) {
                        if (borrower == null) {
                            continue;
                        }
                        borrowerStatement.setString(1, borrower.getId());
                        borrowerStatement.setString(2, borrower.getFullName());
                        borrowerStatement.setString(3, borrower.getGmail());
                        borrowerStatement.setString(4, borrower.getAddress());
                        borrowerStatement.setString(5, borrower.getLinkedAccountEmail());
                        borrowerStatement.setString(6, borrower.getGovernmentIdPhotoPath());
                        borrowerStatement.setString(7, ownerEmail);
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
                    for (Loan loan : safeLoans) {
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
                    for (Payment payment : safePayments) {
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

    private List<Borrower> sanitizeBorrowers(List<Borrower> borrowers) {
        return new ArrayList<>(borrowers == null ? List.of() : borrowers);
    }

    private List<Loan> sanitizeLoans(List<Borrower> borrowers, List<Loan> loans) {
        if (loans == null || loans.isEmpty()) {
            return List.of();
        }
        Set<String> borrowerIds = collectIds(borrowers, true);
        ArrayList<Loan> safeLoans = new ArrayList<>();
        for (Loan loan : loans) {
            if (loan == null) {
                continue;
            }
            String borrowerId = safe(loan.getBorrowerId());
            if (!borrowerId.isEmpty() && borrowerIds.contains(borrowerId)) {
                safeLoans.add(loan);
            }
        }
        return safeLoans;
    }

    private List<Payment> sanitizePayments(List<Loan> loans, List<Payment> payments) {
        if (payments == null || payments.isEmpty()) {
            return List.of();
        }
        Set<String> loanIds = collectIds(loans, false);
        ArrayList<Payment> safePayments = new ArrayList<>();
        for (Payment payment : payments) {
            if (payment == null) {
                continue;
            }
            String loanId = safe(payment.getLoanId());
            if (!loanId.isEmpty() && loanIds.contains(loanId)) {
                safePayments.add(payment);
            }
        }
        return safePayments;
    }

    private Set<String> collectIds(Collection<?> items, boolean borrowerMode) {
        Set<String> ids = new HashSet<>();
        if (items == null) {
            return ids;
        }
        for (Object item : items) {
            if (borrowerMode && item instanceof Borrower borrower) {
                String id = safe(borrower.getId());
                if (!id.isEmpty()) {
                    ids.add(id);
                }
            } else if (!borrowerMode && item instanceof Loan loan) {
                String id = safe(loan.getId());
                if (!id.isEmpty()) {
                    ids.add(id);
                }
            }
        }
        return ids;
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
                "SELECT id, name, gmail, address, linked_account_email, government_id_photo_path FROM borrowers WHERE owner_email = ? ORDER BY id"
            )) {
                statement.setString(1, ownerEmail);
                try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Borrower borrower = new Borrower(
                        resultSet.getString("id"),
                        resultSet.getString("name"),
                        resultSet.getString("gmail"),
                        resultSet.getString("address"),
                        resultSet.getString("linked_account_email"),
                        null
                    );
                    borrower.setGovernmentIdPhotoPath(resultSet.getString("government_id_photo_path"));
                    borrowers.add(borrower);
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

    private AppData loadBorrowerViewFromDatabase(String borrowerEmail) {
        if (!DatabaseManager.isReady()) {
            return null;
        }

        String normalizedBorrowerEmail = normalizeOwnerEmail(borrowerEmail);
        if (normalizedBorrowerEmail.isEmpty()) {
            return new AppData();
        }

        try (Connection connection = DatabaseManager.openConnection()) {
            ArrayList<Borrower> borrowers = new ArrayList<>();
            ArrayList<Loan> loans = new ArrayList<>();
            ArrayList<Payment> payments = new ArrayList<>();

            Borrower matchedBorrower = null;
            try (PreparedStatement statement = connection.prepareStatement(
                """
                SELECT id, name, gmail, address, linked_account_email, government_id_photo_path
                FROM borrowers
                WHERE LOWER(TRIM(linked_account_email)) = LOWER(TRIM(?))
                   OR (
                        (linked_account_email IS NULL OR TRIM(linked_account_email) = '')
                        AND LOWER(TRIM(gmail)) = LOWER(TRIM(?))
                   )
                ORDER BY
                    CASE
                        WHEN LOWER(TRIM(linked_account_email)) = LOWER(TRIM(?)) THEN 0
                        ELSE 1
                    END,
                    owner_email IS NULL OR TRIM(owner_email) = '',
                    owner_email,
                    id
                LIMIT 1
                """
            )) {
                statement.setString(1, normalizedBorrowerEmail);
                statement.setString(2, normalizedBorrowerEmail);
                statement.setString(3, normalizedBorrowerEmail);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        matchedBorrower = new Borrower(
                            resultSet.getString("id"),
                            resultSet.getString("name"),
                            resultSet.getString("gmail"),
                            resultSet.getString("address"),
                            resultSet.getString("linked_account_email"),
                            null
                        );
                        matchedBorrower.setGovernmentIdPhotoPath(resultSet.getString("government_id_photo_path"));
                        borrowers.add(matchedBorrower);
                    }
                }
            }

            if (matchedBorrower == null) {
                return new AppData();
            }

            ensureBorrowerLinkedAccount(connection, matchedBorrower, normalizedBorrowerEmail);

            try (PreparedStatement statement = connection.prepareStatement(
                """
                SELECT id, borrower_id, principal_amount, interest_rate_annual,
                       original_term_months, term_months, start_date, collector_name, status
                FROM loans
                WHERE borrower_id = ?
                ORDER BY id
                """
            )) {
                statement.setString(1, matchedBorrower.getId());
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
                "SELECT id, loan_id, amount, payment_date, method FROM payments WHERE loan_id = ? OR loan_id = ? ORDER BY id"
            )) {
                Set<String> seenPaymentIds = new HashSet<>();
                for (Loan loan : loans) {
                    if (loan == null) {
                        continue;
                    }
                    statement.setString(1, loan.getId());
                    statement.setString(2, matchedBorrower.getId());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        while (resultSet.next()) {
                            String paymentId = resultSet.getString("id");
                            if (!seenPaymentIds.add(safe(paymentId))) {
                                continue;
                            }
                            payments.add(new Payment(
                                paymentId,
                                resultSet.getString("loan_id"),
                                resultSet.getDouble("amount"),
                                parseDate(resultSet.getString("payment_date")),
                                resultSet.getString("method")
                            ));
                        }
                    }
                }
            }

            normalizePaymentLoanReferences(loans, payments);

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
        if (isLegacyFileNewerThanDatabase()) {
            AppData legacyData = loadLegacyFile();
            if (!legacyData.borrowers.isEmpty() || !legacyData.loans.isEmpty() || !legacyData.payments.isEmpty()) {
                saveToDatabase(legacyData.borrowers, legacyData.loans, legacyData.payments);
                return;
            }
        }

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

    private boolean isLegacyFileNewerThanDatabase() {
        try {
            if (!Files.exists(DATA_FILE)) {
                return false;
            }
            if (!Files.exists(DATABASE_FILE)) {
                return true;
            }
            FileTime legacyModified = Files.getLastModifiedTime(DATA_FILE);
            FileTime databaseModified = Files.getLastModifiedTime(DATABASE_FILE);
            return legacyModified.compareTo(databaseModified) > 0;
        } catch (IOException ignored) {
            return false;
        }
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

    private void normalizePaymentLoanReferences(List<Loan> loans, List<Payment> payments) {
        if (loans == null || payments == null || loans.isEmpty() || payments.isEmpty()) {
            return;
        }

        Map<String, Loan> loansById = new LinkedHashMap<>();
        Map<String, ArrayList<Loan>> loansByBorrowerId = new LinkedHashMap<>();
        for (Loan loan : loans) {
            if (loan == null) {
                continue;
            }
            String loanId = safe(loan.getId());
            String borrowerId = safe(loan.getBorrowerId());
            if (!loanId.isEmpty()) {
                loansById.put(loanId, loan);
            }
            if (!borrowerId.isEmpty()) {
                loansByBorrowerId.computeIfAbsent(borrowerId, key -> new ArrayList<>()).add(loan);
            }
        }

        for (Payment payment : payments) {
            if (payment == null) {
                continue;
            }
            String reference = safe(payment.getLoanId());
            if (reference.isEmpty() || loansById.containsKey(reference)) {
                continue;
            }
            ArrayList<Loan> borrowerLoans = loansByBorrowerId.get(reference);
            if (borrowerLoans != null && borrowerLoans.size() == 1) {
                payment.setLoanId(safe(borrowerLoans.get(0).getId()));
            }
        }
    }

    private void ensureBorrowerLinkedAccount(Connection connection, Borrower borrower, String accountEmail) throws SQLException {
        if (borrower == null) {
            return;
        }

        String normalizedAccountEmail = normalizeOwnerEmail(accountEmail);
        if (normalizedAccountEmail.isEmpty()) {
            return;
        }

        String currentLinkedEmail = safe(borrower.getLinkedAccountEmail());
        if (normalizedAccountEmail.equalsIgnoreCase(currentLinkedEmail)) {
            return;
        }

        if (!normalizedAccountEmail.equalsIgnoreCase(safe(borrower.getGmail()))) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement(
            "UPDATE borrowers SET linked_account_email = ? WHERE id = ?"
        )) {
            statement.setString(1, normalizedAccountEmail);
            statement.setString(2, borrower.getId());
            statement.executeUpdate();
        }
        borrower.setLinkedAccountEmail(normalizedAccountEmail);
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

    private String safe(String value) {
        return value == null ? "" : value.trim();
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
