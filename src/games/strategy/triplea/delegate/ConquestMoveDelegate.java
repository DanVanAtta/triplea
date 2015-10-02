package games.strategy.triplea.delegate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import games.strategy.common.delegate.GameDelegateBridge;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.AbstractTriggerAttachment;
import games.strategy.triplea.attatchments.ICondition;
import games.strategy.triplea.attatchments.TriggerAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.dataObjects.MoveValidationResult;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;
import games.strategy.util.Util;

/**
 * Responsible for moving units on the board.
 * <p>
 * Responsible for checking the validity of a move, and for moving the units. <br>
 */
public class ConquestMoveDelegate extends AbstractMoveDelegate implements IMoveDelegate {
  public static String CLEANING_UP_DURING_MOVEMENT_PHASE = "Cleaning up during movement phase";
  // needToInitialize means we only do certain things once, so that if a game is saved then
  // loaded, they aren't done again
  private boolean m_needToInitialize = true;
  private IntegerMap<Territory> m_PUsLost = new IntegerMap<Territory>();

  /** Creates new MoveDelegate */
  public ConquestMoveDelegate() {}

  /**
   * Called before the delegate will run, AND before "start" is called.
   */
  @Override
  public void setDelegateBridgeAndPlayer(final IDelegateBridge iDelegateBridge) {
    super.setDelegateBridgeAndPlayer(new GameDelegateBridge(iDelegateBridge));
  }

  /**
   * Called before the delegate will run.
   */
  @Override
  public void start() {
    super.start();
    final GameData data = getData();
    if (m_needToInitialize) {
      // territory property changes triggered at beginning of combat move // TODO create new delegate called "start of
      // turn" and move them
      // there.
      // First set up a match for what we want to have fire as a default in this delegate. List out as a composite match
      // OR.
      // use 'null, null' because this is the Default firing location for any trigger that does NOT have 'when' set.
      HashMap<ICondition, Boolean> testedConditions = null;
      final Match<TriggerAttachment> moveCombatDelegateBeforeBonusTriggerMatch =
          new CompositeMatchAnd<TriggerAttachment>(AbstractTriggerAttachment.availableUses,
              AbstractTriggerAttachment.whenOrDefaultMatch(null, null),
              new CompositeMatchOr<TriggerAttachment>(AbstractTriggerAttachment.notificationMatch(),
                  TriggerAttachment.playerPropertyMatch(), TriggerAttachment.relationshipTypePropertyMatch(),
                  TriggerAttachment.territoryPropertyMatch(), TriggerAttachment.territoryEffectPropertyMatch(),
                  TriggerAttachment.removeUnitsMatch(), TriggerAttachment.changeOwnershipMatch()));
      final Match<TriggerAttachment> moveCombatDelegateAfterBonusTriggerMatch =
          new CompositeMatchAnd<TriggerAttachment>(AbstractTriggerAttachment.availableUses,
              AbstractTriggerAttachment.whenOrDefaultMatch(null, null),
              new CompositeMatchOr<TriggerAttachment>(TriggerAttachment.placeMatch()));
      final Match<TriggerAttachment> moveCombatDelegateAllTriggerMatch = new CompositeMatchOr<TriggerAttachment>(
          moveCombatDelegateBeforeBonusTriggerMatch, moveCombatDelegateAfterBonusTriggerMatch);
      if (GameStepPropertiesHelper.isCombatMove(data, false) && games.strategy.triplea.Properties.getTriggers(data)) {
        final HashSet<TriggerAttachment> toFirePossible = TriggerAttachment.collectForAllTriggersMatching(
            new HashSet<PlayerID>(Collections.singleton(m_player)), moveCombatDelegateAllTriggerMatch, m_bridge);
        if (!toFirePossible.isEmpty()) {
          // collect conditions and test them for ALL triggers, both those that we will first before and those we will
          // fire after.
          testedConditions = TriggerAttachment.collectTestsForAllTriggers(toFirePossible, m_bridge);
          final HashSet<TriggerAttachment> toFireBeforeBonus =
              TriggerAttachment.collectForAllTriggersMatching(new HashSet<PlayerID>(Collections.singleton(m_player)),
                  moveCombatDelegateBeforeBonusTriggerMatch, m_bridge);
          if (!toFireBeforeBonus.isEmpty()) {
            // get all triggers that are satisfied based on the tested conditions.
            final Set<TriggerAttachment> toFireTestedAndSatisfied = new HashSet<TriggerAttachment>(
                Match.getMatches(toFireBeforeBonus, AbstractTriggerAttachment.isSatisfiedMatch(testedConditions)));
            // now list out individual types to fire, once for each of the matches above.
            TriggerAttachment.triggerNotifications(toFireTestedAndSatisfied, m_bridge, null, null, true, true, true,
                true);
            TriggerAttachment.triggerPlayerPropertyChange(toFireTestedAndSatisfied, m_bridge, null, null, true, true,
                true, true);
            TriggerAttachment.triggerRelationshipTypePropertyChange(toFireTestedAndSatisfied, m_bridge, null, null,
                true, true, true, true);
            TriggerAttachment.triggerTerritoryPropertyChange(toFireTestedAndSatisfied, m_bridge, null, null, true, true,
                true, true);
            TriggerAttachment.triggerTerritoryEffectPropertyChange(toFireTestedAndSatisfied, m_bridge, null, null, true,
                true, true, true);
            TriggerAttachment.triggerChangeOwnership(toFireTestedAndSatisfied, m_bridge, null, null, true, true, true,
                true);
            TriggerAttachment.triggerUnitRemoval(toFireTestedAndSatisfied, m_bridge, null, null, true, true, true,
                true);
          }
        }
      }
      // placing triggered units at beginning of combat move, but after bonuses and repairing, etc, have been done.
      if (GameStepPropertiesHelper.isCombatMove(data, false) && games.strategy.triplea.Properties.getTriggers(data)) {
        final HashSet<TriggerAttachment> toFireAfterBonus = TriggerAttachment.collectForAllTriggersMatching(
            new HashSet<PlayerID>(Collections.singleton(m_player)), moveCombatDelegateAfterBonusTriggerMatch, m_bridge);
        if (!toFireAfterBonus.isEmpty()) {
          // get all triggers that are satisfied based on the tested conditions.
          final Set<TriggerAttachment> toFireTestedAndSatisfied = new HashSet<TriggerAttachment>(
              Match.getMatches(toFireAfterBonus, AbstractTriggerAttachment.isSatisfiedMatch(testedConditions)));
          // now list out individual types to fire, once for each of the matches above.
          TriggerAttachment.triggerUnitPlacement(toFireTestedAndSatisfied, m_bridge, null, null, true, true, true,
              true);
        }
      }
      if (GameStepPropertiesHelper.isResetUnitStateAtStart(data)) {
        resetUnitStateAndDelegateState();
      }
      m_needToInitialize = false;
    }
  }


