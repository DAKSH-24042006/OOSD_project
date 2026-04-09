CREATE DATABASE IF NOT EXISTS hotel_db;
USE hotel_db;

CREATE TABLE IF NOT EXISTS rooms (
    roomId INT PRIMARY KEY,
    type VARCHAR(50),
    price DECIMAL(10,2),
    status VARCHAR(50),
    floor INT,
    bedType VARCHAR(50),
    ac VARCHAR(10),
    wifi VARCHAR(10)
);

CREATE TABLE IF NOT EXISTS customers (
    customerId INT PRIMARY KEY,
    name VARCHAR(100),
    phone VARCHAR(20),
    email VARCHAR(100),
    address TEXT
);

CREATE TABLE IF NOT EXISTS bookings (
    bookingId INT PRIMARY KEY,
    customerId INT,
    roomId INT,
    checkIn DATE,
    checkOut DATE,
    paymentStatus VARCHAR(50),
    paymentMethod VARCHAR(50),
    status VARCHAR(50),
    FOREIGN KEY (customerId) REFERENCES customers(customerId),
    FOREIGN KEY (roomId) REFERENCES rooms(roomId)
);

CREATE TABLE IF NOT EXISTS billing (
    billId INT PRIMARY KEY,
    bookingId INT,
    totalAmount DECIMAL(10,2),
    billDate DATE,
    tax DECIMAL(10,2),
    discount DECIMAL(10,2),
    extraCharges DECIMAL(10,2),
    FOREIGN KEY (bookingId) REFERENCES bookings(bookingId)
);
