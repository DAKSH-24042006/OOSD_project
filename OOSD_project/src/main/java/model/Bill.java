package model;

public class Bill {
    private int billId;
    private int bookingId;
    private double totalAmount;

    public Bill(int billId, int bookingId, double totalAmount) {
        this.billId = billId;
        this.bookingId = bookingId;
        this.totalAmount = totalAmount;
    }

    public int getBillId() { return billId; }
    public void setBillId(int billId) { this.billId = billId; }
    public int getBookingId() { return bookingId; }
    public void setBookingId(int bookingId) { this.bookingId = bookingId; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
}
