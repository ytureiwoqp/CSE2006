# Library Management System

A console (command-line) program written in **Java** for the course *CSE2006 – Programming in Java*.
It keeps track of books, magazines, members and loans. Data is stored in a small embedded database
(H2) using **JDBC**, so no database server, GUI or internet service is needed.

## What it does

- Add, list, search and delete books and magazines
- Register students and faculty (different borrowing limits, loan periods and fine rules)
- Issue and return items, with automatic late-fine calculation
- Reports: items on loan, a member's loan history, inventory summary, recent activity
- Import items from a CSV file, export the catalogue to a file and make a backup copy
- A demo where two threads try to borrow the same book at once (only one succeeds)

## Requirements

| Tool | Version | Check with |
|---|---|---|
| JDK (not only a JRE) | 17 or newer | `java -version` and `javac -version` |
| Apache Maven | 3.8 or newer | `mvn -version` |

Maven downloads the H2 library automatically the first time (internet needed once).
Prefer not to use Maven? See "Running without Maven" below.

## How to run

**1. Get the code and open the project folder** (the folder that contains `pom.xml`):

```bash
git clone https://github.com/<github-username>/library-management-system.git
cd library-management-system
```

**2. Start the program:**

```bash
mvn -q compile exec:java
```

A numbered menu appears. Type a number and press Enter. Your data is saved in `data/librarydb.mv.db`
and is still there the next time you start the program.

**3. Or run the ready-made sample session** (no typing needed; it uses a temporary database and
tries every menu option, including some deliberate mistakes to show the error handling):

```bash
mvn -q compile exec:java -Dexec.args="--memory" < data/sample_input.txt
```

(On Windows PowerShell use: `Get-Content data\sample_input.txt | mvn -q compile exec:java "-Dexec.args=--memory"`)

### A first manual session

1. Option **13**, then press Enter → imports the sample catalogue (3 invalid rows are skipped and reported)
2. Option **3** → register a member (for example `Asha`, `asha@example.com`, `student`)
3. Option **4** → list the items and note an id
4. Option **7** → issue that item id to member id `1`
5. Option **9** → see it listed as on loan
6. Option **8** → return it; enter `6` as days late to see a fine
7. Option **0** → exit

## Running without Maven

You need only a JDK 17+ and the H2 jar.

**Linux / macOS**
```bash
mkdir -p lib out
curl -L -o lib/h2-2.2.220.jar https://repo1.maven.org/maven2/com/h2database/h2/2.2.220/h2-2.2.220.jar
javac -cp lib/h2-2.2.220.jar -d out $(find src/main/java -name "*.java")
java -cp out:lib/h2-2.2.220.jar com.library.Main                                       # menu
java -cp out:lib/h2-2.2.220.jar com.library.Main --memory < data/sample_input.txt      # sample session
```

**Windows (Command Prompt)**
```bat
mkdir lib out
curl -L -o lib\h2-2.2.220.jar https://repo1.maven.org/maven2/com/h2database/h2/2.2.220/h2-2.2.220.jar
dir /s /b src\main\java\*.java > sources.txt
javac -cp lib\h2-2.2.220.jar -d out @sources.txt
java -cp "out;lib\h2-2.2.220.jar" com.library.Main --memory < data\sample_input.txt
```

## Menu

```
 1. Add book              9. Items currently on loan
 2. Add magazine         10. Member loan history
 3. Register member      11. Delete item
 4. List items           12. Inventory summary
 5. Search items         13. Import items from CSV
 6. List members         14. Export catalogue + backup copy
 7. Issue item           15. Recent activity
 8. Return item          16. Two-thread borrowing demo
 0. Exit
```

## Rules

| | Student | Faculty |
|---|---|---|
| Items allowed at once | 3 | 6 |
| Loan period | 14 days | 30 days |
| Late fine | full fine per day | first 7 days free, then half fine |

