package td.wave;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class WaveAnnouncerTest {

    private final WaveAnnouncer announcer = new WaveAnnouncer();

    @Test
    void announceNotifiesEveryRegisteredListener() {
        AtomicInteger firstCalls = new AtomicInteger();
        AtomicInteger secondCalls = new AtomicInteger();
        announcer.addListener(firstCalls::incrementAndGet);
        announcer.addListener(secondCalls::incrementAndGet);

        announcer.announce();

        assertThat(firstCalls).hasValue(1);
        assertThat(secondCalls).hasValue(1);
    }

    @Test
    void removedListenerStopsReceivingAnnouncements() {
        AtomicInteger calls = new AtomicInteger();
        WaveStartListener listener = calls::incrementAndGet;
        announcer.addListener(listener);
        announcer.removeListener(listener);

        announcer.announce();

        assertThat(calls).hasValue(0);
    }
}
