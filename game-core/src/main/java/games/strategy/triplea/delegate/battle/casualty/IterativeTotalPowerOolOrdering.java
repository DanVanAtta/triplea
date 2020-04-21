package games.strategy.triplea.delegate.battle.casualty;

import com.google.common.base.Preconditions;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.battle.casualty.power.calculator.StrengthCalculator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.Builder;

@Builder
class IterativeTotalPowerOolOrdering {

  @Nonnull private final CasualtyOrderOfLosses.Parameters parameters;

  List<Unit> sortUnitsForCasualtiesWithSupport() {
    final Collection<Unit> units = new ArrayList<>(parameters.getTargetsToPickFrom());

    final List<Unit> casualtyOrder = new ArrayList<>();
    while (!units.isEmpty()) {
      final UnitType unitType = bestUnitTypeToSelect(units);
      final Unit unitToRemove = findUnitOfType(unitType, units);
      casualtyOrder.add(unitToRemove);
      units.remove(unitToRemove);
    }
    return casualtyOrder;
  }

  private UnitType bestUnitTypeToSelect(final Collection<Unit> units) {
    int largestPower = Integer.MIN_VALUE;
    final Set<UnitType> bestUnitType = new HashSet<>();

    final Map<UnitType, Integer> unitCounts = computeUnitCounts(units);

    for (final UnitType unitType : unitCounts.keySet()) {
      if (bestUnitType.isEmpty()) {
        bestUnitType.add(unitType);
      }

      final Map<UnitType, Integer> decrementUnitCount = new HashMap<>(unitCounts);
      decrementUnitCount.put(unitType, decrementUnitCount.get(unitType) - 1);

      final int strength =
          StrengthCalculator.builder()
              .gameData(parameters.getData())
              .gamePlayer(parameters.getPlayer())
              .combatModifiers(parameters.getCombatModifiers())
              .unitCounts(decrementUnitCount)
              .enemyUnits(parameters.enemyUnitCounts())
              .build()
              .totalPower();

      if (strength > largestPower) {
        bestUnitType.clear();
        bestUnitType.add(unitType);
        largestPower = strength;
      } else if (strength == largestPower) {
        bestUnitType.add(unitType);
      }
    }
    return breakTie(bestUnitType);
  }

  private static Map<UnitType, Integer> computeUnitCounts(final Collection<Unit> units) {
    final Map<UnitType, Integer> unitCounts = new HashMap<>();
    for (final Unit unit : units) {
      if (!unitCounts.containsKey(unit.getType())) {
        unitCounts.put(unit.getType(), 0);
      }
      unitCounts.put(unit.getType(), unitCounts.get(unit.getType()) + 1);
    }
    return unitCounts;
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
