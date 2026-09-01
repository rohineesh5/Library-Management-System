# 📚 Library Management System

A robust, console-based **Java Library Management System** built with **Core Java, JDBC (Java Database Connectivity), and MySQL**. The project follows clean architecture principles, utilizing the **DAO (Data Access Object) Design Pattern**, **Encapsulation**, **JDBC Transactions**, and **Validation Utilities**.

**Author:** Junaid Khan

---

## 📋 Project Description

The **Library Management System** automates core library administration workflows. It allows librarians to manage books and members, issue and return books, track copy availability in real-time, enforce database transactions, calculate late return fines, and handle user input validation cleanly.

---

## ✨ Features

### 📖 Book Management
- **Add New Books**: Register books with title, author, category, total quantity, and available copies.
- **View All Books**: Display books in a neatly formatted, truncated console table.
- **Search Books**: Look up books by **Book ID** or partial **Title** match (`LIKE`).
- **Update Books**: Edit existing book details with default fallback prompts.
- **Delete Books**: Safely delete book records with user confirmation safeguards.

### 👤 Member Management
- **Register Members**: Add members with full name, email, and phone number.
- **Duplicate Email Prevention**: Pre-checks DB to prevent registering existing emails.
- **View Members**: Formatted table overview of registered library members.
- **Search & Update Members**: Search by name and update contact details.

### 🔄 Book Issue & Return (ACID Transactions)
- **Issue Book**:
  - Validates copy availability (`available > 0`).
  - Decrements available book count in `books`.
  - Creates an `issued_books` record with issue date, due date (+14 days), and `ISSUED` status.
  - Executed inside a **JDBC Transaction** (`setAutoCommit(false)`, `commit()`, `rollback()`).
- **Return Book**:
  - Prevents returning an already returned book.
  - Updates `issued_books` with return date and changes status to `RETURNED`.
  - Increments available copy count in `books`.
  - Transactionally protected against partial database updates.

### 💰 Fine Calculation Utility
- **Standard Loan Period**: 14 days.
- **Fine Rate**: ₹10 per overdue day.
- Uses Java 8+ `LocalDate` and `ChronoUnit.DAYS` for accurate date arithmetic.
- Calculates late days and total fine for returned and active overdue books.

### 🛡️ Input Validation & Error Handling
- Checks for empty strings, positive quantities, non-negative availability, and valid email/phone formats (`Regex`).
- Translates raw MySQL error codes (e.g., duplicate entry `1062`, FK violation `1452`) into clear, actionable error messages.

---

## 📁 Folder Structure

```text
Library-Management-System/
├── src/
│   ├── database/
│   │   └── DBConnection.java       # Manages MySQL Connection via JDBC
│   ├── model/
│   │   ├── Book.java               # Book entity model (Encapsulated)
│   │   ├── Member.java             # Member entity model (Encapsulated)
│   │   └── IssueRecord.java        # Issue/Return record model
│   ├── dao/
│   │   ├── BookDAO.java            # Interface for Book CRUD operations
│   │   ├── BookDAOImpl.java        # Implementation of Book DAO using JDBC
│   │   ├── MemberDAO.java          # Interface for Member operations
│   │   ├── MemberDAOImpl.java      # Implementation of Member DAO
│   │   ├── IssueDAO.java           # Interface for Issue/Return operations
│   │   └── IssueDAOImpl.java       # Implementation with JDBC Transactions
│   ├── util/
│   │   ├── FineCalculator.java     # Fine calculation logic & reports
│   │   └── Validator.java          # Input & SQL error validation utility
│   └── Main.java                   # Console menu interface & entry point
├── sql/
│   └── schema.sql                  # Database creation and sample data script
├── README.md                       # Project documentation
└── .gitignore                      # Git ignore rules for Java binaries
```

---

## 🛠️ Technologies Used

| Technology | Purpose |
|---|---|
| **Java 17+** | Core programming language |
| **JDBC** | Database connectivity & SQL execution (`PreparedStatement`, `ResultSet`) |
| **MySQL Database** | Relational data storage |
| **MySQL Connector/J** | JDBC driver for MySQL |
| **Java Time API** | `LocalDate`, `ChronoUnit` for date arithmetic |

---

## 🗄️ Database Schema

### 1. `books` Table
```sql
CREATE TABLE books (
    book_id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    author VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    quantity INT NOT NULL,
    available INT NOT NULL
);
```

### 2. `members` Table
```sql
CREATE TABLE members (
    member_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(15)
);
```

### 3. `issued_books` Table
```sql
CREATE TABLE issued_books (
    issue_id INT PRIMARY KEY AUTO_INCREMENT,
    book_id INT NOT NULL,
    member_id INT NOT NULL,
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    return_date DATE,
    status VARCHAR(20) DEFAULT 'Issued',
    FOREIGN KEY (book_id) REFERENCES books(book_id),
    FOREIGN KEY (member_id) REFERENCES members(member_id)
);
```

---

## 🚀 How to Run

### Prerequisites
1. **JDK 17 or higher** installed and added to `PATH`.
2. **MySQL Server** installed and running on `localhost:3306`.
3. **MySQL Connector/J JAR** (`mysql-connector-j-x.x.x.jar`).

### Database Setup
1. Open MySQL Command Line or MySQL Workbench.
2. Run the provided SQL script in `sql/schema.sql` or create manually:
   ```sql
   CREATE DATABASE library_db;
   USE library_db;

   -- Execute table creation SQL snippets provided in Database Schema section above
   ```
3. Update connection credentials in `src/database/DBConnection.java`:
   ```java
   private static final String URL = "jdbc:mysql://localhost:3306/library_db";
   private static final String USER = "Your_Username";
   private static final String PASSWORD = "Your_Password";
   ```

### Compilation & Execution

#### Windows (PowerShell / CMD)
```powershell
# Create bin directory for output classes
mkdir bin

# Compile all Java source files (include MySQL JAR in classpath if using external lib folder)
javac -d bin -cp "src;lib/*" src/database/*.java src/model/*.java src/util/*.java src/dao/*.java src/Main.java

# Run Main class
java -cp "bin;lib/*" Main
```

---

## 🔮 Future Improvements

- **GUI Interface**: Upgrade from Console UI to Swing / JavaFX or a Web Application (Spring Boot + React).
- **Role-Based Access Control**: Separate admin (librarian) and user (student) login portals.
- **Automated Fine Payment**: Integrate online payment gateway for overdue fines.
- **Notification System**: Automated Email/SMS reminders for upcoming due dates.

---

## 🎓 Learning Outcomes

Through building this project, key concepts mastered include:
1. **DAO Design Pattern**: Decoupling database persistence logic from application UI logic.
2. **JDBC Best Practices**: Using `PreparedStatement` to prevent SQL Injection, and `try-with-resources` for auto-closing database connections.
3. **ACID Transaction Management**: Managing multi-statement operations with `setAutoCommit(false)`, `commit()`, and `rollback()`.
4. **Encapsulated Models**: Protecting entity states using Java beans conventions (`private` fields, getters/setters).
5. **Robust Validation**: Building reusable validation utilities for input formats, business rules, and friendly SQL error handling.

---

## 👤 Author

**Junaid Khan**

## Copyright

© 2026 Junaid Khan. All Rights Reserved.

This project is the original work of Junaid Khan.

Unauthorized copying, modification, redistribution, publication,
or presentation of this project as someone else's work is not permitted.

If you reference this project, please provide proper attribution to
Junaid Khan and the original repository.
