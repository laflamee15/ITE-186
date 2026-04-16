package lendwise.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import lendwise.models.Borrower;
import lendwise.models.Loan;
import lendwise.models.Payment;
import lendwise.services.LoanCalculator;

public class DashboardPanel extends JPanel {
    private static final Color BLUE = new Color(0x3B, 0x82, 0xF6);
    private static final Color GREEN = new Color(0x22, 0xC5, 0x5E);
    private static final Color ORANGE = new Color(0xF9, 0x73, 0x16);
    private static final Color RED = new Color(0xF8, 0x71, 0x71);
    private static final DateTimeFormatter HUMAN_DATE = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    private final ArrayList<Borrower> borrowers;
    private final ArrayList<Loan> loans;
    private final ArrayList<Payment> payments;
    private final LoanCalculator calculator;
    private final JLabel totalCollectionValue = new JLabel(UITheme.formatCurrency(0.0), SwingConstants.LEFT);
    private final JLabel totalCollectionBody = new JLabel("", SwingConstants.LEFT);
    private final JLabel totalCollectionFooter = new JLabel("", SwingConstants.LEFT);
    private final JLabel totalCollectionPill = new JLabel("", SwingConstants.CENTER);

    private final JLabel activeLoansValue = new JLabel("0", SwingConstants.LEFT);
    private final JLabel activeLoansBody = new JLabel("", SwingConstants.LEFT);
    private final JLabel activeLoansFooter = new JLabel("", SwingConstants.LEFT);
    private final JLabel activeLoansPill = new JLabel("", SwingConstants.CENTER);

    private final JLabel dueSoonValue = new JLabel("0", SwingConstants.LEFT);
    private final JLabel dueSoonBody = new JLabel("", SwingConstants.LEFT);
    private final JLabel dueSoonFooter = new JLabel("", SwingConstants.LEFT);
    private final JLabel dueSoonPill = new JLabel("", SwingConstants.CENTER);

    private final JLabel overdueValue = new JLabel("0", SwingConstants.LEFT);
    private final JLabel overdueBody = new JLabel("", SwingConstants.LEFT);
    private final JLabel overdueFooter = new JLabel("", SwingConstants.LEFT);
    private final JLabel overduePill = new JLabel("", SwingConstants.CENTER);

    private final JLabel subTitleLabel = new JLabel("", SwingConstants.LEFT);
    private final JPanel borrowerProgressList = new JPanel();

    public DashboardPanel(
            ArrayList<Borrower> borrowers,
            ArrayList<Loan> loans,
            ArrayList<Payment> payments,
            LoanCalculator calculator,
            String username
    ) {
        this.borrowers = borrowers;
        this.loans = loans;
        this.payments = payments;
        this.calculator = calculator == null ? new LoanCalculator() : calculator;

        setLayout(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        setBackground(UITheme.CARD);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Dashboard - Portfolio overview", SwingConstants.LEFT);
        title.setForeground(UITheme.TEXT);
        Font titleFont = title.getFont();
        if (titleFont != null) {
            title.setFont(titleFont.deriveFont(Font.BOLD, 28f));
        }

        subTitleLabel.setForeground(UITheme.TEXT_MUTED);
        Font subtitleFont = subTitleLabel.getFont();
        if (subtitleFont != null) {
            subTitleLabel.setFont(subtitleFont.deriveFont(14f));
        }

        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(subTitleLabel);

        wrapper.add(left, BorderLayout.WEST);
        return wrapper;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);

        JPanel cards = new JPanel(new GridLayout(1, 4, 14, 0));
        cards.setOpaque(false);
        cards.add(createMetricCard("DAILY COLLECTION", totalCollectionValue, totalCollectionBody, totalCollectionFooter, totalCollectionPill, "Today", BLUE));
        cards.add(createMetricCard("ACTIVE LOANS", activeLoansValue, activeLoansBody, activeLoansFooter, activeLoansPill, "On-time", GREEN));
        cards.add(createMetricCard("DUE IN 7 DAYS", dueSoonValue, dueSoonBody, dueSoonFooter, dueSoonPill, "Soon", ORANGE));
        cards.add(createMetricCard("OVERDUE LOANS", overdueValue, overdueBody, overdueFooter, overduePill, "Alert", RED));

        body.add(cards, BorderLayout.NORTH);
        body.add(buildBorrowerProgressCard(), BorderLayout.CENTER);
        return body;
    }

