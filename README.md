# LendWise

## 2. Project Description

This application is a desktop-based lending management system designed to help lenders, loan administrators, and registered borrowers manage and monitor lending activities in a more organized and efficient way. It addresses common real-world problems such as manual loan tracking, missed due dates, scattered borrower information, delayed follow-up on overdue accounts, and limited visibility for borrowers regarding their own loan status.

The system uses Java Swing to provide a professional desktop interface and uses SQLite through JDBC for persistent data storage, ensuring that borrower, loan, payment, and account records remain available between sessions. It includes dashboard summaries, role-based account access, borrower-side views, password reset support, and automated overdue reminder features to improve daily monitoring, collection efficiency, and user accessibility.

## 3. Scope and Delimitations

The scope of this study covers the design and development of a desktop-based lending management system that allows authorized users to create accounts, log in securely, manage borrower profiles, record loan details, monitor payment transactions, view dashboard summaries of loan and collection activity, and access borrower-specific loan information through a dedicated user panel. The system is intended to help small-scale lenders or lending staff maintain organized records, while also allowing borrowers to view their own loan, payment, and account activity through a separate portal.

The system is limited to desktop use and does not support web-based or mobile access. It is focused only on borrower, loan, payment, account, and reminder management, and does not include advanced banking features such as online fund transfers, biometric authentication, multi-branch synchronization, or integration with external financial institutions. Although email reminders and password reset emails are supported, they depend on proper Gmail configuration and internet access. The system is intended for use within a single lending organization and is not designed for enterprise-level deployment.

## 4. Features

- User Authentication, Account Creation, and Password Recovery: The system provides secure login and account creation using email and password credentials, with password masking for privacy, separate access for lender and borrower accounts, and a forgot-password feature that sends an OTP code through email for password reset.
- Role-Based Access: Users can sign in either as a lender or as a borrower, allowing the system to display the appropriate dashboard and interface based on the selected account type.
- Borrower Management: Enables lenders or administrators to add, edit, delete, and search borrower records, including borrower ID, full name, Gmail address, home address, and linked account information.
- Loan Management: Allows lenders or administrators to create and manage loan records by assigning a borrower, principal amount, interest rate, term, start date, collector name, and loan status.
- Payment Monitoring: Lets authorized users record, edit, and delete installment payments while automatically updating loan balances, payment progress, and remaining amounts.
- Dashboard Analytics: Provides a visual dashboard showing total daily collection, active loans, loans due within seven days, overdue loans, and today's recorded collections to support faster decision-making.
- Borrower Portal or User Panel: Borrowers can access a dedicated panel where they can view their personal account information, loan summaries, payment records, repayment progress, and remaining balances.
- Loan Overview for Borrowers: The system allows borrowers to view the status of their loans, including active, overdue, due-today, and paid loan conditions, as well as next due dates and remaining balances.
- Payment History for Borrowers: Borrowers can review their own recorded payment transactions through a separate payment history view.
- Account Timeline and Notifications: The borrower side includes a timeline of loan and payment activity, along with notifications related to due dates, overdue status, and completed payments.
- OTP-Based Password Reset: The system supports account recovery through a forgot-password process that sends a one-time password (OTP) to the registered email address, with verification before the password can be changed.
- Overdue Reminder Support: The system includes an email reminder function for overdue borrowers to support timely follow-up and improve collection efficiency.

## 5. Technical Notes

The application uses SQLite through JDBC for persistent storage. When the JDBC driver is available, the database is stored in `data\lendwise.db`.

To build and run the system:

```bat
build-lendwise.bat
run-lendwise.bat
```

For email reminders, update `data\mail.properties` with a valid Gmail sender account and app password, then enable mail sending in the configuration.
