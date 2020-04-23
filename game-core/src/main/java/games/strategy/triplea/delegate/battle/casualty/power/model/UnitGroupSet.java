package games.strategy.triplea.delegate.battle.casualty.power.model;

import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.battle.casualty.CasualtyOrderOfLosses;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Contains a set of unit power buffs. We for example provide operations where if we remove
 * a unit, we'll decrement the needed unit power buff and shift any support buffs to other units.
 */
public class UnitGroupSet {
  private final Set<UnitGroup> unitGroups = new HashSet<>();
  private final Set<SupportBuff> supportBuffs = new HashSet<>();

  public UnitGroupSet(final CasualtyOrderOfLosses.Parameters parameters) {



  }

  public UnitGroupSet copy() {
    // TODO:
    return this;
  }

  public void removeUnit(final UnitType unitType) {
    // TODO:
  }

  public int getTotalPower() {
    // TODO:
    return 0;
  }

  public Collection<UnitType> getWeakestUnit() {
    // TODO:
    return null;
  }

  public Collection<UnitType> unitTypes() {
    // TODO:
    return null;
  }

  public boolean isEmpty() {
  }
}