    private JPanel createMetricCard(
            String title,
            JLabel valueLabel,
            JLabel bodyLabel,
            JLabel footerLabel,
            JLabel footerPill,
            String badgeText,
            Color accent
    ) {
        RoundedPanel card = new RoundedPanel(24, UITheme.metricCardFill(accent));
        card.setBorderColor(accent);
        card.setBorderWidth(1);
        card.setLayout(new BorderLayout(0, 18));
        card.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);

        JLabel titleLabel = new JLabel(title, SwingConstants.LEFT);
        titleLabel.setForeground(UITheme.metricSecondaryText());
        Font titleFont = titleLabel.getFont();
        if (titleFont != null) {
            titleLabel.setFont(titleFont.deriveFont(Font.BOLD, 12f));
        }

        top.add(titleLabel, BorderLayout.WEST);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        valueLabel.setForeground(UITheme.metricPrimaryText());
        Font valueFont = valueLabel.getFont();
        if (valueFont != null) {
            valueLabel.setFont(valueFont.deriveFont(Font.BOLD, 34f));
        }

        bodyLabel.setForeground(UITheme.metricSecondaryText());
        footerLabel.setForeground(UITheme.metricSecondaryText());
        bodyLabel.setVerticalAlignment(SwingConstants.TOP);
        bodyLabel.setAlignmentX(LEFT_ALIGNMENT);
        bodyLabel.setMinimumSize(new Dimension(0, 34));
        bodyLabel.setPreferredSize(new Dimension(150, 34));
        bodyLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        footerLabel.setAlignmentX(LEFT_ALIGNMENT);

        center.add(valueLabel);
        center.add(Box.createVerticalStrut(10));
        center.add(bodyLabel);
        center.add(Box.createVerticalStrut(16));
        center.add(footerLabel);

