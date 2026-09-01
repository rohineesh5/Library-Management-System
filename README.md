# 📚 Library Management System

A console-based **Library Management System** developed using **Core Java, OOP, JDBC, and MySQL**.

The system helps manage books and library members, issue and return books, track book availability, and calculate late-return fines.

## 🚀 Features

- 📖 Add, view, search, update, and delete books
- 👤 Add and view library members
- 📚 Issue books to members
- 🔄 Return issued books
- 📊 Track available book copies
- 💰 Calculate fines for late returns
- ✅ Input validation
- 🔐 Database connectivity using JDBC
- 🔄 Transaction handling for issue/return operations

## 🛠️ Technologies Used

| Technology | Purpose |
|---|---|
| Java | Application development |
| OOP | Encapsulation, abstraction, and interfaces |
| JDBC | Java-MySQL connectivity |
| MySQL | Database management |
| DAO Pattern | Data access layer |
| Git | Version control |
| GitHub | Source code hosting |

## 🏗️ Project Structure

```text
Library-Management-System
│
├── src
│   ├── dao
│   │   ├── BookDAO.java
│   │   ├── BookDAOImpl.java
│   │   ├── IssueDAO.java
│   │   ├── IssueDAOImpl.java
│   │   ├── MemberDAO.java
│   │   └── MemberDAOImpl.java
│   │
│   ├── database
│   │   └── DBConnection.java
│   │
│   ├── model
│   │   ├── Book.java
│   │   ├── IssueRecord.java
│   │   └── Member.java
│   │
│   ├── service
│   │   └── LibraryService.java
│   │
│   ├── util
│   │   ├── FineCalculator.java
│   │   ├── InputHelper.java
│   │   └── Validator.java
│   │
│   └── Main.java
│
├── sql
│   └── schema.sql
│
├── lib
│   └── mysql-connector-j-26.7.0.jar
│
├── .gitignore
├── LICENSE
└── README.md
```

## 🗄️ Database

The project uses **MySQL** with the following main tables:

- `books`
- `members`
- `issued_books`

The `books` table stores book information such as title, author, category, quantity, and available copies.

The `members` table stores library member details.

The `issued_books` table stores book issue details including issue date, due date, return date, and status.

## ▶️ How to Run

### 1. Clone the Repository

```bash
git clone https://github.com/rohineesh5/Library-Management-System.git
```

Move into the project directory:

```bash
cd Library-Management-System
```

### 2. Create the MySQL Database

Open MySQL:

```bash
mysql -u root -p
```

Create the database:

```sql
CREATE DATABASE library_db;
```

Use the database:

```sql
USE library_db;
```

Run the SQL script available in:

```text
sql/schema.sql
```

This creates the required tables for the application.

### 3. Configure Database Credentials

The application reads database credentials from environment variables.

Set:

```text
DB_URL=jdbc:mysql://localhost:3306/library_db
DB_USER=root
DB_PASSWORD=your_mysql_password
```

⚠️ **Do not commit your actual database password to GitHub.**

For Windows PowerShell, the variables can be set using:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/library_db"
$env:DB_USER="root"
$env:DB_PASSWORD = Read-Host "Enter MySQL password"
```

### 4. Compile the Project

Run the following command from the project root in PowerShell:

```powershell
javac -d bin -cp "src;lib/*" (Get-ChildItem -Recurse -Filter *.java src).FullName
```

### 5. Run the Application

```powershell
java -cp "bin;lib/*" Main
```

## 📋 Main Menu

When the application starts, the following options are available:

```text
1.  Add Book
2.  View Books
3.  Search Book
4.  Update Book
5.  Delete Book
6.  Add Member
7.  View Members
8.  Issue Book
9.  Return Book
10. Fine Details
11. Exit
```

## 📖 Book Management

The application supports complete book management operations:

- Add new books
- View all books
- Search for books
- Update book information
- Delete books
- Track total and available copies

## 👤 Member Management

Library members can be managed using:

- Add Member
- View Members

Member information is stored in the MySQL database.

## 🔄 Issue and Return Management

Books can be issued to registered members.

When a book is issued:

- The issue date is recorded
- A due date is generated
- Available book quantity is reduced
- Issue information is stored in the database

When a book is returned:

- The return date is recorded
- The issue status is updated
- Available book quantity is increased

## 💰 Fine Calculation

The system includes fine calculation functionality for overdue books.

It supports:

- Fine calculation for returned books
- Current fine calculation for books that have not yet been returned
- Due-date based calculation

If a book is returned on or before the due date, no fine is charged.

## 🧩 Concepts Implemented

This project demonstrates the practical implementation of:

- Object-Oriented Programming (OOP)
- Classes and Objects
- Constructors
- Encapsulation
- Getters and Setters
- Interfaces
- Interface Implementation
- DAO Design Pattern
- JDBC
- SQL CRUD Operations
- MySQL Database Integration
- Prepared Statements
- Exception Handling
- Input Validation
- Database Transactions
- Git Version Control
- GitHub Repository Management

## 🎯 Learning Outcomes

Through this project, I gained practical experience in connecting a **Java application with a MySQL database using JDBC**.

I also learned how to organize a Java application using separate layers such as **Model, DAO, Database, Service, and Utility classes**.

The project helped strengthen my understanding of:

- Writing maintainable Java code
- Working with relational databases
- Performing CRUD operations
- Using JDBC for database communication
- Applying OOP concepts in a real project
- Using the DAO pattern
- Managing source code using Git
- Publishing and maintaining a project on GitHub

## 👨‍💻 Author

**Rohineesh Bandari**

Java Developer | Software Engineering Enthusiast

---

⭐ If you find this project useful, feel free to explore the repository.