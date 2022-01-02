package kr.syeyoung.reshiftsimulator.session;

import kr.syeyoung.reshiftsimulator.packets.PacketHelper;
import kr.syeyoung.reshiftsimulator.ReshiftSimulator;
import net.minecraft.server.v1_8_R1.*;
import org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.craftbukkit.v1_8_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_8_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_8_R1.util.UnsafeList;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class ReshiftSimSession implements Listener {
    private List<ReshiftPlayer> players;
    private List<ReviveSession> reviveSession;
    private World world;

    private double dpt;

    private Zombie dmgDummy;

    private int taskId;

    private int currTick = 0;

    public Random random = new Random();

    public double getDpt() {
        return dpt;
    }

    public int getCurrTick() {
        return currTick;
    }

    public List<ReshiftPlayer> getPlayers() {
        return players;
    }

    private void stupidZombie() throws NoSuchFieldException, IllegalAccessException {

        EntityInsentient handle = (EntityInsentient) ((CraftLivingEntity) this.dmgDummy).getHandle();
        stupiderZombie(handle.goalSelector);
        stupiderZombie(handle.targetSelector);
    }

    private void stupiderZombie(PathfinderGoalSelector selector) throws NoSuchFieldException, IllegalAccessException {
        Field b = PathfinderGoalSelector.class.getDeclaredField("b");
        Field c = PathfinderGoalSelector.class.getDeclaredField("c");
        b.setAccessible(true);
        c.setAccessible(true);
        ((UnsafeList)b.get(selector)).clear();
        ((UnsafeList)c.get(selector)).clear();
    }

    public ReshiftSimSession(List<Player> players) {
        for (Player p:players) {
            p.getInventory().setContents(new ItemStack[36]);
            p.getInventory().addItem(new ItemStack(Material.IRON_SWORD));
            p.spigot().setCollidesWithEntities(false);
        }

        this.players = players.stream().map(p -> new ReshiftPlayer(p, this)).collect(Collectors.toList());
        this.reviveSession = new CopyOnWriteArrayList<>();

        this.world = Bukkit.createWorld(WorldCreator.name(players.get(0).getUniqueId().toString()).type(WorldType.FLAT));
        this.world.setAutoSave(false);


        for (int x = -10; x <= 10; x++) {
            for (int y = 0; y < 32; y++) {
                this.world.getBlockAt(x, y, -10).setType(Material.QUARTZ_BLOCK);
                this.world.getBlockAt(x, y, -11).setType(Material.QUARTZ_BLOCK);
                this.world.getBlockAt(x, y, 10).setType(Material.QUARTZ_BLOCK);
                this.world.getBlockAt(x, y, 11).setType(Material.QUARTZ_BLOCK);
            }
        }
        for (int z = -10; z <= 10; z++) {
            for (int y = 0; y < 32; y++) {
                this.world.getBlockAt(-10, y, z).setType(Material.QUARTZ_BLOCK);
                this.world.getBlockAt(10, y, z).setType(Material.QUARTZ_BLOCK);
                this.world.getBlockAt(-11, y, z).setType(Material.QUARTZ_BLOCK);
                this.world.getBlockAt(11, y, z).setType(Material.QUARTZ_BLOCK);
            }
        }


        this.dmgDummy = (Zombie) this.world.spawnEntity(new Location(this.world, 0, 4, 0), EntityType.ZOMBIE);
        this.dmgDummy.setCustomName("Very Dangerous Zombie");
        this.dmgDummy.setCustomNameVisible(true);
        try {
            stupidZombie();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        ((CraftLivingEntity) this.dmgDummy).getHandle().setEquipment(4, CraftItemStack.asNMSCopy(new ItemStack(Material.WOOD)));



        this.dpt = 0;

        players.forEach(p -> {
            p.teleport(new Location(this.world, 0, 4, 0));

            p.setGameMode(GameMode.ADVENTURE);
            p.setMaxHealth(32);
            p.setHealth(32);
            p.setFoodLevel(20);

            p.sendMessage("Reshift Simulator / Survive as high dpt you can survive with your team!");
        });

        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(ReshiftSimulator.getPlugin(ReshiftSimulator.class), this::tick, 1L, 1L);


        Bukkit.getPluginManager().registerEvents(this, ReshiftSimulator.getPlugin(ReshiftSimulator.class));
    }

    public void applyDamage() {
        players.stream().filter(r -> r.getStatus() == PlayerStatus.LIVING).map(ReshiftPlayer::getPlayer).forEach(p -> {
            double dmg = dpt * 5;
            if (!p.isOnGround()) dmg /= 1.5;
            if (p.isBlocking()) dmg /= 1.5;


            if (p.getHealth() - dmg < 0.5) {
                ReshiftPlayer rp = this.players.stream().filter(rp2 -> rp2.getPlayer() == p).findFirst().get();
                p.setMaxHealth(20);
                p.setHealth(20);

                rp.setStatus(PlayerStatus.INJURED);
            } else {
                int randomn = random.nextInt(5);
                if (randomn == 0) {
                    Vector normalized = dmgDummy.getLocation().toVector().subtract(p.getLocation().toVector()).normalize();
                    normalized = normalized.setY(0.3);
                    normalized = normalized.multiply(0.5);

                    p.setVelocity(normalized);
                }

                p.damage(dmg);
            }

        });
    }

    public void tick() {
        this.players.forEach(ReshiftPlayer::tick);
        this.reviveSession.forEach(ReviveSession::tick);
        if (currTick % 5 == 0) {
            applyDamage();
        }

        if (++currTick % 40 == 0 && dpt < 1) {
            dpt += 0.02;
        }


        if (players.stream().noneMatch(a -> a.getStatus() == PlayerStatus.LIVING)) {
            HandlerList.unregisterAll(this);
            Bukkit.getScheduler().cancelTask(taskId);

            players.forEach(rs -> {
                PacketHelper.sendTitle(rs.getPlayer(), "You Died", 0, 100, 10);
                PacketHelper.sendSubTitle(rs.getPlayer(), "You Lasted "+dpt+" dmg per tick and "+(currTick / 20.0) +" seconds", 0, 200, 10);
            });

            ReshiftSimulator.getPlugin(ReshiftSimulator.class).gameDone(this);

            Bukkit.getScheduler().runTaskLater(ReshiftSimulator.getPlugin(ReshiftSimulator.class), () -> {
                players.forEach(rs -> {
                    rs.getPlayer().teleport(new Location(Bukkit.getWorld("world"), 0,4,0));
                    rs.getPlayer().removePotionEffect(PotionEffectType.INVISIBILITY);
                    rs.getPlayer().setGameMode(GameMode.ADVENTURE);
                    rs.getPlayer().setAllowFlight(false);

                    rs.removeDeadBody();
                });

                Bukkit.unloadWorld(this.world, false);
                try {
                    FileUtils.deleteDirectory(new File(this.world.getName()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }, 20 * 10);
        }
    }

    public void addReviveSession(ReviveSession reviveSession) {
        this.reviveSession.add(reviveSession);
    }
    public void removeReviveSession(ReviveSession reviveSession) {
        this.reviveSession.remove(reviveSession);
    }

    @EventHandler
    public void toggleShift(PlayerToggleSneakEvent toggleSneakEvent) {
        Player p = toggleSneakEvent.getPlayer();
        Optional<ReshiftPlayer> rp = this.players.stream().filter(rp2 -> rp2.getPlayer() == p).findFirst();
        if (rp.isPresent()) {
            if (toggleSneakEvent.isSneaking()) rp.get().IPressShift();
            else rp.get().IUnShift();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void playerDie(EntityDamageEvent playerDmg) {
        if (playerDmg.getEntity() instanceof Zombie && playerDmg.getEntity().getCustomName().equals("Very Dangerous Zombie")) {
            playerDmg.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void playerLEave(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        Optional<ReshiftPlayer> rp = this.players.stream().filter(rp2 -> rp2.getPlayer() == p).findFirst();
        if (rp.isPresent()) {
            rp.get().setStatus(PlayerStatus.INJURED);
            rp.get().setStatus(PlayerStatus.DEAD);
        }
    }

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent flce) {
        flce.setCancelled(true);
    }
}
