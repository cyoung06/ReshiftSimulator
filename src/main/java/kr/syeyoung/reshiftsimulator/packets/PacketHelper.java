package kr.syeyoung.reshiftsimulator.packets;

import net.minecraft.server.v1_8_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class PacketHelper {
    public static void sendActionbar(Player player, String str) {
        ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutChat(new ChatComponentText(str), (byte) 2));
    }


    public static void sendTitle(Player player, String str, int fadeIn, int sustain, int fadeOut) {
        ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutTitle(fadeIn, sustain, fadeOut));
        ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutTitle(EnumTitleAction.TITLE, new ChatComponentText(str)));
    }


    public static void sendSubTitle(Player player, String str, int fadeIn, int sustain, int fadeOut) {
        ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutTitle(fadeIn, sustain, fadeOut));
        ((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutTitle(EnumTitleAction.SUBTITLE, new ChatComponentText(str)));
    }

}
