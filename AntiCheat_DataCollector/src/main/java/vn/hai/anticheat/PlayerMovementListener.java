package vn.hai.anticheat;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;

public class PlayerMovementListener implements Listener {

    private final DataLogger logger;

    // SỬA LỖI 1: Truyền logger qua Constructor để class này có thể dùng nó mãi mãi
    public PlayerMovementListener( DataLogger logger) {
        this.logger = logger;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // SỬA LỖI 1: Hàm này giờ chỉ còn ĐÚNG 1 tham số là PlayerMoveEvent

        Player player = event.getPlayer();
        Location to = event.getTo();
        
        // Bỏ qua nếu dữ liệu vị trí bị rỗng
        if (to == null) return;

        // 1. Lấy thông tin ở thời điểm hiện tại (Chạy trên Main Thread)
        long timestamp = System.currentTimeMillis();
        String uuid = player.getUniqueId().toString();
        
        double x = to.getX();
        double y = to.getY();
        double z = to.getZ();
        float yaw = to.getYaw();     // Trục Xoay ngang
        float pitch = to.getPitch(); // Trục Xoay dọc đầu

        // 2. Đóng gói thành Dictionary (Map) để lát Gson tự chuyển thành JSON
        Map<String, Object> data = new HashMap<>();
        data.put("event_type", "move");
        data.put("timestamp", timestamp);
        data.put("uuid", uuid);
        data.put("x", x);
        data.put("y", y);
        data.put("z", z);
        data.put("yaw", yaw);
        data.put("pitch", pitch);

        // SỬA LỖI 2: KHÔNG tạo Task mới. Lập tức đẩy Data vào Queue của DataLogger.
        // Luồng Async tự động của DataLogger sẽ lo việc còn lại.
        // Hạn chế in ra Console (getLogger.info) ở đây vì nó sẽ làm trôi màn hình rất nhanh!
        logger.pushData(data);
    }
}