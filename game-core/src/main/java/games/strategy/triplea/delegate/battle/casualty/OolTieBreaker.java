package games.strategy.triplea.delegate.battle.casualty;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitAttachment;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.Builder;

/**
 * Assuming we have multiple unit types all with the same effective combat power, this function will
 * decide which one is the best to choose for a casualty.
 */
@Builder
public class OolTieBreaker implements Function<Collection<UnitType>, UnitType> {

  @Nonnull private final GamePlayer gamePlayer;

  @Override
  public UnitType apply(final Collection<UnitType> unitTypes) {
    return unitTypes.stream()
        .sorted(Comparator.comparingInt(this::totalAttackAndDefensivePower))
        .collect(Collectors.toList())
        .iterator()
        .next();
  }

  private int totalAttackAndDefensivePower(final UnitType unitType) {
    final var unitAttachment = UnitAttachment.get(unitType);
    return unitAttachment.getAttack(gamePlayer) + unitAttachment.getDefense(gamePlayer);
  }
}
