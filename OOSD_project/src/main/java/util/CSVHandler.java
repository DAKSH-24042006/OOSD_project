package util;

import model.Bill;
import model.Booking;
import model.Customer;
import model.Room;

import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CSVHandler {
    private static final String ROOMS_FILE = "rooms.csv";
    private static final String CUSTOMERS_FILE = "customers.csv";
    private static final String BOOKINGS_FILE = "bookings.csv";
    private static final String BILLS_FILE = "billing.csv";

    public static void ensureFilesExist() {
        createFileIfNotExists(ROOMS_FILE, "roomId,type,price,status,floor,bedType,ac,wifi");
        createFileIfNotExists(CUSTOMERS_FILE, "customerId,name,phone,email,address");
        createFileIfNotExists(BOOKINGS_FILE, "bookingId,customerId,roomId,checkIn,checkOut,paymentStatus,paymentMethod");
        createFileIfNotExists(BILLS_FILE, "billId,bookingId,totalAmount");
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
        List<Room> rooms = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(ROOMS_FILE))) {
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] data = line.split(",");
                rooms.add(new Room(Integer.parseInt(data[0]), data[1], Double.parseDouble(data[2]), data[3],
                        Integer.parseInt(data[4]), data[5], data[6], data[7]));
            }
        } catch (IOException ignored) {}
        return rooms;
    }

    public static void saveRooms(List<Room> rooms) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(ROOMS_FILE))) {
            pw.println("roomId,type,price,status,floor,bedType,ac,wifi");
            for (Room room : rooms) {
                pw.printf("%d,%s,%s,%s,%d,%s,%s,%s%n", room.getRoomId(), room.getType(), room.getPrice(),
                        room.getStatus(), room.getFloor(), room.getBedType(), room.getAc(), room.getWifi());
            }
        } catch (IOException ignored) {}
    }

    public static List<Customer> loadCustomers() {
        List<Customer> customers = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(CUSTOMERS_FILE))) {
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] data = line.split(",");
                if (data.length >= 5) {
                    customers.add(new Customer(Integer.parseInt(data[0]), data[1], data[2], data[3], data[4]));
                }
            }
        } catch (IOException ignored) {}
        return customers;
    }

    public static void saveCustomers(List<Customer> customers) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(CUSTOMERS_FILE))) {
            pw.println("customerId,name,phone,email,address");
            for (Customer customer : customers) {
                pw.printf("%d,%s,%s,%s,%s%n", customer.getCustomerId(), customer.getName(), customer.getPhone(),
                        customer.getEmail(), customer.getAddress());
            }
        } catch (IOException ignored) {}
    }

    public static List<Booking> loadBookings() {
        List<Booking> bookings = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(BOOKINGS_FILE))) {
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] data = line.split(",");
                bookings.add(new Booking(Integer.parseInt(data[0]), Integer.parseInt(data[1]), Integer.parseInt(data[2]),
                        LocalDate.parse(data[3]), LocalDate.parse(data[4]), data[5], data[6]));
            }
        } catch (IOException ignored) {}
        return bookings;
    }

    public static void saveBookings(List<Booking> bookings) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(BOOKINGS_FILE))) {
            pw.println("bookingId,customerId,roomId,checkIn,checkOut,paymentStatus,paymentMethod");
            for (Booking booking : bookings) {
                pw.printf("%d,%d,%d,%s,%s,%s,%s%n", booking.getBookingId(), booking.getCustomerId(), booking.getRoomId(),
                        booking.getCheckIn().toString(), booking.getCheckOut().toString(), booking.getPaymentStatus(),
                        booking.getPaymentMethod());
            }
        } catch (IOException ignored) {}
    }

    public static List<Bill> loadBills() {
        List<Bill> bills = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(BILLS_FILE))) {
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] data = line.split(",");
                bills.add(new Bill(Integer.parseInt(data[0]), Integer.parseInt(data[1]), Double.parseDouble(data[2])));
            }
        } catch (IOException ignored) {}
        return bills;
    }

    public static void saveBills(List<Bill> bills) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(BILLS_FILE))) {
            pw.println("billId,bookingId,totalAmount");
            for (Bill bill : bills) {
                pw.printf("%d,%d,%s%n", bill.getBillId(), bill.getBookingId(), bill.getTotalAmount());
            }
        } catch (IOException ignored) {}
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
}
