package games.strategy.triplea.delegate.battle.casualty.power.model;

import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import java.util.Set;
import lombok.Builder;

@Builder
public class SupportBuff {
  /** The unit providing support. */
  private final UnitType supportingUnitType;

  /** The unit actively getting support. */
  private final UnitType supportedUnitType;

  /** The full set of all units that could be supported by this buff. */
  private final Set<Unit> applicableUnitTypes;

  /** Strength adjustment, can be negative. A +1 means unit rolls at +1. */
  private final Integer strengthModifier;

  /** Roll adjustment, can be negative, allows unit to roll more or less dice. */
  private final Integer rollModifier;

  /**
   * The number of supports of this type available. EG: if we have a count of 2, and 3 units, then 2
   * of those 3 units are supported.
   */
  private Integer count;
}
