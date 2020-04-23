package games.strategy.triplea.delegate.battle.casualty.power.model;

import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import java.util.Set;
import lombok.Builder;
import lombok.Value;

@Builder
public class SupportBuff {
  private final UnitType supportingUnitType;
  private final Set<Unit> applicableUnitTypes;
  private final Integer strengthModifier;
  private final Integer rollModifier;
  private Integer count;
}