        card.add(top, BorderLayout.NORTH);
        card.add(center, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildBorrowerProgressCard() {
        RoundedPanel card = new RoundedPanel(24, UITheme.PANEL_BG);
        card.setBorderColor(UITheme.BORDER);
        card.setBorderWidth(1);
        card.setLayout(new BorderLayout(0, 16));
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Borrower Progress", SwingConstants.LEFT);
        title.setForeground(UITheme.TEXT);
        Font titleFont = title.getFont();
        if (titleFont != null) {
            title.setFont(titleFont.deriveFont(Font.BOLD, 22f));
        }

        JLabel subtitle = new JLabel("Payment progress for each active loan record.", SwingConstants.LEFT);
        subtitle.setForeground(UITheme.TEXT_MUTED);
        subtitle.setFont(UITheme.createFont(Font.PLAIN, 13f));

        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(subtitle);

        header.add(left, BorderLayout.WEST);

        borrowerProgressList.setOpaque(false);
        borrowerProgressList.setLayout(new BoxLayout(borrowerProgressList, BoxLayout.Y_AXIS));

        JScrollPane scroll = new JScrollPane(borrowerProgressList);
        scroll.getViewport().setBackground(UITheme.PANEL_BG);
        scroll.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        scroll.setPreferredSize(new Dimension(0, 420));

        RoundedPanel shell = new RoundedPanel(18, UITheme.PANEL_BG);
        shell.setBorderColor(UITheme.BORDER);
        shell.setBorderWidth(1);
        shell.setLayout(new BorderLayout());
        shell.add(scroll, BorderLayout.CENTER);

        card.add(header, BorderLayout.NORTH);
        card.add(shell, BorderLayout.CENTER);
        return card;
    }

    private void configurePill(JLabel label, Color borderColor, Color textColor, int arc, int padding) {
        label.setForeground(textColor);
        label.setOpaque(false);
        label.setBorder(BorderFactory.createCompoundBorder(
            new RoundedButtonBorder(arc, borderColor, false),
            BorderFactory.createEmptyBorder(5, padding, 5, padding)
        ));
    }

    public void refresh() {
        LocalDate today = LocalDate.now();
        double todayCollection = 0.0;
        int todayPaymentCount = 0;
        int activeCount = 0;
        int dueSoonCount = 0;
        int overdueCount = 0;
        double activePrincipal = 0.0;
        ArrayList<LoanProgress> loanProgressItems = new ArrayList<>();

        if (payments != null) {
            for (Payment payment : payments) {
                if (payment == null || payment.getPaymentDate() == null || !today.equals(payment.getPaymentDate())) {
                    continue;
                }
                todayCollection += payment.getAmount();
                todayPaymentCount++;
            }
        }

        if (loans != null) {
            for (Loan loan : loans) {
                if (loan == null) {
                    continue;
                }

                String status = normalizeStatus(loan.getStatus());
                double paidForLoan = totalPaidForLoan(safe(loan.getId()));
                double remainingBalance = calculator.computeRemainingBalance(loan, paidForLoan);
                LocalDate dueDate = resolveDueDate(loan);

                if (!"PAID".equals(status) && remainingBalance > 0.0 && !"OVERDUE".equals(status)) {
                    activeCount++;
                    activePrincipal += remainingBalance;
                }

                if (dueDate != null && !dueDate.isBefore(today) && !dueDate.isAfter(today.plusDays(7)) && !"PAID".equals(status)) {
                    dueSoonCount++;
                }

                if ("OVERDUE".equals(status) || (dueDate != null && dueDate.isBefore(today) && !"PAID".equals(status))) {
                    overdueCount++;
                }

                boolean includeInBorrowerProgress = !"PAID".equals(status) && remainingBalance > 0.0;
                if (includeInBorrowerProgress) {
                    loanProgressItems.add(new LoanProgress(
                        safe(loan.getId()),
                        resolveBorrowerName(safe(loan.getBorrowerId())),
                        calculator.computeTotalPayable(loan),
                        paidForLoan
                    ));
                }
            }
        }

        totalCollectionValue.setText(money(todayCollection));
        totalCollectionBody.setText(metricText("Based on payments recorded for " + HUMAN_DATE.format(today) + ".", 150));
        totalCollectionFooter.setText(metricText("Cash inflow today", 150));
        totalCollectionPill.setText(todayPaymentCount + (todayPaymentCount == 1 ? " payment" : " payments"));

        activeLoansValue.setText(Integer.toString(activeCount));
        activeLoansBody.setText(metricText("Total balance of " + money(activePrincipal) + ".", 150));
        activeLoansFooter.setText(metricText("Green - on-time", 150));
        activeLoansPill.setText(money(activePrincipal));

        dueSoonValue.setText(Integer.toString(dueSoonCount));
        dueSoonBody.setText(metricText("Orange loans are due in the next 7 days.", 150));
        dueSoonFooter.setText(metricText("Smart due date alert", 150));
        dueSoonPill.setText(money(sumPrincipalForDueWindow(today, today.plusDays(7))));

        overdueValue.setText(Integer.toString(overdueCount));
        overdueBody.setText(metricText("Loans past due by 1+ day.", 150));
        overdueFooter.setText(metricText("Requires follow up", 150));
        overduePill.setText(money(sumOverduePrincipal(today)));

        subTitleLabel.setText("Monitor collections, due windows, and borrower repayment progress.");
        populateBorrowerProgress(loanProgressItems);
    }

    private void populateBorrowerProgress(ArrayList<LoanProgress> loanProgressItems) {
        borrowerProgressList.removeAll();

        if (loanProgressItems.isEmpty()) {
            JLabel empty = new JLabel("No active loans to show yet.", SwingConstants.CENTER);
            empty.setForeground(UITheme.TEXT_MUTED);
            empty.setBorder(BorderFactory.createEmptyBorder(32, 12, 32, 12));
            borrowerProgressList.add(empty);
            borrowerProgressList.revalidate();
            borrowerProgressList.repaint();
            return;
        }

        boolean first = true;
        for (LoanProgress progress : loanProgressItems) {
            if (!first) {
                borrowerProgressList.add(Box.createVerticalStrut(10));
            }
            borrowerProgressList.add(createBorrowerProgressItem(progress));
            first = false;
        }
        if (first) {
            JLabel empty = new JLabel("No active borrower loans right now.", SwingConstants.CENTER);
            empty.setForeground(UITheme.TEXT_MUTED);
            empty.setBorder(BorderFactory.createEmptyBorder(28, 12, 28, 12));
            borrowerProgressList.add(empty);
        }
        borrowerProgressList.revalidate();
        borrowerProgressList.repaint();
    }

    private JPanel createBorrowerProgressItem(LoanProgress progress) {
        RoundedPanel item = new RoundedPanel(18, UITheme.CARD_2);
        item.setBorderColor(UITheme.BORDER);
        item.setBorderWidth(1);
        item.setLayout(new BoxLayout(item, BoxLayout.Y_AXIS));
        item.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel nameLabel = new JLabel(progress.borrowerName, SwingConstants.LEFT);
        nameLabel.setForeground(UITheme.TEXT);
        nameLabel.setFont(UITheme.createFont(Font.BOLD, 18f));

        JLabel loanLabel = new JLabel("Loan " + progress.loanId, SwingConstants.LEFT);
        loanLabel.setForeground(UITheme.TEXT_MUTED);
        loanLabel.setFont(UITheme.createFont(Font.PLAIN, 12.5f));

        double percent = progress.totalPayable <= 0.0
            ? 0.0
            : Math.min(100.0, (progress.totalPaid / progress.totalPayable) * 100.0);

        JPanel textBlock = new JPanel();
        textBlock.setOpaque(false);
        textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));

