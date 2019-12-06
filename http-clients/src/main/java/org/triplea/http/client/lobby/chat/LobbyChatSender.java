package org.triplea.http.client.lobby.chat;

import com.google.common.annotations.VisibleForTesting;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Consumer;
import lombok.extern.java.Log;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.chat.messages.client.ChatClientEnvelopeFactory;
import org.triplea.http.client.web.socket.GenericWebSocketClient;

/** Client to send chat messages to lobby. */
@Log
public class LobbyChatSender {

  private final GenericWebSocketClient webSocketClient;
  private final ChatClientEnvelopeFactory outboundMessageFactory;

  public LobbyChatSender(final URI lobbyUri, final ApiKey apiKey) {
    this(
        new GenericWebSocketClient(
            URI.create(lobbyUri + LobbyChatListener.LOBBY_CHAT_WEBSOCKET_PATH),
            "Failed to connect to chat."),
        new ChatClientEnvelopeFactory(apiKey));
  }

  @VisibleForTesting
  LobbyChatSender(
      final GenericWebSocketClient webSocketClient,
      final ChatClientEnvelopeFactory clientEventFactory) {
    this.webSocketClient = webSocketClient;
    outboundMessageFactory = clientEventFactory;
  }

  public static LobbyChatSender newClient(final URI lobbyUri, final ApiKey apiKey) {
    return new LobbyChatSender(lobbyUri, apiKey);
  }

  public void slapPlayer(final PlayerName playerName) {
    webSocketClient.send(outboundMessageFactory.slapMessage(playerName));
  }

  public void sendChatMessage(final String message) {
    webSocketClient.send(outboundMessageFactory.sendMessage(message));
  }

  public void close() {
    webSocketClient.close();
  }

  public Collection<ChatParticipant> connect() {
    webSocketClient.send(outboundMessageFactory.connectToChat());
    return new HashSet<>();
  }

  public void updateStatus(final String status) {
    webSocketClient.send(outboundMessageFactory.updateMyPlayerStatus(status));
  }

  public void addConnectionLostListener(final Consumer<String> connectionLostListener) {
    webSocketClient.addConnectionLostListener(connectionLostListener);
  }

  public void addConnectionClosedListener(final Consumer<String> connectionLostListener) {
    webSocketClient.addConnectionClosedListener(connectionLostListener);
  }
}
