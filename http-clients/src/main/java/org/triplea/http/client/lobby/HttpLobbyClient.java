package org.triplea.http.client.lobby;

import java.net.URI;
import java.util.function.Consumer;
import lombok.Getter;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.lobby.chat.LobbyChatListener;
import org.triplea.http.client.lobby.chat.LobbyChatSender;
import org.triplea.http.client.lobby.game.ConnectivityCheckClient;
import org.triplea.http.client.lobby.game.listing.GameListingClient;
import org.triplea.http.client.lobby.moderator.ModeratorChatClient;
import org.triplea.http.client.lobby.moderator.toolbox.HttpModeratorToolboxClient;
import org.triplea.http.client.lobby.user.account.UserAccountClient;

/** Holder class for the various http clients that access lobby resources. */
@Getter
public class HttpLobbyClient {
  private final ConnectivityCheckClient connectivityCheckClient;
  private final GameListingClient gameListingClient;
  private final HttpModeratorToolboxClient httpModeratorToolboxClient;
  private final LobbyChatSender lobbyChatClient;
  private final LobbyChatListener lobbyChatListener;
  private final ModeratorChatClient moderatorLobbyClient;
  private final UserAccountClient userAccountClient;

  private HttpLobbyClient(final URI lobbyUri, final ApiKey apiKey) {
    connectivityCheckClient = ConnectivityCheckClient.newClient(lobbyUri, apiKey);
    gameListingClient = GameListingClient.newClient(lobbyUri, apiKey);
    httpModeratorToolboxClient = HttpModeratorToolboxClient.newClient(lobbyUri, apiKey);
    lobbyChatClient = LobbyChatSender.newClient(lobbyUri, apiKey);
    lobbyChatListener = new LobbyChatListener(lobbyUri);
    moderatorLobbyClient = ModeratorChatClient.newClient(lobbyUri, apiKey);
    userAccountClient = UserAccountClient.newClient(lobbyUri, apiKey);
  }

  public static HttpLobbyClient newClient(final URI lobbyUri, final ApiKey apiKey) {
    return new HttpLobbyClient(lobbyUri, apiKey);
  }

  /**
   * Connection closed listener is invoked whenever the underlying connection is closed, whether by
   * ur or remote server.
   */
  public void addConnectionClosedListener(final Consumer<String> connectionClosedListener) {
    lobbyChatClient.addConnectionLostListener(connectionClosedListener);
  }
}