        JLabel summary = new JLabel(
            money(progress.totalPaid) + " paid out of " + money(progress.totalPayable),
            SwingConstants.LEFT
        );
        summary.setForeground(UITheme.TEXT);
        summary.setFont(UITheme.createFont(Font.BOLD, 18f));

        double remaining = Math.max(0.0, progress.totalPayable - progress.totalPaid);
        JLabel remainingLabel = new JLabel(
            money(remaining) + " remaining before this loan is fully paid.",
            SwingConstants.LEFT
        );
        remainingLabel.setForeground(UITheme.TEXT_MUTED);
        remainingLabel.setFont(UITheme.createFont(Font.PLAIN, 12.5f));

        textBlock.add(nameLabel);
        textBlock.add(Box.createVerticalStrut(3));
        textBlock.add(loanLabel);
        textBlock.add(Box.createVerticalStrut(6));
        textBlock.add(summary);
        textBlock.add(Box.createVerticalStrut(6));
        textBlock.add(remainingLabel);

        RepaymentProgressBar progressBar = new RepaymentProgressBar();
        progressBar.setPreferredSize(new Dimension(0, 16));
        progressBar.setProgress(percent / 100.0);

        JLabel percentLabel = new JLabel(String.format("%.0f%%", percent), SwingConstants.RIGHT);
        percentLabel.setForeground(UITheme.isLightMode() ? new Color(0x1D, 0x4E, 0xD8) : Color.WHITE);
        percentLabel.setFont(UITheme.createFont(Font.BOLD, 18f));
        percentLabel.setPreferredSize(new Dimension(54, 16));

        JPanel progressRow = new JPanel(new BorderLayout(14, 0));
        progressRow.setOpaque(false);
        progressRow.setAlignmentX(LEFT_ALIGNMENT);
        progressRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        progressRow.setPreferredSize(new Dimension(0, 20));
        progressRow.add(progressBar, BorderLayout.CENTER);
        progressRow.add(percentLabel, BorderLayout.EAST);

        textBlock.setAlignmentX(LEFT_ALIGNMENT);

