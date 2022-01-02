package kr.syeyoung.reshiftsimulator.session;

import kr.syeyoung.reshiftsimulator.packets.DeadBody;
import kr.syeyoung.reshiftsimulator.packets.PacketHelper;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.stream.Collectors;

public class ReshiftPlayer {
    private PlayerStatus status;
    private Player player;

    private ReshiftSimSession session;

    private int tickTilDead;

    private ReviveSession currentReviveSession;

    private boolean bugged;

    private DeadBody deadBody;

    public ReviveSession getReviveSession() {
        return currentReviveSession;
    }

    public ReshiftPlayer(Player player, ReshiftSimSession session) {
        this.status = PlayerStatus.LIVING;
        this.player = player;
        this.session = session;
    }

    public PlayerStatus getStatus() {
        return status;
    }

    public void setStatus(PlayerStatus status) {
        if (this.status == PlayerStatus.LIVING && status == PlayerStatus.INJURED) {
            tickTilDead = 20 * 10;
            bugged = true;
            if (currentReviveSession != null) {
                ReviveSession rs = currentReviveSession;
                currentReviveSession = null;
                rs.cancel();
            }

            session.getPlayers().stream().map(ReshiftPlayer::getPlayer).forEach(p -> {
                p.sendMessage(player.getName()+" got knocked down. You have 10 seconds to revive him");
            });

            try {
                deadBody = new DeadBody(getPlayer());
            } catch (Exception e) {}
            getPlayer().setGameMode(GameMode.SPECTATOR);
        }

        if (status == PlayerStatus.LIVING && this.status != PlayerStatus.LIVING) {
            getPlayer().setMaxHealth(32);
            getPlayer().setHealth(32);
            getPlayer().removePotionEffect(PotionEffectType.INVISIBILITY);
            getPlayer().setGameMode(GameMode.ADVENTURE);
            getPlayer().getInventory().setContents(new ItemStack[36]);
            getPlayer().getInventory().addItem(new ItemStack(Material.IRON_SWORD));
            if (deadBody != null) {
                deadBody.remove();
                getPlayer().teleport(deadBody.getDeadBodyLoc());
                deadBody = null;
            } else {
                getPlayer().teleport(new Location(getPlayer().getWorld(), 0, 4, 0));
            }
        }
        this.status = status;
    }

    public void removeDeadBody() {
        if (deadBody != null) deadBody.remove();
    }

    public Location getLocation() {
        if (deadBody != null) return deadBody.getDeadBodyLoc();
        else return player.getLocation();
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public ReshiftSimSession getSession() {
        return session;
    }

    public ReviveSession getCurrentReviveSession() {
        return currentReviveSession;
    }

    public void setCurrentReviveSession(ReviveSession currentReviveSession) {
        this.currentReviveSession = currentReviveSession;
    }

    public int getTickTilDead() {
        return tickTilDead;
    }

    public void IPressShift() {
        if (status == PlayerStatus.LIVING)
            bugged = false;
    }

    public void IUnShift() {
        if (currentReviveSession != null && status == PlayerStatus.LIVING) {
            ReviveSession rs = currentReviveSession;
            currentReviveSession = null;
            rs.cancel();
        }
    }

    public void tick() {
        if (status == PlayerStatus.INJURED) {
            tickTilDead--;
            PacketHelper.sendActionbar(getPlayer(), "You'll die in "+(tickTilDead / 20.0)+" Seconds!");
            if (tickTilDead == 0) {
                status = PlayerStatus.DEAD;
                getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, true));
                getPlayer().setAllowFlight(true);
            }
        } else if (status == PlayerStatus.LIVING) {
            if (session.getDpt() < 1) {
                PacketHelper.sendActionbar(getPlayer(), "Dmg Per Tick "+(session.getDpt())+" / Dmg Per Second" + (session.getDpt() * 20));
            } else {
                PacketHelper.sendActionbar(getPlayer(), "Dmg Per Tick "+(session.getDpt())+" / Dmg Per Second" + (session.getDpt() * 20) +" / Dpt increase in "+(session.getCurrTick() % 40 / 20.0)+" Seconds");
            }

            if ((currentReviveSession == null  || currentReviveSession.isDone())&& this.player.isSneaking() && !this.bugged) {
                List<ReshiftPlayer> deadPlayers = session.getPlayers().stream().filter(rp -> rp.getStatus() == PlayerStatus.INJURED && rp.getLocation().distanceSquared(getPlayer().getLocation()) < 2).collect(Collectors.toList());
                if (deadPlayers.size() == 0) return;
                ReshiftPlayer Irev = deadPlayers.get(session.random.nextInt(deadPlayers.size()));
                new ReviveSession(this, Irev);
            }
            removeDeadBody();
        } else if (status == PlayerStatus.DEAD) {
            PacketHelper.sendActionbar(getPlayer(), "You died :/");
        }
    }
}
