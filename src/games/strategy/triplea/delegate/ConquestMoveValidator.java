package games.strategy.triplea.delegate;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.dataObjects.MoveValidationResult;

public class ConquestMoveValidator {

  public static MoveValidationResult validateMove(final Collection<Unit> units, final Route route,
      final PlayerID player, final Collection<Unit> transportsToLoad, final Map<Unit, Collection<Unit>> newDependents,
      final boolean isNonCombat, final List<UndoableMove> undoableMoves, final GameData data) {


    return new MoveValidationResult();
  }

}
