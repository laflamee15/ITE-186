package lendwise.utils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import lendwise.models.Borrower;
import lendwise.models.Loan;
import lendwise.models.Payment;

public class AppDataStore {
    private static final Path DATA_DIR = Paths.get("data");
    private static final Path DATA_FILE = DATA_DIR.resolve("lendwise-data.dat");

    public void loadInto(List<Borrower> borrowers, List<Loan> loans, List<Payment> payments) {
        borrowers.clear();
        loans.clear();
        payments.clear();

        AppData data = load();
        borrowers.addAll(data.borrowers);
        loans.addAll(data.loans);
        payments.addAll(data.payments);
    }

    public void save(List<Borrower> borrowers, List<Loan> loans, List<Payment> payments) {
        try {
            Files.createDirectories(DATA_DIR);
            try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(DATA_FILE))) {
                output.writeObject(new AppData(borrowers, loans, payments));
            }
        } catch (IOException ignored) {
            // Keep the app usable even if storage fails.
        }
    }

    private AppData load() {
        if (!Files.exists(DATA_FILE)) {
            return new AppData();
        }
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(DATA_FILE))) {
            Object stored = input.readObject();
            if (stored instanceof AppData appData) {
                return appData;
            }
        } catch (IOException | ClassNotFoundException ignored) {
            // Fall back to empty data if the file cannot be read.
        }
        return new AppData();
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
