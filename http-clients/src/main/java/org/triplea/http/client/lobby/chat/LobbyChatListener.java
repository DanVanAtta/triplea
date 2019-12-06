package org.triplea.http.client.lobby.chat;

import java.net.URI;
import lombok.extern.java.Log;
import org.triplea.http.client.lobby.chat.messages.server.ChatServerMessageType;
import org.triplea.http.client.web.socket.WebsocketListener;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;

/** Client that receives lobby chat messages. */
@Log
public class LobbyChatListener
    extends WebsocketListener<ChatServerMessageType, ChatMessageListeners> {

  public static final String LOBBY_CHAT_WEBSOCKET_PATH = "/lobby/chat/websocket";

  public LobbyChatListener(final URI hostUri) {
    super(hostUri, LOBBY_CHAT_WEBSOCKET_PATH);
  }

  @Override
  protected ChatServerMessageType readMessageType(
      final ServerMessageEnvelope serverMessageEnvelope) {
    return ChatServerMessageType.valueOf(serverMessageEnvelope.getMessageType());
  }
}
