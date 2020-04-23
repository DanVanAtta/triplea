package games.strategy.triplea.delegate.battle.casualty.power.model;

import games.strategy.engine.data.UnitType;
import javax.annotation.Nonnull;
import lombok.Builder;

@Builder
public class UnitGroup {
  @Nonnull private final UnitType unitType;
  @Nonnull private final Integer strength;
  @Nonnull private final Integer diceRolls;
  @Nonnull private Integer unitCount;
}
