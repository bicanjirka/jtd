package td.economy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RefundTest {

    @Test
    void whatNoWaveHasUsedComesBackWholeAndWhatOneHasAtTheSellShare() {
        Refund refund = Refund.of(40, 100);

        assertThat(refund.inFull()).isEqualTo(40);
        assertThat(refund.shared()).isEqualTo(SellRefund.of(100));
        assertThat(refund.total()).isEqualTo(40 + SellRefund.of(100));
    }

    @Test
    void aRefundIsFullOnlyWhenNoneOfItIsAShare() {
        assertThat(Refund.of(40, 0).isFull()).isTrue();
        assertThat(Refund.of(40, 100).isFull()).isFalse();
        assertThat(Refund.none().isFull()).isFalse();
    }

    @Test
    void whatGivingSomethingUpIsWorthIsEachPartLessWhatIsKept() {
        Refund whole = new Refund(50, 30);

        assertThat(whole.minus(new Refund(20, 30))).isEqualTo(new Refund(30, 0));
    }
}
