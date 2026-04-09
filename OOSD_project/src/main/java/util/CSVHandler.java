package util;

import model.Bill;
import model.Booking;
import model.Customer;
import model.Room;

import java.io.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class CSVHandler {
    private static final String ROOMS_FILE = "rooms.csv";
    private static final String CUSTOMERS_FILE = "customers.csv";
    private static final String BOOKINGS_FILE = "bookings.csv";
    private static final String BILLS_FILE = "billing.csv";

    private static final ReentrantLock roomsLock = new ReentrantLock();
    private static final ReentrantLock customersLock = new ReentrantLock();
    private static final ReentrantLock bookingsLock = new ReentrantLock();
    private static final ReentrantLock billsLock = new ReentrantLock();

    public static ReentrantLock getRoomsLock() { return roomsLock; }
    public static ReentrantLock getCustomersLock() { return customersLock; }
    public static ReentrantLock getBookingsLock() { return bookingsLock; }
    public static ReentrantLock getBillsLock() { return billsLock; }

    public static void ensureFilesExist() {
        createFileIfNotExists(ROOMS_FILE, "roomId,type,price,status,floor,bedType,ac,wifi");
        createFileIfNotExists(CUSTOMERS_FILE, "customerId,name,phone,email,address");
        createFileIfNotExists(BOOKINGS_FILE, "bookingId,customerId,roomId,checkIn,checkOut,paymentStatus,paymentMethod,status");
        createFileIfNotExists(BILLS_FILE, "billId,bookingId,totalAmount,billDate,tax,discount,extraCharges");
    }

    private static void createFileIfNotExists(String fileName, String header) {
        File file = new File(fileName);
        if (!file.exists()) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
                pw.println(header);
            } catch (IOException ignored) {}
        }
    }

    public static List<Room> loadRooms() {
        roomsLock.lock();
        try {
            List<Room> rooms = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(ROOMS_FILE))) {
                String line = br.readLine();
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    try {
                        String[] data = line.split(",");
                        if (data.length >= 8) {
                            rooms.add(new Room(Integer.parseInt(data[0].trim()), data[1].trim(), Double.parseDouble(data[2].trim()), data[3].trim(),
                                    Integer.parseInt(data[4].trim()), data[5].trim(), data[6].trim(), data[7].trim()));
                        }
                    } catch (Exception ignored) {}
                }
            } catch (IOException ignored) {}
            return rooms;
        } finally {
            roomsLock.unlock();
        }
    }

    public static void saveRooms(List<Room> rooms) {
        roomsLock.lock();
        try {
            try (PrintWriter pw = new PrintWriter(new FileWriter(ROOMS_FILE))) {
                pw.println("roomId,type,price,status,floor,bedType,ac,wifi");
                for (Room room : rooms) {
                    pw.printf("%d,%s,%s,%s,%d,%s,%s,%s%n", room.getRoomId(), room.getType(), room.getPrice(),
                            room.getStatus(), room.getFloor(), room.getBedType(), room.getAc(), room.getWifi());
                }
            } catch (IOException ignored) {}
        } finally {
            roomsLock.unlock();
        }
    }

    public static List<Customer> loadCustomers() {
        customersLock.lock();
        try {
            List<Customer> customers = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(CUSTOMERS_FILE))) {
                String line = br.readLine();
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    try {
                        String[] data = line.split(",");
                        if (data.length >= 5) {
                            customers.add(new Customer(Integer.parseInt(data[0].trim()), data[1].trim(), data[2].trim(), data[3].trim(), data[4].trim()));
                        }
                    } catch (Exception ignored) {}
                }
            } catch (IOException ignored) {}
            return customers;
        } finally {
            customersLock.unlock();
        }
    }

    public static void saveCustomers(List<Customer> customers) {
        customersLock.lock();
        try {
            try (PrintWriter pw = new PrintWriter(new FileWriter(CUSTOMERS_FILE))) {
                pw.println("customerId,name,phone,email,address");
                for (Customer customer : customers) {
                    pw.printf("%d,%s,%s,%s,%s%n", customer.getCustomerId(), customer.getName(), customer.getPhone(),
                            customer.getEmail(), customer.getAddress());
                }
            } catch (IOException ignored) {}
        } finally {
            customersLock.unlock();
        }
    }

    public static List<Booking> loadBookings() {
        bookingsLock.lock();
        try {
            List<Booking> bookings = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(BOOKINGS_FILE))) {
                String line = br.readLine();
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    try {
                        String[] data = line.split(",");
                        if (data.length >= 7) {
                            Booking b = new Booking(Integer.parseInt(data[0].trim()), Integer.parseInt(data[1].trim()), Integer.parseInt(data[2].trim()),
                                    LocalDate.parse(data[3].trim()), LocalDate.parse(data[4].trim()), data[5].trim(), data[6].trim());
                            if (data.length >= 8) b.setStatus(data[7].trim());
                            bookings.add(b);
                        }
                    } catch (Exception ignored) {}
                }
            } catch (IOException ignored) {}
            return bookings;
        } finally {
            bookingsLock.unlock();
        }
    }

    public static void saveBookings(List<Booking> bookings) {
        bookingsLock.lock();
        try {
            try (PrintWriter pw = new PrintWriter(new FileWriter(BOOKINGS_FILE))) {
                pw.println("bookingId,customerId,roomId,checkIn,checkOut,paymentStatus,paymentMethod,status");
                for (Booking booking : bookings) {
                    pw.printf("%d,%d,%d,%s,%s,%s,%s,%s%n", booking.getBookingId(), booking.getCustomerId(), booking.getRoomId(),
                            booking.getCheckIn().toString(), booking.getCheckOut().toString(), booking.getPaymentStatus(),
                            booking.getPaymentMethod(), booking.getStatus());
                }
            } catch (IOException ignored) {}
        } finally {
            bookingsLock.unlock();
        }
    }

    public static List<Bill> loadBills() {
        billsLock.lock();
        try {
            List<Bill> bills = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(BILLS_FILE))) {
                String line = br.readLine(); // skip header
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    String[] data = line.split(",");
                    try {
                        if (data.length >= 7) {
                            bills.add(new Bill(Integer.parseInt(data[0]), Integer.parseInt(data[1]), Double.parseDouble(data[2]),
                                    data[3], Double.parseDouble(data[4]), Double.parseDouble(data[5]), Double.parseDouble(data[6])));
                        } else if (data.length >= 4) {
                            bills.add(new Bill(Integer.parseInt(data[0]), Integer.parseInt(data[1]), Double.parseDouble(data[2]), data[3]));
                        }
                    } catch (Exception ignored) {}
                }
            } catch (IOException ignored) {}
            return bills;
        } finally {
            billsLock.unlock();
        }
    }

    public static void saveBills(List<Bill> bills) {
        billsLock.lock();
        try {
            try (PrintWriter pw = new PrintWriter(new FileWriter(BILLS_FILE))) {
                pw.println("billId,bookingId,totalAmount,billDate,tax,discount,extraCharges");
                for (Bill bill : bills) {
                    pw.printf("%d,%d,%s,%s,%s,%s,%s%n", bill.getBillId(), bill.getBookingId(), bill.getTotalAmount(),
                            bill.getBillDate(), bill.getTax(), bill.getDiscount(), bill.getExtraCharges());
                }
            } catch (IOException ignored) {}
        } finally {
            billsLock.unlock();
        }
    }

    public static int nextCustomerId() {
        customersLock.lock();
        try {
            return loadCustomers().stream().mapToInt(Customer::getCustomerId).max().orElse(0) + 1;
        } finally {
            customersLock.unlock();
        }
    }

    public static int nextBookingId() {
        bookingsLock.lock();
        try {
            return loadBookings().stream().mapToInt(Booking::getBookingId).max().orElse(0) + 1;
        } finally {
            bookingsLock.unlock();
        }
    }

    public static int nextBillId() {
        billsLock.lock();
        try {
            return loadBills().stream().mapToInt(Bill::getBillId).max().orElse(0) + 1;
        } finally {
            billsLock.unlock();
        }
    }

    public static Room getRoomById(int id) {
        return loadRooms().stream().filter(r -> r.getRoomId() == id).findFirst().orElse(null);
    }

    public static Customer getCustomerById(int id) {
        return loadCustomers().stream().filter(c -> c.getCustomerId() == id).findFirst().orElse(null);
    }

    public static Booking getBookingById(int id) {
        return loadBookings().stream().filter(b -> b.getBookingId() == id).findFirst().orElse(null);
    }

    public static void syncAllToDB() throws SQLException {
        // Rooms
        List<Room> rooms = loadRooms();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("REPLACE INTO rooms (roomId, type, price, status, floor, bedType, ac, wifi) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            for (Room r : rooms) {
                pstmt.setInt(1, r.getRoomId());
                pstmt.setString(2, r.getType());
                pstmt.setDouble(3, r.getPrice());
                pstmt.setString(4, r.getStatus());
                pstmt.setInt(5, r.getFloor());
                pstmt.setString(6, r.getBedType());
                pstmt.setString(7, r.getAc());
                pstmt.setString(8, r.getWifi());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }

        // Customers
        List<Customer> customers = loadCustomers();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("REPLACE INTO customers (customerId, name, phone, email, address) VALUES (?, ?, ?, ?, ?)")) {
            for (Customer c : customers) {
                pstmt.setInt(1, c.getCustomerId());
                pstmt.setString(2, c.getName());
                pstmt.setString(3, c.getPhone());
                pstmt.setString(4, c.getEmail());
                pstmt.setString(5, c.getAddress());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }

        // Bookings
        List<Booking> bookings = loadBookings();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("REPLACE INTO bookings (bookingId, customerId, roomId, checkIn, checkOut, paymentStatus, paymentMethod, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            for (Booking b : bookings) {
                pstmt.setInt(1, b.getBookingId());
                pstmt.setInt(2, b.getCustomerId());
                pstmt.setInt(3, b.getRoomId());
                pstmt.setString(4, b.getCheckIn().toString());
                pstmt.setString(5, b.getCheckOut().toString());
                pstmt.setString(6, b.getPaymentStatus());
                pstmt.setString(7, b.getPaymentMethod());
                pstmt.setString(8, b.getStatus());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }

        // Bills
        List<Bill> bills = loadBills();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("REPLACE INTO billing (billId, bookingId, totalAmount, billDate, tax, discount, extraCharges) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            for (Bill b : bills) {
                pstmt.setInt(1, b.getBillId());
                pstmt.setInt(2, b.getBookingId());
                pstmt.setDouble(3, b.getTotalAmount());
                pstmt.setString(4, b.getBillDate());
                pstmt.setDouble(5, b.getTax());
                pstmt.setDouble(6, b.getDiscount());
                pstmt.setDouble(7, b.getExtraCharges());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }
}
