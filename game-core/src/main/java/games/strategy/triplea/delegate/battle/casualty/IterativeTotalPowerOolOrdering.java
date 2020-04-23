package games.strategy.triplea.delegate.battle.casualty;

import com.google.common.base.Preconditions;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.battle.casualty.power.model.UnitGroupSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.Builder;

@Builder
class IterativeTotalPowerOolOrdering {

  @Nonnull private final CasualtyOrderOfLosses.Parameters parameters;

  List<Unit> sortUnitsForCasualtiesWithSupport() {
    final List<Unit> casualtyOrder = new ArrayList<>();
    final List<Unit> remainingUnits = new ArrayList<>(parameters.getTargetsToPickFrom());

    final UnitGroupSet masterUnitGroupSet = new UnitGroupSet(parameters);

    while (!masterUnitGroupSet.isEmpty()) {
      final Collection<UnitType> unitType = masterUnitGroupSet.getWeakestUnit();
      final UnitType typeToPick = breakTie(unitType);
      masterUnitGroupSet.removeUnit(typeToPick);
      final Unit unitToRemove = findUnitOfType(typeToPick, remainingUnits);
      casualtyOrder.add(unitToRemove);
      remainingUnits.remove(unitToRemove);
    }
    return casualtyOrder;
  }

  private static Unit findUnitOfType(final UnitType unitType, final Collection<Unit> units) {
    return units.stream()
        .filter(unit -> unit.getType().equals(unitType))
        .findAny()
        .orElseThrow(
            () ->
                new RuntimeException(
                    "Error, expected to find unit type: " + unitType + " in units: " + units));
  }

  private UnitType breakTie(final Collection<UnitType> unitTypes) {
    Preconditions.checkArgument(!unitTypes.isEmpty());

    return OolTieBreaker.builder() //
        .gamePlayer(parameters.getPlayer())
        .build()
        .apply(unitTypes);
  }
}
