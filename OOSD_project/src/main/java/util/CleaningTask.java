package util;

import javafx.application.Platform;
import model.Room;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class CleaningTask implements Runnable {
    private Room room;
    private Runnable onComplete;

    public CleaningTask(Room room, Runnable onComplete) {
        this.room = room;
        this.onComplete = onComplete;
    }

    @Override
    public void run() {
        synchronized (room) {
            updateStatus("IN_PROGRESS");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            updateStatus("CLEAN");
            if (onComplete != null) {
                Platform.runLater(onComplete);
            }
        }
    }

    private void updateStatus(String status) {
        room.setCleaningStatus(status);
        updateDatabase(room);
        updateCSV(room);
    }

    private void updateDatabase(Room r) {
        String sql = "UPDATE rooms SET cleaning_status=? WHERE roomId=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            synchronized (DBConnection.getDbMonitor()) {
                pstmt.setString(1, r.getCleaningStatus());
                pstmt.setInt(2, r.getRoomId());
                pstmt.executeUpdate();
            }
        } catch (SQLException ignored) {}
    }

    private void updateCSV(Room r) {
        CSVHandler.getRoomsLock().lock();
        try {
            List<Room> rooms = CSVHandler.loadRooms();
            List<Room> updatedRooms = rooms.stream()
                    .map(rm -> rm.getRoomId() == r.getRoomId() ? r : rm)
                    .collect(Collectors.toList());
            CSVHandler.saveRooms(updatedRooms);
        } finally {
            CSVHandler.getRoomsLock().unlock();
        }
    }
}
