package vn.hai.anticheat;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.HashMap;
import java.util.Map;

public class MiningListener implements Listener {
    private final DataLogger logger;

    public MiningListener(DataLogger logger) {
        this.logger = logger;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        Map<String, Object> data = new HashMap<>();
        data.put("event_type", "mine");
        data.put("timestamp", System.currentTimeMillis());
        data.put("uuid", player.getUniqueId().toString());
        
        data.put("block_type", block.getType().name());
        data.put("block_x", block.getX());
        data.put("block_y", block.getY());
        data.put("block_z", block.getZ());
        data.put("light_level", block.getLightLevel()); // Đặc trưng bắt X-Ray (Đào kim cương trong bóng tối)

        logger.pushData(data);
    }
}