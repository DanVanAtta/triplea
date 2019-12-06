package org.triplea.http.client.lobby.game.listing.messages;

import java.util.function.Consumer;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.triplea.http.client.lobby.game.listing.GameListingListeners;
import org.triplea.http.client.lobby.game.listing.LobbyGameListing;
import org.triplea.http.client.web.socket.messages.WebsocketMessageType;

/** Represents the types of messages server can send to client for game listing updates. */
@AllArgsConstructor
@Getter(onMethod_ = @Override)
public
enum GameListingMessageType implements WebsocketMessageType<GameListingListeners> {
  GAME_ADDED(LobbyGameListing.class, GameListingListeners::getGameUpdatedListener),
  GAME_REMOVED(String.class, GameListingListeners::getGameRemovedListener);

  private final Class<?> classType;
  private final Function<GameListingListeners, Consumer<?>> listenerMethod;
}
