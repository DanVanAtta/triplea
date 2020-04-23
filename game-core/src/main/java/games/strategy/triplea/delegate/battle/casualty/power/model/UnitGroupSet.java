package games.strategy.triplea.delegate.battle.casualty.power.model;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.battle.casualty.CasualtyOrderOfLosses;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;

/**
 * Contains a set of unit power buffs. We for example provide operations where if we remove a unit,
 * we'll decrement the needed unit power buff and shift any support buffs to other units.
 */
@AllArgsConstructor
public class UnitGroupSet {
  private final Map<UnitType, Map<GamePlayer, UnitGroup>> unitGroups;
  private final Set<SupportBuff> supportBuffs;

  public UnitGroupSet(final CasualtyOrderOfLosses.Parameters parameters) {
    this.unitGroups = new HashMap<>();
    for (final Unit unit : parameters.getTargetsToPickFrom()) {
      final var unitTypeByPlayer = new UnitTypeByPlayer(unit.getType(), unit.getOwner());
      if (!unitGroups.containsKey(unitTypeByPlayer.getUnitType())) {
        unitGroups.put(unitTypeByPlayer.getUnitType(), new HashMap<>());
      }
      if (!unitGroups
          .get(unitTypeByPlayer.getUnitType())
          .containsKey(unitTypeByPlayer.getGamePlayer())) {
        final UnitAttachment attachment = UnitAttachment.get(unitTypeByPlayer.getUnitType());
        unitGroups
            .get(unitTypeByPlayer.getUnitType())
            .put(
                unitTypeByPlayer.getGamePlayer(),
                UnitGroup.builder()
                    .unitTypeByPlayer(unitTypeByPlayer)
                    .unitCount(0)
                    .diceRolls(
                        parameters.getCombatModifiers().isDefending()
                            ? attachment.getDefenseRolls(unitTypeByPlayer.getGamePlayer())
                            : attachment.getAttackRolls(unitTypeByPlayer.getGamePlayer()))
                    .strength(
                        parameters.getCombatModifiers().isDefending()
                            ? attachment.getDefense(unitTypeByPlayer.getGamePlayer())
                            : attachment.getAttack(unitTypeByPlayer.getGamePlayer()))
                    .build());
      }
      unitGroups
          .get(unitTypeByPlayer.getUnitType())
          .get(unitTypeByPlayer.getGamePlayer())
          .incrementCount();
    }

    this.supportBuffs = new HashSet<>();
  }

  public void removeUnit(final UnitTypeByPlayer unitTypeByPlayer) {
    final Map<GamePlayer, UnitGroup> groupByPlayer =
        Optional.ofNullable(unitGroups.get(unitTypeByPlayer.getUnitType()))
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Could not find unit: "
                            + unitTypeByPlayer.getUnitType()
                            + ", in groups: "
                            + unitGroups));

    final UnitGroup unitGroup =
        Optional.ofNullable(groupByPlayer.get(unitTypeByPlayer.getGamePlayer()))
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Could not find unit: "
                            + unitTypeByPlayer.getGamePlayer()
                            + ", in groups: "
                            + groupByPlayer));

    unitGroup.decrementCount();
    if (unitGroup.isEmpty()) {
      groupByPlayer.remove(unitTypeByPlayer.getGamePlayer());
      if (groupByPlayer.isEmpty()) {
        unitGroups.remove(unitTypeByPlayer.getUnitType());
      }
    }

    // TODO: re-allocate supports
  }

  public Collection<UnitTypeByPlayer> getWeakestUnit() {
    final Set<UnitTypeByPlayer> weakestUnits = new HashSet<>();
    int weakestStrength = Integer.MAX_VALUE;

    for (final Map.Entry<UnitType, Map<GamePlayer, UnitGroup>> unitTypeEntry :
        unitGroups.entrySet()) {
      for (final Map.Entry<GamePlayer, UnitGroup> unitGroupByPlayerEntry :
          unitTypeEntry.getValue().entrySet()) {

        final int weakestInGroup =
            getWeakestUnit(unitGroupByPlayerEntry.getValue().getUnitTypeByPlayer());
        if (weakestStrength >= weakestInGroup) {

          if (weakestStrength > weakestInGroup) {
            // we have found a new minimum
            weakestStrength = weakestInGroup;
            weakestUnits.clear();
          }
          weakestUnits.add(unitGroupByPlayerEntry.getValue().getUnitTypeByPlayer());
        }
      }
    }
    return weakestUnits;
  }

  private int getWeakestUnit(final UnitTypeByPlayer unitTypeByPlayer) {
    final UnitGroup unitGroup =
        unitGroups.get(unitTypeByPlayer.getUnitType()).get(unitTypeByPlayer.getGamePlayer());
    return unitGroup.getDiceRolls() * unitGroup.getStrength();
  }

  public Collection<UnitTypeByPlayer> unitTypes() {
    final Collection<UnitTypeByPlayer> unitTypes = new HashSet<>();
    for (final Map.Entry<UnitType, Map<GamePlayer, UnitGroup>> unitTypeEntry :
        unitGroups.entrySet()) {
      for (final GamePlayer gamePlayer : unitTypeEntry.getValue().keySet()) {
        unitTypes.add(new UnitTypeByPlayer(unitTypeEntry.getKey(), gamePlayer));
      }
    }
    return unitTypes;
  }

  public boolean isEmpty() {
    return unitGroups.isEmpty();
  }
}
