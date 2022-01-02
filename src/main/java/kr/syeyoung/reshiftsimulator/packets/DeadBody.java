package kr.syeyoung.reshiftsimulator.packets;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.v1_8_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.List;

public class DeadBody {

    public DeadBody(Player p) throws Exception {
        playFakeBed(p);
    }

    public static DataWatcher clonePlayerDatawatcher(Player player, int currentEntId) {

        EntityHuman h = new EntityHuman(((CraftWorld) player.getWorld()).getHandle(), ((CraftPlayer) player).getProfile()) {
            @Override
            public void sendMessage(IChatBaseComponent arg0) {
                return;
            }

            @Override
            public boolean a(int arg0, String arg1) {
                return false;
            }

            @Override
            public BlockPosition getChunkCoordinates() {
                return null;
            }

            @Override
            public boolean v() {
                return false;
            }
        };
        h.d(currentEntId);
        return h.getDataWatcher();
    }

    void setValue(Object instance, String fieldName, Object value) throws Exception {
        Field field = instance.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(instance, value);
    }

    void playFakeBed(Player p) throws Exception {
        BlockPosition pos =
                new BlockPosition(p.getLocation().getBlockX(), 0, p.getLocation().getBlockZ());
        playFakeBed(p, pos);
    }

    private static int GlobalEntityId = 999999;
    private int entityId = GlobalEntityId ++;

    @SuppressWarnings("deprecation")
    void playFakeBed(Player p, BlockPosition pos) throws Exception {
        PacketPlayOutNamedEntitySpawn packetEntitySpawn = new PacketPlayOutNamedEntitySpawn();

        CraftPlayer p1 = (CraftPlayer) p;


        deadBodyLoc = p.getLocation().clone().getWorld().getHighestBlockAt(p.getLocation()).getLocation();
        if (deadBodyLoc.getX() > 10) deadBodyLoc.setX(10);
        else if (deadBodyLoc.getX() < -10) deadBodyLoc.setX(-10);
        if (deadBodyLoc.getZ() > 10) deadBodyLoc.setZ(10);
        else if (deadBodyLoc.getZ() < -10) deadBodyLoc.setZ(-10);


        DataWatcher dw = clonePlayerDatawatcher(p, entityId);
        dw.watch(10, p1.getHandle().getDataWatcher().getByte(10));

        GameProfile prof = new GameProfile(p1.getUniqueId(), p1.getName());

        PacketPlayOutPlayerInfo packetInfo =
                new PacketPlayOutPlayerInfo(EnumPlayerInfoAction.ADD_PLAYER);
        PlayerInfoData  data = new PlayerInfoData(packetInfo, prof, 0,
                EnumGamemode.SURVIVAL, new ChatMessage("", new Object[0]));
        List<PlayerInfoData> dataList = Lists.newArrayList();
        dataList.add(data);
        setValue(packetInfo, "b", dataList);

        setValue(packetEntitySpawn, "a", entityId);
        setValue(packetEntitySpawn, "b", prof.getId());
        setValue(packetEntitySpawn, "c", MathHelper.floor(((EntityHuman) p1.getHandle()).locX * 32D));
        setValue(packetEntitySpawn, "d", MathHelper.floor(deadBodyLoc.getY() * 32D));
        setValue(packetEntitySpawn, "e", MathHelper.floor(((EntityHuman) p1.getHandle()).locZ * 32D));
        setValue(packetEntitySpawn, "f",
                (byte) ((int) (((EntityHuman) p1.getHandle()).yaw * 256.0F / 360.0F)));
        setValue(packetEntitySpawn, "g",
                (byte) ((int) (((EntityHuman) p1.getHandle()).pitch * 256.0F / 360.0F)));
        setValue(packetEntitySpawn, "i", dw);

        PacketPlayOutBed packetBed = new PacketPlayOutBed();

        setValue(packetBed, "a", entityId);
        setValue(packetBed, "b", pos);

        PacketPlayOutEntityTeleport packetTeleport = new PacketPlayOutEntityTeleport();
        setValue(packetTeleport, "a", entityId);
        setValue(packetTeleport, "b", MathHelper.floor(((EntityHuman) p1.getHandle()).locX * 32.0D));
        setValue(packetTeleport, "c", MathHelper.floor(deadBodyLoc.getY() * 32.0D));
        setValue(packetTeleport, "d", MathHelper.floor(((EntityHuman) p1.getHandle()).locZ * 32.0D));
        setValue(packetTeleport, "e",
                (byte) ((int) (((EntityHuman) p1.getHandle()).yaw * 256.0F / 360.0F)));
        setValue(packetTeleport, "f",
                (byte) ((int) (((EntityHuman) p1.getHandle()).pitch * 256.0F / 360.0F)));
        setValue(packetTeleport, "g", true);

        PacketPlayOutEntityTeleport packetTeleportDown = new PacketPlayOutEntityTeleport();
        setValue(packetTeleportDown, "a", entityId);
        setValue(packetTeleportDown, "b",
                MathHelper.floor(((EntityHuman) p1.getHandle()).locX * 32.0D));
        setValue(packetTeleportDown, "c", 0);
        setValue(packetTeleportDown, "d",
                MathHelper.floor(((EntityHuman) p1.getHandle()).locZ * 32.0D));
        setValue(packetTeleportDown, "e",
                (byte) 0);
        setValue(packetTeleportDown, "f",
                (byte) 0);
        setValue(packetTeleportDown, "g", true);


        loc = p.getLocation().clone();
        loc = loc.subtract(0, loc.getY(), 0);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendBlockChange(loc, Material.BED_BLOCK, (byte) 0);

            CraftPlayer pl = ((CraftPlayer) player);

                pl.getHandle().playerConnection.sendPacket(packetInfo);
                pl.getHandle().playerConnection.sendPacket(packetEntitySpawn);
                pl.getHandle().playerConnection.sendPacket(packetTeleportDown);
                pl.getHandle().playerConnection.sendPacket(packetBed);
                pl.getHandle().playerConnection.sendPacket(packetTeleport);
        }

        dataList.clear();
    }

    private Location loc;
    private Location deadBodyLoc;

    public Location getDeadBodyLoc() {
        return deadBodyLoc;
    }

    public void remove() {
        PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(entityId);
        org.bukkit.block.Block b = loc.getBlock();
        for (Player p : loc.getWorld().getPlayers()) {
            ((CraftPlayer) p).getHandle().playerConnection
                    .sendPacket(packet);
            p.sendBlockChange(b.getLocation(), b.getType(), b.getData());
        }
    }
}
