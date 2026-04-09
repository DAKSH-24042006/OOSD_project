CREATE DATABASE IF NOT EXISTS hotel_db;
USE hotel_db;

CREATE TABLE IF NOT EXISTS rooms (
    roomId INT PRIMARY KEY,
    type VARCHAR(50),
    price DOUBLE,
    status VARCHAR(50),
    floor INT,
    bedType VARCHAR(50),
    ac VARCHAR(50),
    wifi VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS customers (
    customerId INT PRIMARY KEY,
    name VARCHAR(100),
    phone VARCHAR(50),
    email VARCHAR(100),
    address VARCHAR(200)
);

CREATE TABLE IF NOT EXISTS bookings (
    bookingId INT PRIMARY KEY,
    customerId INT,
    roomId INT,
    checkIn DATE,
    checkOut DATE,
    paymentStatus VARCHAR(50),
    paymentMethod VARCHAR(50),
    FOREIGN KEY (customerId) REFERENCES customers(customerId) ON DELETE CASCADE,
    FOREIGN KEY (roomId) REFERENCES rooms(roomId) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS billing (
    billId INT PRIMARY KEY,
    bookingId INT,
    totalAmount DOUBLE,
    FOREIGN KEY (bookingId) REFERENCES bookings(bookingId) ON DELETE CASCADE
);
