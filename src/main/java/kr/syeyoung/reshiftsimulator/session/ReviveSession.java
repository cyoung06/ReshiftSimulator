package kr.syeyoung.reshiftsimulator.session;

import kr.syeyoung.reshiftsimulator.packets.PacketHelper;

public class ReviveSession {
    private final ReshiftPlayer living;
    private final ReshiftPlayer target;

    private double ticksRemaining;

    private boolean isDone = false;

    public ReviveSession(ReshiftPlayer living, ReshiftPlayer target) {
        this.living = living;
        this.target = target;
        if (living.getStatus() != PlayerStatus.LIVING) throw new IllegalArgumentException("living player is not living");
        if (target.getStatus() != PlayerStatus.INJURED) throw new IllegalArgumentException("target player is not injured");

        int rr = target.getSession().random.nextInt(5);
        if (rr != 0) {
            ticksRemaining = 0.3 * 20;
        } else {
            ticksRemaining = 1.5 * 20;
        }

        start();
    }

    public void tick() {
        ticksRemaining --;
        if (ticksRemaining <= 0) {
            target.setStatus(PlayerStatus.LIVING);
            target.getPlayer().setHealth(target.getPlayer().getMaxHealth() / 2);

            living.getSession().removeReviveSession(this);

            living.getSession().getPlayers().stream().map(ReshiftPlayer::getPlayer).forEach(p -> {
                p.sendMessage(living.getPlayer().getName()+" Revived" +target.getPlayer().getName());
            });
            isDone = true;
        }

        PacketHelper.sendActionbar(living.getPlayer(), "Reviving "+target.getPlayer().getName()+"... "+(ticksRemaining / 20.0));
        PacketHelper.sendActionbar(target.getPlayer(), "Revived by "+living.getPlayer().getName()+"... "+(ticksRemaining / 20.0));
    }

    public boolean isDone(){
        return isDone;
    }

    public void cancel() {
        if (!isDone) {
            target.setStatus(PlayerStatus.INJURED);

            living.getSession().removeReviveSession(this);
            isDone = true;
        }
    }

    public void start() {
        target.setStatus(PlayerStatus.REVIVING);
        target.setCurrentReviveSession(this);
        living.setCurrentReviveSession(this);

        living.getSession().addReviveSession(this);
        isDone = false;
    }
}