  /**
   * Called before the delegate will stop running.
   */
  @Override
  public void end() {
    super.end();
    final GameData data = getData();
    if (GameStepPropertiesHelper.isResetUnitStateAtEnd(data)) {
      resetUnitStateAndDelegateState();
    }
    m_needToInitialize = true;
  }

  @Override
  public Serializable saveState() {
    // see below
    return saveState(true);
  }

  /**
   * Returns the state of the Delegate. We dont want to save the undoState if
   * we are saving the state for an undo move (we dont need it, it will just
   * take up extra space).
   */
  private Serializable saveState(final boolean saveUndo) {
    final MoveExtendedDelegateState state = new MoveExtendedDelegateState();
    state.superState = super.saveState();
    state.m_needToInitialize = m_needToInitialize;
    state.m_PUsLost = m_PUsLost;
    return state;
  }

  @Override
  public void loadState(final Serializable state) {
    final MoveExtendedDelegateState s = (MoveExtendedDelegateState) state;
    super.loadState(s.superState);
    m_needToInitialize = s.m_needToInitialize;
    m_PUsLost = s.m_PUsLost;
  }

  @Override
  public boolean delegateCurrentlyRequiresUserInput() {
    final CompositeMatchAnd<Unit> moveableUnitOwnedByMe = new CompositeMatchAnd<Unit>();
    moveableUnitOwnedByMe.add(Matches.unitIsOwnedBy(m_player));
    // right now, land units on transports have movement taken away when they their transport moves
    moveableUnitOwnedByMe.add(new CompositeMatchOr<Unit>(Matches.unitHasMovementLeft,
        new CompositeMatchAnd<Unit>(Matches.UnitIsLand, Matches.unitIsBeingTransported())));
    // if not non combat, can not move aa units
    if (GameStepPropertiesHelper.isCombatMove(getData(), false)) {
      moveableUnitOwnedByMe.add(Matches.UnitCanNotMoveDuringCombatMove.invert());
    }
    for (final Territory item : getData().getMap().getTerritories()) {
      if (item.getUnits().someMatch(moveableUnitOwnedByMe)) {
        return true;
      }
    }
    return false;
  }


