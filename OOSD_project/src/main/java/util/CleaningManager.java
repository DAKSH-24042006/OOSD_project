package util;

import model.Room;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CleaningManager {
    private static final ExecutorService executor = Executors.newFixedThreadPool(3, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    public static void assignCleaning(Room room, Runnable onComplete) {
        executor.submit(new CleaningTask(room, onComplete));
    }
}
