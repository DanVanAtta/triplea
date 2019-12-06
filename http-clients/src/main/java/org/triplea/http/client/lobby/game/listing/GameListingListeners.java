package org.triplea.http.client.lobby.game.listing;

import java.util.function.Consumer;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class GameListingListeners {
  @Nonnull private final Consumer<LobbyGameListing> gameUpdatedListener;
  @Nonnull private final Consumer<String> gameRemovedListener;
}
