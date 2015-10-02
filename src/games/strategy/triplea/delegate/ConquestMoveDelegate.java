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
