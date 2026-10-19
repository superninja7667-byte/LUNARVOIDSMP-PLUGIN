package gg.lunarvoidsmp.core;

/** Ownership policy for a personal effect without replacing another source's effect. */
public final class ManagedEffect {
    public interface Effect {
        boolean present();
        boolean matches();
        boolean owned();
        void owned(boolean value);
        boolean grant();
        void remove();
    }
    private ManagedEffect() {}
    public static void apply(boolean enabled,Effect effect) {
        if(effect.owned() && !effect.matches()) effect.owned(false);
        if(!enabled) { release(effect); return; }
        if(!effect.present()) effect.owned(effect.grant());
    }
    public static void release(Effect effect) {
        if(!effect.owned()) return;
        if(effect.matches()) {
            effect.remove();
            if(effect.matches()) return; // Another plugin may cancel removal; retain ownership for retry.
        }
        effect.owned(false);
    }
}
