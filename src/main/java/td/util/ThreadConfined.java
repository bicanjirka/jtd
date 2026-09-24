package td.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Which thread owns a class's non-final, non-volatile state; Checkstyle requires it on such
 * classes. State that both threads touch is not confined and needs a {@code volatile} or an
 * immutable snapshot instead.
 * <p>
 * Construction is not covered: an object may be built anywhere and safely published to its owner.
 * The claim is about who reads and mutates it afterwards.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface ThreadConfined {

    Owner value();

    enum Owner {
        /** The {@code game-loop} thread: simulation state. */
        GAME_LOOP,
        /** Swing's Event Dispatch Thread: components, input and the level lifecycle. */
        EVENT_DISPATCH_THREAD,
        /** Whatever thread owns the containing object, for a helper with no lifetime of its own. */
        ENCLOSING
    }
}
