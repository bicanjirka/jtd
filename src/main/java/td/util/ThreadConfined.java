package td.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares which thread owns a class's mutable state, for a class whose fields are therefore
 * neither {@code final} nor {@code volatile}. Checkstyle requires it on any class with such a
 * field. Where the honest answer is "both threads", the field is not confined and belongs behind
 * a {@code volatile} or an immutable snapshot instead.
 * <p>
 * Construction is not part of the claim: an object is built by whichever thread creates it and
 * handed to its owner by safe publication (a {@code CopyOnWriteArrayList}, a {@code volatile}
 * write). What the annotation claims is who may mutate and read it <em>afterwards</em>.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface ThreadConfined {

    Owner value();

    /**
     * Who owns the annotated class's mutable state.
     */
    enum Owner {
        /**
         * The {@code game-loop} thread: simulation state, mutated only from tick code.
         */
        GAME_LOOP,
        /**
         * Swing's Event Dispatch Thread: components, input handling, the level lifecycle.
         */
        EVENT_DISPATCH_THREAD,
        /**
         * Whichever thread owns the object holding this one. A helper with no independent
         * lifetime - a turret's aim, a wave accumulator - inherits its container's owner
         * rather than claiming one of its own.
         */
        ENCLOSING
    }
}
