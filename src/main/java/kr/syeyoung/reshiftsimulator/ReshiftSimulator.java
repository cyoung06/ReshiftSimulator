package kr.syeyoung.reshiftsimulator;

import kr.syeyoung.reshiftsimulator.packets.DeadBody;
import kr.syeyoung.reshiftsimulator.session.ReshiftPlayer;
import kr.syeyoung.reshiftsimulator.session.ReshiftSimSession;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ReshiftSimulator extends JavaPlugin {
    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    private List<ReshiftSimSession> rss = new ArrayList<>();
    private List<Player> players = new ArrayList<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getLabel().equals("rs")) {
            List<Player> players = Arrays.stream(args).map(Bukkit::getPlayer).filter(Objects::nonNull).collect(Collectors.toList());
            for (Player p:players) {
                if (this.players.contains(p)) {
                    sender.sendMessage("Nah that player's already in session");
                    return true;
                }
            }
            this.players.addAll(players);
            rss.add(new ReshiftSimSession(players));
        } else {
            try {
                new DeadBody((Player) sender);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    public void gameDone(ReshiftSimSession session) {
        rss.remove(session);
        session.getPlayers().stream().map(ReshiftPlayer::getPlayer).forEach(players::remove);
    }
}
