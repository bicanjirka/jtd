package td.ui;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import javax.swing.SwingUtilities;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Builds the test class and runs each test on the EDT, as the game builds and drives Swing. Off it, the work a
 * component posts to the EDT (a caret laying its text out again) races the test's own calls on that component.
 */
final class EventDispatchThreadExtension implements InvocationInterceptor {

    /** A step that may throw anything, as {@link Invocation#proceed} does. */
    private interface Step<E extends Throwable> {
        void run() throws E;
    }

    @Override
    public <T> T interceptTestClassConstructor(Invocation<T> invocation,
                                               ReflectiveInvocationContext<Constructor<T>> invocationContext,
                                               ExtensionContext extensionContext) throws Throwable {
        return onEventDispatchThread(invocation);
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
                                    ExtensionContext extensionContext) throws Throwable {
        onEventDispatchThread(invocation);
    }

    private static <T> T onEventDispatchThread(Invocation<T> invocation) throws Throwable {
        AtomicReference<T> result = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(unchecked(() -> result.set(invocation.proceed())));
        } catch (InvocationTargetException failed) {
            throw failed.getCause();
        }
        // What the test left queued runs now, not while the next test runs.
        SwingUtilities.invokeAndWait(() -> { });
        return result.get();
    }

    /**
     * {@code step} as a {@link Runnable}. Checked exceptions exist only for the compiler, so the cast lets whatever
     * the step throws reach {@code invokeAndWait}, which reports it as the cause of an {@link InvocationTargetException}.
     */
    @SuppressWarnings("unchecked")
    private static Runnable unchecked(Step<? extends Throwable> step) {
        return ((Step<RuntimeException>) step)::run;
    }
}
