package model;

public class Bill {
    private int billId;
    private int bookingId;
    private double totalAmount;
    private String billDate;
    private double tax;
    private double discount;
    private double extraCharges;

    public Bill(int billId, int bookingId, double totalAmount, String billDate) {
        this(billId, bookingId, totalAmount, billDate, 0, 0, 0);
    }

    public Bill(int billId, int bookingId, double totalAmount, String billDate, double tax, double discount, double extraCharges) {
        this.billId = billId;
        this.bookingId = bookingId;
        this.totalAmount = totalAmount;
        this.billDate = billDate;
        this.tax = tax;
        this.discount = discount;
        this.extraCharges = extraCharges;
    }

    public int getBillId() { return billId; }
    public void setBillId(int billId) { this.billId = billId; }
    public int getBookingId() { return bookingId; }
    public void setBookingId(int bookingId) { this.bookingId = bookingId; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public String getBillDate() { return billDate; }
    public void setBillDate(String billDate) { this.billDate = billDate; }
    public double getTax() { return tax; }
    public void setTax(double tax) { this.tax = tax; }
    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }
    public double getExtraCharges() { return extraCharges; }
    public void setExtraCharges(double extraCharges) { this.extraCharges = extraCharges; }
}
