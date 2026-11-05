package game.heroes;

import java.util.Objects;

/** Владелец правил способности отделён от личности её настоящего исполнителя. */
public record AbilityUse(HeroType owner, Channel channel) {
    public enum Channel { NATIVE, BORROWED }

    public AbilityUse {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(channel, "channel");
    }

    public static AbilityUse of(HeroType owner, boolean borrowed) {
        return new AbilityUse(owner, borrowed ? Channel.BORROWED : Channel.NATIVE);
    }

    public boolean borrowed() { return channel == Channel.BORROWED; }
}
