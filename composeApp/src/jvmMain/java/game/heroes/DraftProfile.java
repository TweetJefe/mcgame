package game.heroes;

/** Участие в новых каталогах; порядок не зависит от регистрации или enum ordinal. */
public record DraftProfile(boolean pickable, int order) {
    public static final DraftProfile EXCLUDED = new DraftProfile(false, 0);

    public DraftProfile {
        if (order < 0) throw new IllegalArgumentException("Negative draft order");
    }
    public static DraftProfile pickable(int order) { return new DraftProfile(true, order); }
}
