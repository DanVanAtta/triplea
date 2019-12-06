package org.triplea.http.client.web.socket;

import com.google.common.base.Preconditions;
import java.net.URI;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.Setter;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;
import org.triplea.http.client.web.socket.messages.WebsocketMessageType;

/** Uber-generic websocket listener! Just pass it the right listeners. */
public abstract class WebsocketListener<
        MessageTypeT extends WebsocketMessageType<ListenersTypeT>, ListenersTypeT>
    implements Consumer<ServerMessageEnvelope> {
  private final GenericWebSocketClient webSocketClient;
  @Setter
  private ListenersTypeT listeners;

  protected WebsocketListener(
      final URI hostUri, final String websocketPath, final ListenersTypeT listeners) {
    final URI websocketUri = URI.create(hostUri + websocketPath);
    webSocketClient =
        new GenericWebSocketClient(websocketUri, "Failed to connect to " + websocketUri);
    webSocketClient.addMessageListener(this);
    this.listeners = listeners;
  }

  protected WebsocketListener(
      final URI hostUri, final String websocketPath) {
    this(hostUri, websocketPath, null);
  }

  public void close() {
    webSocketClient.close();
  }

  @Override
  public void accept(final ServerMessageEnvelope serverMessageEnvelope) {
    Preconditions.checkState(listeners != null);
    readMessageTypeValue(serverMessageEnvelope)
        .ifPresent(
            messageType -> messageType.sendPayloadToListener(serverMessageEnvelope, listeners));
  }

  protected abstract MessageTypeT readMessageType(ServerMessageEnvelope serverMessageEnvelope);

  private Optional<MessageTypeT> readMessageTypeValue(
      final ServerMessageEnvelope serverMessageEnvelope) {
    try {
      return Optional.of(readMessageType(serverMessageEnvelope));
    } catch (final IllegalArgumentException e) {
      // expect this to happen when we try to use an enum 'valueOf' method for a type
      // that does not match the enum.
      return Optional.empty();
    }
  }
}
