# LendWise

This project now supports SQLite through JDBC.

## What you need

1. Create a `lib` folder in the project root if it does not exist.
2. Put the SQLite JDBC jar inside `lib`.
   Example filename: `sqlite-jdbc-3.46.1.3.jar`

## Build the app

Run:

```bat
build-lendwise.bat
```

## Run the app

Run:

```bat
run-lendwise.bat
```

## Where the database is

When the JDBC jar is available, the app creates the SQLite database automatically here:

```text
data\lendwise.db
```

You do not need to create the database manually.

## View the database manually

You can open `data\lendwise.db` using any SQLite viewer, such as DB Browser for SQLite.

Tables created by the app:

- `borrowers`
- `loans`
- `payments`
- `accounts`

## Notes

- If the SQLite JDBC jar is missing, the app falls back to the older file-based storage.
- For automatic Gmail reminders inside the app, fill in `data\mail.properties` with your sender Gmail and Gmail App Password, then set `mail.enabled=true`.