        item.add(textBlock);
        item.add(Box.createVerticalStrut(10));
        item.add(progressRow);
        return item;
    }

    private String metricText(String value, int width) {
        return "<html><div style='width:" + width + "px;'>" + escapeHtml(safe(value)) + "</div></html>";
    }

    private String escapeHtml(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }

    private String resolveBorrowerName(String borrowerId) {
        if (borrowers == null || borrowerId == null) {
            return "";
        }
        for (Borrower borrower : borrowers) {
            if (borrower != null && borrowerId.equalsIgnoreCase(safe(borrower.getId()))) {
                return safe(borrower.getFullName());
            }
        }
        return borrowerId;
    }

    private LocalDate resolveDueDate(Loan loan) {
        if (loan == null || loan.getStartDate() == null || loan.getOriginalTermMonths() <= 0) {
            return null;
        }
        double paidForLoan = totalPaidForLoan(safe(loan.getId()));
        int remainingTermMonths = calculator.computeRemainingTermMonths(loan, paidForLoan);
        if ("PAID".equals(normalizeStatus(loan.getStatus())) || remainingTermMonths <= 0) {
            return null;
        }
        int completedInstallments = Math.max(0, loan.getOriginalTermMonths() - remainingTermMonths);
        int nextInstallmentNumber = completedInstallments + 1;
        if (nextInstallmentNumber > loan.getOriginalTermMonths()) {
            nextInstallmentNumber = loan.getOriginalTermMonths();
        }
        return loan.getStartDate().plusMonths(nextInstallmentNumber);
    }

    private double sumPrincipalForDueWindow(LocalDate from, LocalDate to) {
        double total = 0.0;
        if (loans == null) {
            return total;
        }
        for (Loan loan : loans) {
            if (loan == null) {
                continue;
            }
            LocalDate dueDate = resolveDueDate(loan);
            String status = normalizeStatus(loan.getStatus());
            if (dueDate != null && !dueDate.isBefore(from) && !dueDate.isAfter(to) && !"PAID".equals(status)) {
                total += calculator.computeRemainingBalance(loan, totalPaidForLoan(safe(loan.getId())));
            }
        }
        return total;
    }

    private double sumOverduePrincipal(LocalDate today) {
        double total = 0.0;
        if (loans == null) {
            return total;
        }
        for (Loan loan : loans) {
            if (loan == null) {
                continue;
            }
            LocalDate dueDate = resolveDueDate(loan);
            String status = normalizeStatus(loan.getStatus());
            if (("OVERDUE".equals(status) || (dueDate != null && dueDate.isBefore(today))) && !"PAID".equals(status)) {
                total += calculator.computeRemainingBalance(loan, totalPaidForLoan(safe(loan.getId())));
            }
        }
        return total;
    }

    private double totalPaidForLoan(String loanId) {
        double total = 0.0;
        if (payments == null || loanId == null) {
            return total;
        }
        for (Payment payment : payments) {
            if (payment != null && loanId.equalsIgnoreCase(safe(payment.getLoanId()))) {
                total += payment.getAmount();
            }
        }
        return total;
    }

    private String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase();
    }

    private String money(double amount) {
        return UITheme.formatCurrency(amount);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private static final class RepaymentProgressBar extends JPanel {
        private double progress;

        private RepaymentProgressBar() {
            setOpaque(false);
        }

        private void setProgress(double progress) {
            this.progress = Math.max(0.0, Math.min(1.0, progress));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int arc = height;

            Color trackTop = UITheme.isLightMode() ? new Color(0xE9, 0xF1, 0xFB) : new Color(0x1B, 0x23, 0x33);
            Color trackBottom = UITheme.isLightMode() ? new Color(0xD9, 0xE6, 0xF5) : new Color(0x14, 0x1B, 0x28);
            g2.setPaint(new GradientPaint(0, 0, trackTop, 0, height, trackBottom));
            g2.fillRoundRect(0, 0, width, height, arc, arc);

            int fillWidth = (int) Math.round(width * progress);
            if (fillWidth > 0) {
                Color fillStart = UITheme.isLightMode() ? new Color(0x34, 0xD3, 0x99) : new Color(0x22, 0xC5, 0x5E);
                Color fillEnd = UITheme.isLightMode() ? new Color(0x0F, 0xA5, 0xE9) : new Color(0x25, 0x63, 0xEB);
                g2.setPaint(new GradientPaint(0, 0, fillStart, fillWidth, 0, fillEnd));
                g2.fillRoundRect(0, 0, fillWidth, height, arc, arc);

                int sheenWidth = Math.max(18, Math.min(56, fillWidth / 3));
                g2.setColor(new Color(255, 255, 255, UITheme.isLightMode() ? 70 : 42));
                g2.fillRoundRect(Math.max(0, fillWidth - sheenWidth), 2, sheenWidth, Math.max(4, height - 4), arc, arc);
            }

            g2.setColor(UITheme.BORDER);
            g2.drawRoundRect(0, 0, width - 1, height - 1, arc, arc);
            g2.dispose();
        }
    }

    private static final class LoanProgress {
        private final String loanId;
        private final String borrowerName;
        private double totalPayable;
        private double totalPaid;

        private LoanProgress(String loanId, String borrowerName, double totalPayable, double totalPaid) {
            this.loanId = loanId == null || loanId.trim().isEmpty() ? "Unknown" : loanId.trim();
            this.borrowerName = borrowerName == null || borrowerName.trim().isEmpty() ? "Unknown borrower" : borrowerName.trim();
            this.totalPayable = totalPayable;
            this.totalPaid = totalPaid;
        }
    }
}
