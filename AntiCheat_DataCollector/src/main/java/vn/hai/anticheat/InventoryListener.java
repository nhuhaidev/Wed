package vn.hai.anticheat;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashMap;
import java.util.Map;

public class InventoryListener implements Listener {
    private final DataLogger logger;

    public InventoryListener(DataLogger logger) {
        this.logger = logger;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        Map<String, Object> data = new HashMap<>();
        data.put("event_type", "inventory");
        data.put("timestamp", System.currentTimeMillis());
        data.put("uuid", player.getUniqueId().toString());
        
        data.put("action_type", event.getAction().name());
        data.put("slot_clicked", event.getSlot());
        data.put("is_shift_click", event.isShiftClick());

        logger.pushData(data);
    }
}