Fine per day: **book 2.00**, **magazine 1.00**. When returning, you enter how many days after the due
date the item came back. Example: student, book, 6 days late → 6 × 2.00 = **12.00**.
Faculty, book, 10 days late → (10 − 7) × 2.00 × 0.5 = **3.00**.

## Settings

The JDBC driver, URL and login are read from `config/db.properties` (not written in the code):

```properties
db.driver=org.h2.Driver
db.url=jdbc:h2:file:./data/librarydb
db.user=sa
db.password=
```

To start again with an empty database, delete the `data/librarydb*` files
(keep `sample_items.csv` and `sample_input.txt`).

CSV format (`data/sample_items.csv`): `type,title,creator,isbn-or-issue,category`, one item per line,
no commas inside a field. Lines starting with `#` are ignored.

## Project structure

```
pom.xml                            Maven file
config/db.properties               JDBC settings
data/sample_items.csv              sample catalogue (with 3 invalid rows)
data/sample_input.txt              scripted session for the sample run
src/main/java/com/library/
    Main.java                      console menu
    model/     Borrowable, LibraryItem, Book, Magazine, MemberType, Member
    exception/ LibraryException
    dao/       Database                (singleton, JDBC connection and tables)
    service/   LibraryService          (database operations and library rules)
    util/      FileUtil, BorrowThread  (file input/output, thread)
```

## Syllabus topics used

| Unit | Topics | Where |
|---|---|---|
| 1 | variables, data types, operators, console input/output, `if/else`, `switch`, `for`, for-each, `while`, `break`, `continue` | `Main` (menu loop, `switch`, `readInt`), `FileUtil` (`while`, `continue`) |
| 2 | classes and objects, constructors, `this`, `final`, access modifiers, inheritance, `super`, overriding, abstract class, interface, polymorphism (overloading and overriding), encapsulation, `instanceof`, enum with constructor, singleton, anonymous class | `LibraryItem` (abstract) → `Book`, `Magazine`; `Borrowable`; `MemberType`; `Database` (singleton); overloaded `searchItems`; `instanceof` in `getInventoryCounts`; anonymous `Comparator` in `Main.listItems` |
| 3 | exceptions, `try/catch`, `throw`/`throws`, multiple catch, custom exception, annotations, threads, thread life cycle, synchronization, packages | `LibraryException`; `catch (A \| B e)` in `Main`; `@Override`; `BorrowThread extends Thread` (state NEW → TERMINATED, `start`, `join`); `synchronized` in `LibraryService.issueItem`/`returnItem`; packages `model`, `exception`, `dao`, `service`, `util` |
| 4 | Strings, 1-D and 2-D arrays, `ArrayList`, `Stack`, byte and character streams, Reader/Writer | String methods in `LibraryService`/`FileUtil` (`split`, `trim`, `toLowerCase`); `String[]` in `FileUtil`; `int[][]` in `getInventoryCounts`; `ArrayList` everywhere; `Stack` for recent activity; `BufferedReader`/`FileReader`, `PrintWriter`/`FileWriter`, `FileInputStream`/`FileOutputStream` in `FileUtil` |
| 5 | JDBC: driver, connection, queries and results, driver information kept outside the code | `Database` (reads `config/db.properties`); `PreparedStatement`, `ResultSet`, `INSERT`/`SELECT`/`UPDATE`/`DELETE` and `JOIN` queries in `LibraryService` |

JPA is not used; storage is done directly with JDBC.

## Troubleshooting

| Problem | Fix |
|---|---|
| `mvn` not found | Install Maven and add it to your PATH, or use "Running without Maven" |
| `javac` not found / `release 17 not supported` | You have a JRE or an old JDK. Install a JDK 17 or newer |
| `config/db.properties not found` | You are not in the project folder. `cd` into it (the program still runs with defaults) |
| `Database may be already in use` | Another copy of the program is running. Close it, or delete `data/librarydb*` |
