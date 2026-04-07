package model;

public class Room {
    private int roomId;
    private String type;
    private double price;
    private String status;
    private int floor;
    private String bedType;
    private String ac;
    private String wifi;

    public Room(int roomId, String type, double price, String status, int floor, String bedType, String ac, String wifi) {
        this.roomId = roomId;
        this.type = type;
        this.price = price;
        this.status = status;
        this.floor = floor;
        this.bedType = bedType;
        this.ac = ac;
        this.wifi = wifi;
    }

    public int getRoomId() { return roomId; }
    public void setRoomId(int roomId) { this.roomId = roomId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getFloor() { return floor; }
    public void setFloor(int floor) { this.floor = floor; }
    public String getBedType() { return bedType; }
    public void setBedType(String bedType) { this.bedType = bedType; }
    public String getAc() { return ac; }
    public void setAc(String ac) { this.ac = ac; }
    public String getWifi() { return wifi; }
    public void setWifi(String wifi) { this.wifi = wifi; }

    @Override
    public String toString() { return roomId + " - " + type; }
}
