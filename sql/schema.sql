-- ============================================================
-- Library Management System
-- Database Schema
-- ============================================================

CREATE DATABASE IF NOT EXISTS library_db;

USE library_db;


-- ============================================================
-- BOOKS TABLE
-- ============================================================

CREATE TABLE IF NOT EXISTS books (
    book_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    author VARCHAR(100) NOT NULL,
    category VARCHAR(100),
    quantity INT NOT NULL,
    available INT NOT NULL
);


-- ============================================================
-- MEMBERS TABLE
-- ============================================================

CREATE TABLE IF NOT EXISTS members (
    member_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL
);


-- ============================================================
-- ISSUED BOOKS TABLE
-- ============================================================

CREATE TABLE IF NOT EXISTS issued_books (
    issue_id INT AUTO_INCREMENT PRIMARY KEY,
    book_id INT NOT NULL,
    member_id INT NOT NULL,
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    return_date DATE NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ISSUED',

    CONSTRAINT fk_issued_book
        FOREIGN KEY (book_id)
        REFERENCES books(book_id),

    CONSTRAINT fk_issued_member
        FOREIGN KEY (member_id)
        REFERENCES members(member_id)
);


-- ============================================================
-- SAMPLE BOOK DATA
-- ============================================================

INSERT INTO books
    (title, author, category, quantity, available)
VALUES
    ('Java Programming', 'James Gosling', 'Programming', 5, 5);


-- ============================================================
-- END OF SCHEMA
-- ============================================================