  private void resetUnitStateAndDelegateState() {
    // while not a 'unit state', this is fine here for now. since we only have one instance of this delegate, as long as
    // it gets cleared once per player's turn block, we are fine.
    m_PUsLost.clear();
    final Change change = getResetUnitStateChange(getData());
    if (!change.isEmpty()) {
      // if no non-combat occurred, we may have cleanup left from combat
      // that we need to spawn an event for
      m_bridge.getHistoryWriter().startEvent(CLEANING_UP_DURING_MOVEMENT_PHASE);
      m_bridge.addChange(change);
    }
  }

  public static Change getResetUnitStateChange(final GameData data) {
    final CompositeChange change = new CompositeChange();
    for (final Unit u : data.getUnits()) {
      final TripleAUnit taUnit = TripleAUnit.get(u);
      if (taUnit.getAlreadyMoved() != 0) {
        change.add(ChangeFactory.unitPropertyChange(u, 0, TripleAUnit.ALREADY_MOVED));
      }
      if (taUnit.getWasInCombat()) {
        change.add(ChangeFactory.unitPropertyChange(u, false, TripleAUnit.WAS_IN_COMBAT));
      }
      if (taUnit.getSubmerged()) {
        change.add(ChangeFactory.unitPropertyChange(u, false, TripleAUnit.SUBMERGED));
      }
      if (taUnit.getAirborne()) {
        change.add(ChangeFactory.unitPropertyChange(u, false, TripleAUnit.AIRBORNE));
      }
      if (taUnit.getLaunched() != 0) {
        change.add(ChangeFactory.unitPropertyChange(u, 0, TripleAUnit.LAUNCHED));
      }
      if (!taUnit.getUnloaded().isEmpty()) {
        change.add(ChangeFactory.unitPropertyChange(u, Collections.EMPTY_LIST, TripleAUnit.UNLOADED));
      }
      if (taUnit.getWasLoadedThisTurn()) {
        change.add(ChangeFactory.unitPropertyChange(u, Boolean.FALSE, TripleAUnit.LOADED_THIS_TURN));
      }
      if (taUnit.getUnloadedTo() != null) {
        change.add(ChangeFactory.unitPropertyChange(u, null, TripleAUnit.UNLOADED_TO));
      }
      if (taUnit.getWasUnloadedInCombatPhase()) {
        change.add(ChangeFactory.unitPropertyChange(u, Boolean.FALSE, TripleAUnit.UNLOADED_IN_COMBAT_PHASE));
      }
      if (taUnit.getWasAmphibious()) {
        change.add(ChangeFactory.unitPropertyChange(u, Boolean.FALSE, TripleAUnit.UNLOADED_AMPHIBIOUS));
      }
    }
    return change;
  }

