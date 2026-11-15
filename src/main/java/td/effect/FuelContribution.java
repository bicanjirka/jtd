package td.effect;

import td.damage.AttackOrigin;

/**
 * One source's share of a pool (burn, poison or Soulfire): the {@code fuel} it still holds and what it has burned that
 * the next pulse deals ({@code owed}), both in damage units, with the {@code sink} that credits that source.
 */
record FuelContribution(DamageSink sink, AttackOrigin origin, float fuel, float owed) {

    static FuelContribution of(DamageSink sink, AttackOrigin origin, float fuel) {
        return new FuelContribution(sink, origin, fuel, 0f);
    }

    /** {@code more} fuel from the same source. */
    FuelContribution fedBy(float more) {
        return new FuelContribution(this.sink, this.origin, this.fuel + more, this.owed);
    }

    /** {@code share} of its fuel burned, now owed. */
    FuelContribution burned(float share) {
        float burned = this.fuel * share;
        return new FuelContribution(this.sink, this.origin, this.fuel - burned, this.owed + burned);
    }

    /** {@code dealt} of what it owed dealt; the part of a unit left over stays owed. */
    FuelContribution paid(int dealt) {
        return new FuelContribution(this.sink, this.origin, this.fuel, this.owed - dealt);
    }
}
