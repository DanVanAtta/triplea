package org.triplea.http.client.lobby.game.listing;

import java.net.URI;
import org.triplea.http.client.lobby.game.listing.messages.GameListingMessageType;
import org.triplea.http.client.web.socket.WebsocketListener;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;

/**
 * Http client for interacting with lobby game listing. Can be used to post, remove, boot, fetch and
 * update games.
 */
public class GameListingWebsocketListener
    extends WebsocketListener<GameListingMessageType, GameListingListeners> {

  public static final String GAME_LISTING_WEBSOCKET = "/lobby/games/listings/ws";

  GameListingWebsocketListener(final URI hostUri, final GameListingListeners gameListingListeners) {
    super(hostUri, GAME_LISTING_WEBSOCKET, gameListingListeners);
  }

  @Override
  public GameListingMessageType readMessageType(final ServerMessageEnvelope serverMessageEnvelope) {
    return GameListingMessageType.valueOf(serverMessageEnvelope.getMessageType());
  }
}