  private void removeMovementFromAirOnDamagedAlliedCarriers(final IDelegateBridge aBridge, final PlayerID player) {
    final GameData data = aBridge.getData();
    final Match<Unit> crippledAlliedCarriersMatch = new CompositeMatchAnd<Unit>(Matches.isUnitAllied(player, data),
        Matches.unitIsOwnedBy(player).invert(), Matches.UnitIsCarrier,
        Matches.UnitHasWhenCombatDamagedEffect(UnitAttachment.UNITSMAYNOTLEAVEALLIEDCARRIER));
    final Match<Unit> ownedFightersMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsAir,
        Matches.UnitCanLandOnCarrier, Matches.unitHasMovementLeft);
    final CompositeChange change = new CompositeChange();
    for (final Territory t : data.getMap().getTerritories()) {
      final Collection<Unit> ownedFighters = t.getUnits().getMatches(ownedFightersMatch);
      if (ownedFighters.isEmpty()) {
        continue;
      }
      final Collection<Unit> crippledAlliedCarriers =
          Match.getMatches(t.getUnits().getUnits(), crippledAlliedCarriersMatch);
      if (crippledAlliedCarriers.isEmpty()) {
        continue;
      }
      for (final Unit fighter : ownedFighters) {
        final TripleAUnit taUnit = (TripleAUnit) fighter;
        if (taUnit.getTransportedBy() != null) {
          if (crippledAlliedCarriers.contains(taUnit.getTransportedBy())) {
            change.add(ChangeFactory.markNoMovementChange(fighter));
          }
        }
      }
    }
    if (!change.isEmpty()) {
      aBridge.addChange(change);
    }
  }

  private Change giveBonusMovement(final IDelegateBridge aBridge, final PlayerID player) {
    final GameData data = aBridge.getData();
    final CompositeChange change = new CompositeChange();
    for (final Territory t : data.getMap().getTerritories()) {
      for (final Unit u : t.getUnits().getUnits()) {
        if (Matches.UnitCanBeGivenBonusMovementByFacilitiesInItsTerritory(t, player, data).match(u)) {
          if (!Matches.isUnitAllied(player, data).match(u)) {
            continue;
          }
          int bonusMovement = Integer.MIN_VALUE;
          final Collection<Unit> givesBonusUnits = new ArrayList<Unit>();
          final Match<Unit> givesBonusUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data),
              Matches.UnitCanGiveBonusMovementToThisUnit(u));
          givesBonusUnits.addAll(Match.getMatches(t.getUnits().getUnits(), givesBonusUnit));
          if (Matches.UnitIsSea.match(u)) {
            final Match<Unit> givesBonusUnitLand = new CompositeMatchAnd<Unit>(givesBonusUnit, Matches.UnitIsLand);
            final List<Territory> neighbors =
                new ArrayList<Territory>(data.getMap().getNeighbors(t, Matches.TerritoryIsLand));
            for (final Territory current : neighbors) {
              givesBonusUnits.addAll(Match.getMatches(current.getUnits().getUnits(), givesBonusUnitLand));
            }
          } else if (Matches.UnitIsLand.match(u)) {
            final Match<Unit> givesBonusUnitSea = new CompositeMatchAnd<Unit>(givesBonusUnit, Matches.UnitIsSea);
            final List<Territory> neighbors =
                new ArrayList<Territory>(data.getMap().getNeighbors(t, Matches.TerritoryIsWater));
            for (final Territory current : neighbors) {
              givesBonusUnits.addAll(Match.getMatches(current.getUnits().getUnits(), givesBonusUnitSea));
            }
          }
          for (final Unit bonusGiver : givesBonusUnits) {
            final int tempBonus = UnitAttachment.get(bonusGiver.getType()).getGivesMovement().getInt(u.getType());
            if (tempBonus > bonusMovement) {
              bonusMovement = tempBonus;
            }
          }
          if (bonusMovement != Integer.MIN_VALUE && bonusMovement != 0) {
            bonusMovement = Math.max(bonusMovement, (UnitAttachment.get(u.getType()).getMovement(player) * -1));
            change.add(ChangeFactory.unitPropertyChange(u, bonusMovement, TripleAUnit.BONUS_MOVEMENT));
          }
        }
      }
    }
    return change;
  }

  @Override
  public String move(final Collection<Unit> units, final Route route, final Collection<Unit> transportsThatCanBeLoaded,
      final Map<Unit, Collection<Unit>> newDependents) {
    final GameData data = getData();
    // there reason we use this, is because if we are in edit mode, we may have a different unit owner than the current
    // player.
    final PlayerID player = getUnitsOwner(units);
    final MoveValidationResult result = MoveValidator.validateMove(units, route, player, transportsThatCanBeLoaded,
        newDependents, GameStepPropertiesHelper.isNonCombatMove(data, false), m_movesToUndo, data);
    final StringBuilder errorMsg = new StringBuilder(100);
    final int numProblems = result.getTotalWarningCount() - (result.hasError() ? 0 : 1);
    final String numErrorsMsg =
        numProblems > 0 ? ("; " + numProblems + " " + MyFormatter.pluralize("error", numProblems) + " not shown") : "";
    if (result.hasError()) {
      return errorMsg.append(result.getError()).append(numErrorsMsg).toString();
    }
    if (result.hasDisallowedUnits()) {
      return errorMsg.append(result.getDisallowedUnitWarning(0)).append(numErrorsMsg).toString();
    }
    if (result.hasUnresolvedUnits()) {
      return errorMsg.append(result.getUnresolvedUnitWarning(0)).append(numErrorsMsg).toString();
    }
    // do the move
    final UndoableMove currentMove = new UndoableMove(data, units, route);
    final String transcriptText = MyFormatter.unitsToTextNoOwner(units) + " moved from " + route.getStart().getName()
        + " to " + route.getEnd().getName();
    m_bridge.getHistoryWriter().startEvent(transcriptText, currentMove.getDescriptionObject());
    m_tempMovePerformer = new MovePerformer();
    m_tempMovePerformer.initialize(this);
    m_tempMovePerformer.moveUnits(units, route, player, transportsThatCanBeLoaded, newDependents, currentMove);
    m_tempMovePerformer = null;
    return null;
  }

  public static Collection<Territory> getEmptyNeutral(final Route route) {
    final Match<Territory> emptyNeutral =
        new CompositeMatchAnd<Territory>(Matches.TerritoryIsEmpty, Matches.TerritoryIsNeutralButNotWater);
    final Collection<Territory> neutral = route.getMatches(emptyNeutral);
    return neutral;
  }

  public static Change ensureCanMoveOneSpaceChange(final Unit unit) {
    final int alreadyMoved = TripleAUnit.get(unit).getAlreadyMoved();
    final int maxMovement = UnitAttachment.get(unit.getType()).getMovement(unit.getOwner());
    final int bonusMovement = TripleAUnit.get(unit).getBonusMovement();
    return ChangeFactory.unitPropertyChange(unit, Math.min(alreadyMoved, (maxMovement + bonusMovement) - 1),
        TripleAUnit.ALREADY_MOVED);
  }

  /**
   * This method is static so it can be called from the client side.
   *
   * @param route
   *        referring route
   * @param units
   *        referring units
   * @param transportsToLoad
   *        units to be loaded
   * @return a map of unit -> transport (null if no mapping can be
   *         done either because there is not sufficient transport capacity or because
   *         a unit is not with its transport)
   */
  public static Map<Unit, Unit> mapTransports(final Route route, final Collection<Unit> units,
      final Collection<Unit> transportsToLoad) {
    if (route.isLoad()) {
      return mapTransportsToLoad(units, transportsToLoad);
    }
    if (route.isUnload()) {
      return mapTransportsAlreadyLoaded(units, route.getStart().getUnits().getUnits());
    }
    return mapTransportsAlreadyLoaded(units, units);
  }

  /**
   * This method is static so it can be called from the client side.
   *
   * @param route
   *        referring route
   * @param units
   *        referring units
   * @param transportsToLoad
   *        units to be loaded
   * @param isload
   * @param player
   *        PlayerID
   * @return a map of unit -> transport (null if no mapping can be
   *         done either because there is not sufficient transport capacity or because
   *         a unit is not with its transport)
   */
  public static Map<Unit, Unit> mapTransports(final Route route, final Collection<Unit> units,
      final Collection<Unit> transportsToLoad, final boolean isload, final PlayerID player) {
    if (isload) {
      return mapTransportsToLoad(units, transportsToLoad);
    }
    if (route != null && route.isUnload()) {
      return mapTransportsAlreadyLoaded(units, route.getStart().getUnits().getUnits());
    }
    return mapTransportsAlreadyLoaded(units, units);
  }

  /**
   * This method is static so it can be called from the client side.
   *
   * @param route
   *        referring route
   * @param units
   *        referring units
   * @param transportsToLoad
   *        units to be loaded
   * @param isload
   * @param player
   *        PlayerID
   * @return a map of unit -> air transport (null if no mapping can be
   *         done either because there is not sufficient transport capacity or because
   *         a unit is not with its transport)
   */
  public static Map<Unit, Unit> mapAirTransports(final Route route, final Collection<Unit> units,
      final Collection<Unit> transportsToLoad, final boolean isload, final PlayerID player) {
    return mapTransports(route, units, transportsToLoad, isload, player);
    // return mapUnitsToAirTransports(units, Match.getMatches(transportsToLoad, Matches.UnitIsAirTransport));
  }

  /**
   * This method is static so it can be called from the client side.
   *
   * @param route
   *        referring route
   * @param units
   *        referring units
   * @param transportsToLoad
   * @param isload
   * @param player
   *        PlayerID
   * @return list of max number of each type of unit that may be loaded
   */
  public static List<Unit> mapAirTransportPossibilities(final Route route, final Collection<Unit> units,
      final Collection<Unit> transportsToLoad, final boolean isload, final PlayerID player) {
    return mapAirTransportsToLoad2(units, Match.getMatches(transportsToLoad, Matches.UnitIsAirTransport));
  }

  /**
   * Returns a map of unit -> transport. Unit must already be loaded in the
   * transport. If no units are loaded in the transports then an empty Map will
   * be returned.
   */
  private static Map<Unit, Unit> mapTransportsAlreadyLoaded(final Collection<Unit> units,
      final Collection<Unit> transports) {
    final Collection<Unit> canBeTransported = Match.getMatches(units, Matches.UnitCanBeTransported);
    final Collection<Unit> canTransport = Match.getMatches(transports, Matches.UnitCanTransport);
    final Map<Unit, Unit> mapping = new HashMap<Unit, Unit>();
    final Iterator<Unit> land = canBeTransported.iterator();
    while (land.hasNext()) {
      final Unit currentTransported = land.next();
      final Unit transport = TransportTracker.transportedBy(currentTransported);
      // already being transported, make sure it is in transports
      if (transport == null) {
        continue;
      }
      if (!canTransport.contains(transport)) {
        continue;
      }
      mapping.put(currentTransported, transport);
    }
    return mapping;
  }

  /**
   * Returns a map of unit -> transport. Tries to find transports to load all
   * units. If it can't succeed returns an empty Map.
   */
  private static Map<Unit, Unit> mapTransportsToLoad(final Collection<Unit> units, final Collection<Unit> transports) {
    final List<Unit> canBeTransported = Match.getMatches(units, Matches.UnitCanBeTransported);
    int transportIndex = 0;
    final Comparator<Unit> transportCostComparator = new Comparator<Unit>() {
      @Override
      public int compare(final Unit o1, final Unit o2) {
        final int cost1 = UnitAttachment.get((o1).getUnitType()).getTransportCost();
        final int cost2 = UnitAttachment.get((o2).getUnitType()).getTransportCost();
        return cost2 - cost1;
      }
    };
    // fill the units with the highest cost first.
    // allows easy loading of 2 infantry and 2 tanks on 2 transports
    // in WW2V2 rules.
    Collections.sort(canBeTransported, transportCostComparator);
    final List<Unit> canTransport = Match.getMatches(transports, Matches.UnitCanTransport);
    final Comparator<Unit> transportCapacityComparator = new Comparator<Unit>() {
      @Override
      public int compare(final Unit o1, final Unit o2) {
        final int capacityLeft1 = TransportTracker.getAvailableCapacity(o1);
        final int capacityLeft2 = TransportTracker.getAvailableCapacity(o1);
        if (capacityLeft1 != capacityLeft2) {
          return capacityLeft1 - capacityLeft2;
        }
        final int capacity1 = UnitAttachment.get((o1).getUnitType()).getTransportCapacity();
        final int capacity2 = UnitAttachment.get((o2).getUnitType()).getTransportCapacity();
        return capacity1 - capacity2;
      }
    };
    // fill transports with the lowest capacity first
    Collections.sort(canTransport, transportCapacityComparator);
    final Map<Unit, Unit> mapping = new HashMap<Unit, Unit>();
    final IntegerMap<Unit> addedLoad = new IntegerMap<Unit>();
    final Comparator<Unit> previouslyLoadedToLast = transportsThatPreviouslyUnloadedComeLast();
    for (final Unit land : canBeTransported) {
      final UnitAttachment landUA = UnitAttachment.get(land.getType());
      final int cost = landUA.getTransportCost();
      boolean loaded = false;
      // we want to try to distribute units evenly to all the transports
      // if the user has 2 infantry, and selects two transports to load
      // we should put 1 infantry in each transport.
      // the algorithm below does not guarantee even distribution in all cases
      // but it solves most of the cases
      final List<Unit> shiftedToEnd = Util.shiftElementsToEnd(canTransport, transportIndex);
      // review the following loop in light of bug ticket 2827064- previously unloaded trns perhaps shouldn't be
      // included.
      Collections.sort(shiftedToEnd, previouslyLoadedToLast);
      final Iterator<Unit> transportIter = shiftedToEnd.iterator();
      while (transportIter.hasNext() && !loaded) {
        transportIndex++;
        if (transportIndex >= canTransport.size()) {
          transportIndex = 0;
        }
        final Unit transport = transportIter.next();
        int capacity = TransportTracker.getAvailableCapacity(transport);
        capacity -= addedLoad.getInt(transport);
        if (capacity >= cost) {
          addedLoad.add(transport, cost);
          mapping.put(land, transport);
          loaded = true;
        }
      }
    }
    return mapping;
  }

  private static Comparator<Unit> transportsThatPreviouslyUnloadedComeLast() {
    return new Comparator<Unit>() {
      @Override
      public int compare(final Unit t1, final Unit t2) {
        if (t1 == t2 || t1.equals(t2)) {
          return 0;
        }
        final boolean t1previous = TransportTracker.hasTransportUnloadedInPreviousPhase(t1);
        final boolean t2previous = TransportTracker.hasTransportUnloadedInPreviousPhase(t2);
        if (t1previous == t2previous) {
          return 0;
        }
        if (t1previous == false) {
          return -1;
        }
        return 1;
      }
    };
  }

  private static List<Unit> mapAirTransportsToLoad2(final Collection<Unit> units, final Collection<Unit> transports) {
    final Comparator<Unit> c = new Comparator<Unit>() {
      @Override
      public int compare(final Unit o1, final Unit o2) {
        final int cost1 = UnitAttachment.get((o1).getUnitType()).getTransportCost();
        final int cost2 = UnitAttachment.get((o2).getUnitType()).getTransportCost();
        // descending transportCost
        return cost2 - cost1;
      }
    };
    Collections.sort((List<Unit>) units, c);
    // Define the max of all units that could be loaded
    final List<Unit> totalLoad = new ArrayList<Unit>();
    // Get a list of the unit categories
    final Collection<UnitCategory> unitTypes = UnitSeperator.categorize(units, null, false, true);
    final Collection<UnitCategory> transportTypes = UnitSeperator.categorize(transports, null, false, false);
    for (final UnitCategory unitType : unitTypes) {
      final int transportCost = unitType.getTransportCost();
      for (final UnitCategory transportType : transportTypes) {
        final int transportCapacity = UnitAttachment.get(transportType.getType()).getTransportCapacity();
        if (transportCost > 0 && transportCapacity >= transportCost) {
          final int transportCount = Match.countMatches(transports, Matches.unitIsOfType(transportType.getType()));
          final int ttlTransportCapacity = transportCount * (int) Math.floor(transportCapacity / transportCost);
          totalLoad.addAll(Match.getNMatches(units, ttlTransportCapacity, Matches.unitIsOfType(unitType.getType())));
        }
      }
    }
    return totalLoad;
  }

  /** Does nothing with Conquest rules */
  @Override
  public int PUsAlreadyLost(final Territory t) {
    return 0;
  }

  /** Does nothing with Conquest rules*/
  @Override
  public void PUsLost(final Territory t, final int amt) { }
}


class ConquestMoveExtendedDelegateState implements Serializable {
  private static final long serialVersionUID = 5352248885420819215L;
  Serializable superState;
  // add other variables here:
  public boolean m_firstRun = true;
  public boolean m_needToInitialize;
  public boolean m_needToDoRockets;
  public IntegerMap<Territory> m_PUsLost;
}
