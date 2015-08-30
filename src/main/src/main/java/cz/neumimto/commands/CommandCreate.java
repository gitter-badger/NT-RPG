package cz.neumimto.commands;

import cz.neumimto.GroupService;
import cz.neumimto.NtRpgPlugin;
import cz.neumimto.configuration.CommandLocalization;
import cz.neumimto.configuration.CommandPermissions;
import cz.neumimto.configuration.Localization;
import cz.neumimto.configuration.PluginConfig;
import cz.neumimto.ioc.Command;
import cz.neumimto.ioc.Inject;
import cz.neumimto.players.CharacterBase;
import cz.neumimto.players.IActiveCharacter;
import cz.neumimto.players.CharacterService;
import cz.neumimto.players.groups.Guild;
import cz.neumimto.players.groups.NClass;
import cz.neumimto.players.groups.Race;
import org.spongepowered.api.Game;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandResult;
import org.spongepowered.api.util.command.CommandSource;

/**
 * Created by NeumimTo on 23.7.2015.
 */
@Command
public class CommandCreate extends CommandBase {

    @Inject
    CharacterService characterService;

    @Inject
    Game game;

    @Inject
    NtRpgPlugin plugin;

    @Inject
    GroupService groupService;

    public CommandCreate() {
        addAlias(CommandPermissions.COMMAND_CREATE_ALIAS);
        setUsage(CommandLocalization.COMMAND_CREATE_USAGE);
        setDescription(CommandLocalization.COMMAND_CREATE_DESCRIPTION);
    }

    @Override
    public CommandResult process(CommandSource commandSource, String s) throws CommandException {
        if (commandSource instanceof Player) {
            String[] args = s.split(" ");
            if (args.length != 2) {
                commandSource.sendMessage(Texts.of(getUsage(commandSource)));
                return CommandResult.empty();
            }
            if (args[0].equalsIgnoreCase("character")) {
                game.getScheduler().createTaskBuilder().async().execute(() -> {
                    Player player = (Player) commandSource;
                    int i = characterService.canCreateNewCharacter(player.getUniqueId());
                    if (i == 1) {
                        commandSource.sendMessage(Texts.of(Localization.REACHED_CHARACTER_LIMIT));
                    } else if (i == 2) {
                        commandSource.sendMessage(Texts.of(Localization.CHARACTER_EXISTS));
                    } else if (i == 0) {
                        CharacterBase characterBase = new CharacterBase();
                        characterBase.setName(args[1]);
                        characterBase.setGuild(Guild.Default.getName());
                        characterBase.setRace(Race.Default.getName());
                        characterBase.setPrimaryClass(NClass.Default.getName());
                        characterBase.setUuid(player.getUniqueId());
                        characterBase.setLevel(1);
                        characterBase.setSkillPoints(PluginConfig.SKILLPOINTS_ON_START);
                        characterBase.setAttributePoints(PluginConfig.ATTRIBUTEPOINTS_ON_START);
                        characterService.save(characterBase);
                        IActiveCharacter character = characterService.buildActiveCharacterAsynchronously(player, characterBase);
                        characterService.setActiveCharacterSynchronously(player.getUniqueId(), character);
                        commandSource.sendMessage(Texts.of(CommandLocalization.CHARACTER_CREATED.replaceAll("%1",characterBase.getName())));
                    }
                }).submit(plugin);
            }
        }
        return CommandResult.success();
    }
}
