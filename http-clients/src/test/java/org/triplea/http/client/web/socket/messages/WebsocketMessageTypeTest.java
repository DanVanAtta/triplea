package org.triplea.http.client.web.socket.messages;

import static org.mockito.Mockito.*;

import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WebsocketMessageTypeTest {
  @Getter
  @AllArgsConstructor
  private static class ExampleMessageListeners {
    @Nonnull Consumer<String> listener;
  }

  @AllArgsConstructor
  @Getter(onMethod_ = @Override)
  private enum ExampleMessageType implements WebsocketMessageType<ExampleMessageListeners> {
    MESSAGE_TYPE(String.class, ExampleMessageListeners::getListener);

    private final Class<?> classType;
    private final Function<ExampleMessageListeners, Consumer<?>> listenerMethod;
  }

  @Mock private Consumer<String> listenerImplementation;

  private ExampleMessageListeners exampleMessageListeners;

  @BeforeEach
  void setup() {
    exampleMessageListeners = new ExampleMessageListeners(listenerImplementation);
  }

  @Test
  @DisplayName(
      "Verify that the message payload is extracted and sent to the listener implementation")
  void routeMessageToListener() {
    final ServerMessageEnvelope serverMessageEnvelope =
        ServerMessageEnvelope.packageMessage(ExampleMessageType.MESSAGE_TYPE.toString(), "payload");

    ExampleMessageType.MESSAGE_TYPE.sendPayloadToListener(
        serverMessageEnvelope, exampleMessageListeners);

    verify(listenerImplementation).accept("payload");
  }

  @Test
  void badMessageTypeThrows() {
    final ServerMessageEnvelope serverMessageEnvelope =
        ServerMessageEnvelope.packageMessage("wrong type", "payload");

    ExampleMessageType.MESSAGE_TYPE.sendPayloadToListener(
        serverMessageEnvelope, exampleMessageListeners);

    verify(listenerImplementation).accept("payload");
  }
}
