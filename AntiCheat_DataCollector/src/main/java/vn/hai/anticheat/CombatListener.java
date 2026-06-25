package vn.hai.anticheat;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.HashMap;
import java.util.Map;

public class CombatListener implements Listener {
    private final DataLogger logger;

    public CombatListener(DataLogger logger) {
        this.logger = logger;
    }

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        // Chỉ ghi nhận nếu người ra đòn là Người chơi (Player)
        if (!(event.getDamager() instanceof Player)) return;

        Player attacker = (Player) event.getDamager();
        double distance = attacker.getLocation().distance(event.getEntity().getLocation());

        Map<String, Object> data = new HashMap<>();
        data.put("event_type", "combat");
        data.put("timestamp", System.currentTimeMillis());
        data.put("uuid", attacker.getUniqueId().toString());
        
        data.put("target_type", event.getEntity().getType().name());
        data.put("distance", distance);
        data.put("damage_dealt", event.getDamage());

        logger.pushData(data);
    }
}