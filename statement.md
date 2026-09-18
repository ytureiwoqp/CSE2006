# Project Statement – Library Management System

**Course:** CSE2006 – Programming in Java  
**Project type:** Console (command-line) application in Java with JDBC persistence

## Problem statement

Small libraries and college reading rooms need a dependable way to know, at any moment, which items they hold,
which of them are on the shelf, and who has borrowed the rest. When this is done on paper or in a spreadsheet,
the same problems keep appearing:

- an item is lent to two people at the same time;
- a member borrows more items than they are allowed;
- late fines are calculated inconsistently from one staff member to another;
- there is no quick way to see what is currently on loan or what a member has borrowed in the past.

The problem is to build a program that **records the catalogue and the members, enforces the lending rules
automatically, calculates late fines correctly, keeps a permanent record of every loan, and reports on it** –
using the language features taught in the course (object-oriented programming, exception handling,
multithreading, collections, file I/O and JDBC).

## Scope

### In scope

- A catalogue of **books and magazines** (add, list, search, delete)
- **Members** of two types, students and faculty, with different borrowing limits, loan periods and fine rules
- **Issuing and returning** items with automatic rule checks and late-fine calculation
- A permanent **loan history** stored in a relational database (H2, accessed through JDBC)
- **Reports:** items on loan, a member's loan history, inventory summary, recent activity
- **File handling:** bulk import from CSV, export of the catalogue, backup copy
- **Safe concurrent borrowing:** two simultaneous requests can never give the same copy to two members
  (demonstrated with two threads)
- Input validation, clear error messages, and automated tests

### Out of scope

- Graphical or web interface
- User login and roles
- E-mail or SMS reminders
- Automatic overdue detection from calendar dates (the librarian enters the number of days late when an item is returned)
- Reservations, online payment of fines
- JPA / ORM (persistence uses plain JDBC)
- Multi-computer (client–server) use of one database

## Target users

- **Librarians and library assistants** of a small college, school or departmental library – the people who
  operate the program.
- **Students and evaluators** of the course – the program is also a worked example of the course topics and can be
  run without typing by feeding it the included sample script.

## High-level features

| Area | Features |
|---|---|
| Catalogue | Add books and magazines; list sorted by title with availability; keyword search (optionally books or magazines only); delete unused items |
| Members | Register students and faculty with a unique e-mail; list members |
| Circulation | Issue an item (checks: item exists, member exists, item free, member under limit); return an item and calculate the fine; student and faculty rules; recent-activity list |
| Reports | Items currently on loan; member loan history with fines; inventory summary table |
| Files | Import items from CSV (invalid rows are skipped and reported); export the catalogue to CSV and create a backup copy |
| Concurrency | Synchronized issue/return; a two-thread demonstration in which exactly one borrower wins |
| Quality | Custom exception with readable messages; `PreparedStatement` for every user-driven query; 24 automated tests; scripted sample session |

## Key rules

| | Student | Faculty |
|---|---|---|
| Items allowed at once | 3 | 6 |
| Loan period | 14 days | 30 days |
| Grace days before fines start | 0 | 7 |
| Fine factor | full | half |

Base fine per late day: book 2.00, magazine 1.00.
**Fine = max(0, days late − grace days) × base fine × fine factor.**

## Where to find more

- How to install, run and test: [`README.md`](README.md)
- Requirements, architecture, diagrams, design decisions, implementation and testing:
  [`docs/Library_Management_System_Report.pdf`](docs/Library_Management_System_Report.pdf)
