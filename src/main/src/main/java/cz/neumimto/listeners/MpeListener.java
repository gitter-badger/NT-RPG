package cz.neumimto.listeners;

import com.google.common.base.Optional;
import cz.neumimto.configuration.PluginConfig;
import cz.neumimto.events.PlayerDataPreloadComplete;
import cz.neumimto.events.SkillPrepareEvent;
import cz.neumimto.gui.Gui;
import cz.neumimto.ioc.Inject;
import cz.neumimto.ioc.Listener;
import cz.neumimto.players.CharacterService;
import org.spongepowered.api.Game;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.Subscribe;

/**
 * Created by NeumimTo on 12.2.2015.
 */
@Listener
public class MpeListener {

    @Inject
    private CharacterService characterService;

    @Inject
    private Game game;

    @Subscribe
    public void onPlayerDataPreloadComplete(PlayerDataPreloadComplete event) {
        Optional<Player> retardedOptional = game.getServer().getPlayer(event.getPlayer());
        if (retardedOptional.isPresent()) {
            Player player = retardedOptional.get();
            if (event.getCharacterBases().isEmpty() && PluginConfig.CREATE_FIRST_CHAR_AFTER_LOGIN) {
                characterService.characterCreateState(player, true);
            }
            if (!event.getCharacterBases().isEmpty()) {
                if (PluginConfig.PLAYER_AUTO_CHOOSE_LAST_PLAYED_CHAR || event.getCharacterBases().size() == 1) {
                    characterService.setActiveCharacter(event.getPlayer(),characterService.buildActiveCharacter(player,event.getCharacterBases().get(0)));
                } else {
                    Gui.invokeCharacterMenu(player,event.getCharacterBases());
                }
            }
        }
    }

    @Subscribe(order = Order.EARLY)
    public void onSkillPreUseEvent(SkillPrepareEvent event) {
        if (event.isCancelled())
            return;
        event.getCallbacks().runAll();
    }
}
