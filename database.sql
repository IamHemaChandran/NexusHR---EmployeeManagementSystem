-- EMS Pro v3 — Full Database Setup
-- Run this entire file in MySQL Workbench

DROP DATABASE IF EXISTS EmployeeManagement;
CREATE DATABASE EmployeeManagement;
USE EmployeeManagement;

-- ── Login table with roles ────────────────────────────────────────────────────
CREATE TABLE login (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(20)  DEFAULT 'admin',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO login (username, password, role) VALUES
    ('admin',  'admin123',   'admin'),
    ('admin1', 'admin1234',  'admin'),
    ('jeel',   '123456789',  'admin'),
    ('hr1',    'hr123456',   'hr'),
    ('emp1',   'emp123456',  'employee');

-- ── Employee table ────────────────────────────────────────────────────────────
CREATE TABLE employee (
    empId       VARCHAR(20)  PRIMARY KEY,
    name        VARCHAR(40)  NOT NULL,
    fname       VARCHAR(40),
    dob         VARCHAR(20),
    salary      VARCHAR(20),
    address     VARCHAR(100),
    phone       VARCHAR(15),
    email       VARCHAR(100) UNIQUE,
    education   VARCHAR(40),
    designation VARCHAR(40),
    aadhaar     VARCHAR(15),
    photo       VARCHAR(255) DEFAULT '',
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ── Attendance table ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS attendance (
    id       INT AUTO_INCREMENT PRIMARY KEY,
    empId    VARCHAR(20) NOT NULL,
    date     DATE NOT NULL,
    status   VARCHAR(20) DEFAULT 'Absent',
    checkIn  VARCHAR(10),
    checkOut VARCHAR(10),
    UNIQUE KEY uniq_emp_date (empId, date)
);

-- ── Leaves table ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS leaves (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    empId       VARCHAR(20) NOT NULL,
    empName     VARCHAR(40),
    leaveType   VARCHAR(30),
    fromDate    DATE,
    toDate      DATE,
    days        INT DEFAULT 1,
    reason      TEXT,
    status      VARCHAR(20) DEFAULT 'Pending',
    appliedOn   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ── Sample employees ──────────────────────────────────────────────────────────
INSERT INTO employee (empId,name,fname,dob,salary,address,phone,email,education,designation,aadhaar) VALUES
('EMP-1001','Rahul Kumar',  'Suresh Kumar', '1995-06-15','75000','123 MG Road, Bengaluru',   '+91 98765 43210','rahul@company.com', 'B.Tech','Software Engineer',  '123456789012'),
('EMP-1002','Priya Shah',   'Ramesh Shah',  '1993-03-22','82000','456 Park Street, Mumbai',  '+91 91234 56789','priya@company.com', 'MBA',   'HR Manager',         '234567890123'),
('EMP-1003','Arjun Mehta',  'Vijay Mehta',  '1991-11-08','95000','789 Anna Salai, Chennai',  '+91 99887 76655','arjun@company.com', 'B.E',   'DevOps Lead',        '345678901234'),
('EMP-1004','Sneha Patel',  'Kiran Patel',  '1997-07-30','68000','321 CG Road, Ahmedabad',   '+91 88776 54321','sneha@company.com', 'B.Des', 'UI Designer',        '456789012345'),
('EMP-1005','Vikram Rao',   'Prasad Rao',   '1990-02-14','71000','654 Banjara Hills, Hyderabad','+91 77665 43210','vikram@company.com','M.Sc', 'Data Analyst',      '567890123456');

SELECT 'Setup complete!' AS status;
SELECT * FROM login;
SELECT * FROM employee;
