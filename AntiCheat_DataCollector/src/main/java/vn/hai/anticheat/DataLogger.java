package vn.hai.anticheat;

import com.google.gson.Gson;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DataLogger {
    // Sử dụng ConcurrentLinkedQueue để Main Thread và Async Thread không bị đụng độ bộ nhớ
    private final ConcurrentLinkedQueue<Object> logQueue = new ConcurrentLinkedQueue<>();
    private final JavaPlugin plugin;
    private final Gson gson = new Gson();
    private final File logFile;
    private final File logFileAll;

    public DataLogger(JavaPlugin plugin) {
        this.plugin = plugin;
        
        // Tạo file lưu trữ data.json trong thư mục plugins/DataCollector
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        this.logFile = new File(plugin.getDataFolder(), "raw_server_events.json");
        this.logFileAll = new File(plugin.getDataFolder(), "raw_server_events_all.json");
    }

    // Hàm này được gọi trong PlayerMovementListener (Chạy trên Main Thread)
    public void pushData(Object dataRecord) {
        logQueue.add(dataRecord); // Đẩy vào Queue cực kỳ nhanh và không gây lag
    }

    // Hàm này gọi ở onEnable() của DataCollectorPlugin
    public void startAsyncLogging() {
        // Tạo một BukkitRunnable chạy bất đồng bộ
        new BukkitRunnable() {
            @Override
            public void run() {
                if (logQueue.isEmpty()) return; // Nếu không có ai di chuyển thì bỏ qua

                // Rút toàn bộ dữ liệu từ Queue ra một List tạm
                List<Object> batchToSave = new ArrayList<>();
                while (!logQueue.isEmpty()) {
                    batchToSave.add(logQueue.poll());
                }

                // Thực hiện ghi I/O vào file (Vì đang ở luồng Async nên không sợ lag server)
                try (FileWriter fw = new FileWriter(logFile, true);
                     PrintWriter bw = new PrintWriter(fw)) {
                    
                    for (Object record : batchToSave) {
                        // Chuyển đổi dữ liệu sang chuỗi JSON và ghi ra file
                        bw.println(gson.toJson(record)); 
                    }
                    
                } catch (IOException e) {
                    plugin.getLogger().severe("Lỗi khi ghi file log: " + e.getMessage());
                }
            }
        }.runTaskTimerAsynchronously(plugin, 100L, 100L); // Chạy lặp lại mỗi 100 ticks (5 giây)
    }
    public void flushRemainingData() {
        if (logQueue.isEmpty()) {
            plugin.getLogger().info("Khong co du lieu ton dong trong Queue.");
            return;
        }

        plugin.getLogger().info("Dang xa (flush) " + logQueue.size() + " records xuong o cung...");
        
        // Chạy đồng bộ (Synchronous) để ép ổ cứng ghi xong mới cho phép server tắt
        try (FileWriter fw = new FileWriter(logFile, true);
             PrintWriter bw = new PrintWriter(fw)) {
            
            while (!logQueue.isEmpty()) {
                Object record = logQueue.poll();
                bw.println(gson.toJson(record)); 
            }
            
        } catch (IOException e) {
            plugin.getLogger().severe("Loi khi flush file log: " + e.getMessage());
        }
    }
}