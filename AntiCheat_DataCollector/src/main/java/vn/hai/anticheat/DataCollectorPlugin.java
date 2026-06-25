package vn.hai.anticheat;

import org.bukkit.plugin.java.JavaPlugin;

public class DataCollectorPlugin extends JavaPlugin {

    // Khai báo biến ở cấp độ Class để onDisable() có thể truy cập được
    private DataLogger logger;
    
    @Override
    public void onEnable() {
        // Hàm này được trigger khi server khởi động và nạp plugin
        getLogger().info("========================================");
        getLogger().info("[DataCollector] Plugin dang khoi dong...");
        getLogger().info("[DataCollector] Chuan bi luong Asynchronous de ghi Log...");
        getLogger().info("========================================");
        
        // TODO: Sắp tới ta sẽ đăng ký EventListener và khởi tạo luồng I/O ở đây
        // 1. Khởi tạo Logger
        this.logger = new DataLogger(this);
        
        // 2. Truyền logger  để nó có công cụ ghi log
        getServer().getPluginManager().registerEvents(new PlayerMovementListener(this.logger), this);
        
        getServer().getPluginManager().registerEvents(new CombatListener(this.logger), this);
        getServer().getPluginManager().registerEvents(new MiningListener(this.logger), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(this.logger), this);
        // 3. Bắt đầu luồng Asynchronous để ghi log
        this.logger.startAsyncLogging();
    }

    @Override
    public void onDisable() {
        // Hàm này cực kỳ quan trọng đối với Data Engineering. 
        // Khi sập server hoặc tắt đột ngột, ta phải xả (flush) toàn bộ buffer log 
        // còn kẹt trong RAM xuống ổ cứng để không bị mất dữ liệu đào tạo (training data).
        getLogger().info("[DataCollector] Dong ket noi. Dang luu cac file JSON dang do...");
    }
}