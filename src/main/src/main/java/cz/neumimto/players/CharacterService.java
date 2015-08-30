package cz.neumimto.players;

import cz.neumimto.GroupService;
import cz.neumimto.LoggingService;
import cz.neumimto.NtRpgPlugin;
import cz.neumimto.configuration.PluginConfig;
import cz.neumimto.effects.EffectService;
import cz.neumimto.effects.common.def.ManaRegeneration;
import cz.neumimto.events.*;
import cz.neumimto.inventory.InventoryService;
import cz.neumimto.ioc.Inject;
import cz.neumimto.ioc.Singleton;
import cz.neumimto.persistance.PlayerDao;
import cz.neumimto.players.events.PartyInviteEvent;
import cz.neumimto.players.groups.Guild;
import cz.neumimto.players.groups.NClass;
import cz.neumimto.players.groups.Race;
import cz.neumimto.players.parties.Party;
import cz.neumimto.players.properties.DefaultProperties;
import cz.neumimto.players.properties.PlayerPropertyService;
import cz.neumimto.skills.*;
import cz.neumimto.utils.Utils;
import org.spongepowered.api.Game;
import org.spongepowered.api.attribute.Attributes;
import org.spongepowered.api.data.manipulator.mutable.AttributeData;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.item.ItemType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by NeumimTo on 26.12.2014.
 */
@Singleton
public class CharacterService {

    @Inject
    private SkillService skillService;

    @Inject
    private Game game;

    @Inject
    private NtRpgPlugin plugin;

    @Inject
    private PlayerDao playerDao;

    @Inject
    private LoggingService logger;

    @Inject
    private InventoryService inventoryService;

    @Inject
    private GroupService groupService;

    @Inject
    private PlayerPropertyService playerPropertyService;

    private Map<UUID, NPlayer> playerWrappers = new ConcurrentHashMap<>();
    private Map<UUID, IActiveCharacter> characters = new HashMap<>();

    @Inject
    private EffectService effectService;

    /**
     * Creates a dummy character for a player until player chooses new one
     *
     * @param id
     */
    public void putInLoadQueue(UUID id) {
        characters.put(id, buildDummyChar(id));
        game.getScheduler().createTaskBuilder().delay(0).name("PlayerDataTaskLoad-" + id).async().execute(() -> {
            final List<CharacterBase> playerCharacters = playerDao.getPlayersCharacters(id);
            game.getScheduler().createTaskBuilder().delay(0).name("Callback-PlayerDataTaskLoad" + id).execute(() -> {
                PlayerDataPreloadComplete event = new PlayerDataPreloadComplete(id, playerCharacters);
                game.getEventManager().post(event);
            }).submit(plugin);
        }).submit(plugin);
    }

    public void updateArmorRestrictions(IActiveCharacter character) {
        Set<ItemType> allowedArmor = character.updateItemRestrictions().getAllowedArmor();
        EventCharacterArmorPostUpdate event = new EventCharacterArmorPostUpdate(character, allowedArmor);
        game.getEventManager().post(event);
    }


    public void characterCreateState(Player player, boolean first) {
        if (first) {
            setActiveCharacter(player.getUniqueId(), buildDummyChar(player.getUniqueId()));
        }

    }

    /**
     * Gets list of player's characters
     * The method should be invoked only from async task
     *
     * @param id
     * @return
     */
    public List<CharacterBase> getPlayersCharacters(UUID id) {
        return playerDao.getPlayersCharacters(id);
    }

    public void putInSaveQueue(CharacterBase base) {
        game.getScheduler().createTaskBuilder().async().name("PlayerDataTaskSave").execute(() -> {
            save(base);
        }).submit(plugin);
    }

    /**
     * Saves player data
     * @param base
     */
    public void save(CharacterBase base) {
        playerDao.update(base);
    }


    public NPlayer getPlayerWrapper(UUID uuid) {
        return playerWrappers.get(uuid);
    }

    /**
     * @param uuid
     * @return character, never returns null
     */
    public IActiveCharacter getCharacter(UUID uuid) {
        return characters.get(uuid);
    }

    /**
     * Activates character for specified player, replaces old
     * @param uuid
     * @param character
     * @return new character
     */
    public IActiveCharacter setActiveCharacter(UUID uuid, IActiveCharacter character) {
        IActiveCharacter activeCharacter = getCharacter(uuid);
        if (activeCharacter == null) {
            characters.put(uuid, character);
        } else {
            characters.remove(uuid);
            if (character != null) {
                characters.put(uuid, character);
                initActiveCharacter(character);
            }
        }
        return character;
    }

    /**
     * Creates party
     * @param character
     */
    public void createParty(ActiveCharacter character) {
        UUID id = character.getPlayer().getUniqueId();
        CancellableEvent event = new PartyCreateEvent(character);
        game.getEventManager().post(event);
        if (!event.isCancelled()) {
            Party party = new Party(character);
            party.addPlayer(character);
        }
    }

    /**
     * Changes leader of a party. The old leader is not removed from the party
     * @param party
     * @param newleader
     */
    public void changePartyLeader(Party party, ActiveCharacter newleader) {
        party.setLeader(newleader);
    }

    /**
     *
     * @param party
     * @param character
     * @return
     */
    public int partyJoin(Party party, ActiveCharacter character) {
        if (party.getPlayers().contains(character)) {
            return 1;
        }
        if (party.getInvites().contains(character.getPlayer().getUniqueId())) {
            party.getInvites().remove(character.getPlayer().getUniqueId());
            party.addPlayer(character);
        }
        return 2;
    }

    /**
     * Invites character to a party
     * @param party
     * @param character
     * @return 1 - if character is already in the party
     *         0 - ok
     */
    public int inviteToParty(Party party, ActiveCharacter character) {
        if (party.getPlayers().contains(character)) {
            return 1;
        }
        PartyInviteEvent event = new PartyInviteEvent(party,character);
        if (!event.isCancelled()) {
            party.getInvites().add(character.getPlayer().getUniqueId());
            return 0;
        }
        return 1;
    }

    private void initActiveCharacter(IActiveCharacter character) {
        effectService.addEffect(new ManaRegeneration(character),character);
    }

    /**
     * Does nothing if character is PreloadChar.
     * If group is null nothing is changed
     * character is saved
     * Updates item restriction
     *
     * @param character
     * @param nClass
     * @param slot      defines primary/Secondary/... class
     * @param race
     * @param guild
     */
    public void updatePlayerGroups(IActiveCharacter character, NClass nClass, int slot, Race race, Guild guild) {
        if (character.isStub())
            return;
        if (nClass != null)
            character.setClass(nClass, slot);
        if (race != null)
            character.setRace(race);
        if (guild != null) {
            character.setGuild(guild);
        }
        putInSaveQueue(character.getCharacterBase());
        updateArmorRestrictions(character);
        updateMaxHealth(character);
        updateMaxMana(character);
    }


    /**
     * updates maximal mana from character properties
     * @param character
     */
    public void updateMaxMana(IActiveCharacter character) {
        IReservable mana = character.getMana();
        float max_mana = character.getCharacterProperty(DefaultProperties.max_mana) - character.getCharacterProperty(DefaultProperties.reserved_mana);
        float actreserved = character.getCharacterProperty(DefaultProperties.reserved_mana);
        float reserved = character.getCharacterProperty(DefaultProperties.reserved_mana_multiplier);
        float maxval = max_mana-(actreserved*reserved);
        character.getMana().setMaxValue(maxval);
    }

    /**
     * Updates maximal health from character properties
     * @param character
     */
    public void updateMaxHealth(IActiveCharacter character) {
        Health health = character.getHealth();
        float max_health = character.getCharacterProperty(DefaultProperties.max_health) - character.getCharacterProperty(DefaultProperties.reserved_health);
        float actreserved = character.getCharacterProperty(DefaultProperties.reserved_health);
        float reserved = character.getCharacterProperty(DefaultProperties.reserved_health_multiplier);
        float maxval = max_health-(actreserved*reserved);
        if (maxval <= 0) {
            maxval = 1;
        }
        character.getHealth().setMaxValue(maxval);
    }


    public void removeCachedWrapper(UUID uuid) {
        removeCachedCharacter(uuid);
        playerWrappers.remove(uuid);
    }

    public void removeCachedCharacter(UUID uuid) {
        characters.remove(uuid);
    }

    /**
     * Removes all player's data from database.
     * Only way how to get back deleted data is backup your database.
     *
     * @param uniqueId player's uuid
     */
    public void removePlayerData(UUID uniqueId) {
        removeCachedWrapper(uniqueId);
        playerDao.deleteData(uniqueId, (i) -> logger.debug(" DELETED " + i + " CHARACTERS OF " + uniqueId));
    }

    /**
     * builds dummy character as a placeholder, Almost all actions are restricted
     * @param uuid
     * @return
     */
    public PreloadCharacter buildDummyChar(UUID uuid) {
        return new PreloadCharacter(uuid);
    }

    /**
     * Creates active character from character base
     * Runs async task, which fetch cooldowns and skills;
     *
     * @param player
     * @param characterBase
     * @return
     */
    public ActiveCharacter buildActiveCharacter(Player player, CharacterBase characterBase) {
        ActiveCharacter activeCharacter = new ActiveCharacter(player, characterBase);
        activeCharacter.setRace(groupService.getRace(characterBase.getRace()));
        activeCharacter.setGuild(groupService.getGuild(characterBase.getGuild()));
        activeCharacter.setPrimaryClass(groupService.getNClass(characterBase.getPrimaryClass()));
        String s = activeCharacter.getPrimaryClass().getnClass().getName();
        Double d = characterBase.getClasses().get(s);
        if (d != null) {
            activeCharacter.getPrimaryClass().setExperiences(d);
        }
        Map<Integer, Float> defaults = playerPropertyService.getDefaults();
        for (Map.Entry<Integer, Float> integerFloatEntry : defaults.entrySet()) {
            activeCharacter.setCharacterProperty(integerFloatEntry.getKey(),integerFloatEntry.getValue());
        }
        game.getScheduler().createTaskBuilder().delay(0).async().name("FetchCharBaseDataAsync-" + player.getUniqueId())
                .execute(() -> {
                    resolveSkillsCds(characterBase,activeCharacter);
                    game.getScheduler().createTaskBuilder().name("FetchCharBaseDataCallback-"+ player.getUniqueId()).execute(() -> initSkills(activeCharacter)).submit(plugin);
                }).submit(plugin);
        return activeCharacter;
    }

    private void initSkills(ActiveCharacter activeCharacter) {
        activeCharacter.getSkills().values().stream().forEach(e -> e.getSkill().onCharacterInit(activeCharacter,e.getLevel()));
    }

    private void resolveSkillsCds(CharacterBase characterBase,IActiveCharacter character) {
        Map<String, Long> cooldowns = characterBase.getCooldowns();
        Map<String, Integer> skills = characterBase.getSkills();
        long l = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> iterator = cooldowns.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> next = iterator.next();
            if (next.getValue() <= l) ;
            iterator.remove();
        }
        for (Map.Entry<String, Integer> stringIntegerEntry : skills.entrySet()) {
            ExtendedSkillInfo info = new ExtendedSkillInfo();
            ISkill skill = skillService.getSkill(stringIntegerEntry.getKey());
            if (skill != null) {
                info.setLevel(stringIntegerEntry.getValue());
                //this should be threadsafe, skilltrees are created only once, during the plugin startup0
                SkillInfo info1 = character.getPrimaryClass().getnClass().getSkillTree().getSkills().get(skill.getName());
                info.setSkillInfo(info1);
                character.addSkill(info.getSkill().getName(), info);
            }
        }
    }

    /**
     * Due to fetchtype Lazy is [u]recommended[/u] to run this method only from async thread.
     *
     * @param player
     * @param characterBase
     * @return
     */
    public ActiveCharacter buildActiveCharacterAsynchronously(Player player, CharacterBase characterBase) {
        ActiveCharacter activeCharacter = new ActiveCharacter(player, characterBase);
        activeCharacter.setRace(groupService.getRace(characterBase.getRace()));
        activeCharacter.setGuild(groupService.getGuild(characterBase.getGuild()));
        activeCharacter.setPrimaryClass(groupService.getNClass(characterBase.getPrimaryClass()));
        String s = activeCharacter.getPrimaryClass().getnClass().getName();
        Double d = characterBase.getClasses().get(s);
        if (d != null) {
            activeCharacter.getPrimaryClass().setExperiences(d);
        }
        resolveSkillsCds(characterBase,activeCharacter);
        game.getScheduler().createTaskBuilder().name("FetchCharBaseDataCallback-" + player.getUniqueId()).execute(() -> {
            Map<Integer, Float> defaults = playerPropertyService.getDefaults();
            for (Map.Entry<Integer, Float> integerFloatEntry : defaults.entrySet()) {
                activeCharacter.setCharacterProperty(integerFloatEntry.getKey(), integerFloatEntry.getValue());
            }
            initSkills(activeCharacter);
        }).submit(plugin);
        return activeCharacter;
    }

    /**
     * @param uniqueId player's uuid
     * @return 1 - if player reached maximal amount of characters
     * 2 - if player has character with same name
     * 0 - ok
     */
    public int canCreateNewCharacter(UUID uniqueId) {
        //todo use db query
        List<CharacterBase> list = getPlayersCharacters(uniqueId);
        if (list.size() >= PluginConfig.PLAYER_MAX_CHARS) {
            return 1;
        }
        if (list.stream().anyMatch(c -> c.getName().equals(uniqueId))) {
            return 2;
        }
        return 0;
    }

    /**
     * Invokes synchronous task which activates character for player
     *
     * @param uuid
     * @param character
     */
    public void setActiveCharacterSynchronously(UUID uuid, IActiveCharacter character) {
        game.getScheduler().createTaskBuilder().name("SyncTaskActivateCharOf" + uuid).execute(() -> {
            setActiveCharacter(uuid, character);
        }).submit(plugin);
    }

    /**
     * Heals the character and`fire an event
     * @param character
     * @param healedamount
     * @return healed hp
     */
    public double healCharacter(IActiveCharacter character, float healedamount) {
        if (character.getHealth().getValue() == character.getHealth().getMaxValue()) {
            return 0;
        }
        SkillHealEvent event = null;
        if (character.getHealth().getValue() + healedamount > character.getHealth().getMaxValue()) {
            healedamount = (float) (character.getHealth().getMaxValue() - (character.getHealth().getValue() + healedamount));
        }
        event = new SkillHealEvent(character, healedamount);
        game.getEventManager().post(event);
        if (event.isCancelled())
            return 0;
        return setCharacterHealth(event.getCharacter(), event.getAmount());
    }


    /**
     * sets character's hp to choosen amount.
     * @param character
     * @param amount
     * @return difference
     */
    public double setCharacterHealth(IActiveCharacter character, double amount) {
        if (character.getHealth().getValue() + amount > character.getHealth().getMaxValue()) {
            setCharacterToFullHealth(character);
            return character.getHealth().getMaxValue() - (character.getHealth().getValue() + amount);
        }
        character.getHealth().setValue(character.getHealth().getValue() + amount);
        return amount;
    }

    /**
     * sets character to its full health
     * @param character
     */
    public void setCharacterToFullHealth(IActiveCharacter character) {
        character.getHealth().setValue(character.getHealth().getMaxValue());
    }

    /**
     * @param character
     * @param skill
     * @return 1 if character has no skillpoints,
     * 2 if character has not learned the skill yet
     * 3 if skill requires higher level. The formula is MinPlayerLevel + skillLevel > characterlevel
     * (this restricts abusing where player might rush certain skills in a skilltree and spending all skillpoints on a single skill).
     * 4 if skill is on max level
     * 5 - if event is cancelled
     * 0 - ok
     */
    public int upgradeSkill(IActiveCharacter character, ISkill skill) {
        if (character.getSkillPoints() <= 0) {
            return 1;
        }
        ExtendedSkillInfo extendedSkillInfo = character.getSkillInfo(skill);
        if (extendedSkillInfo == null)
            return 2;
        if (extendedSkillInfo.getLevel() + extendedSkillInfo.getSkillInfo().getMinPlayerLevel() > character.getLevel()) {
            return 3;
        }
        if (extendedSkillInfo.getLevel() + 1 > extendedSkillInfo.getSkillInfo().getMaxSkillLevel()) {
            return 4;
        }
        SkillUpgradeEvent event = new SkillUpgradeEvent(character, skill, extendedSkillInfo.getLevel() + 1);
        game.getEventManager().post(event);
        if (event.isCancelled())
            return 5;
        extendedSkillInfo.setLevel(event.getLevel());
        short s = character.getCharacterBase().getSkillPoints();
        character.getCharacterBase().setSkillPoints((short) (s - 1));
        character.getCharacterBase().setUsedSkillPoints((short) (s + 1));
        putInSaveQueue(character.getCharacterBase());
        skill.skillUpgrade(character, event.getLevel());
        return 0;
    }

    /**
     * @param character
     * @param skill
     * @return 1 - if the player has no avalaible skill points
     *         2 - if the player is below required level
     *         4 - if the player has none of required skills in the chain
     *         5 - if the player has none of (soft)required skills in the chain
     *         6 - if the player already has the skill
     *         3 - event SkillLearnEvent is cancelled
     *         0 - ok;
     */
    public int characterLearnskill(IActiveCharacter character, ISkill skill, SkillTree skillTree) {
        if (character.getSkillPoints() <= 0) {
            return 1;
        }
        SkillInfo info = skillTree.getSkills().get(skill.getName());
        if (character.getLevel() < info.getMinPlayerLevel()) {
            return 2;
        }
        for (SkillInfo skillInfo : info.getHardDepends()) {
            if (!character.hasSkill(skillInfo.getSkillName())) {
                return 4;
            }
        }
        boolean hasSkill = false;
        for (SkillInfo skillInfo : info.getSoftDepends()) {
            if (character.hasSkill(skillInfo.getSkillName())) {
                hasSkill = true;
                break;
            }
        }
        if (!hasSkill)
            return 5;
        for (SkillInfo skillInfo : info.getConflicts()) {
            if (character.hasSkill(skillInfo.getSkillName()))
                return 6;
        }
        SkillLearnEvent event = new SkillLearnEvent(character, skill);
        game.getEventManager().post(event);
        if (event.isCancelled()) {
            return 3;
        }

        short s = character.getCharacterBase().getSkillPoints();
        character.getCharacterBase().setSkillPoints((short) (s - 1));
        character.getCharacterBase().setUsedSkillPoints((short) (s + 1));

        skill.skillLearn(character);

        ExtendedSkillInfo einfo = new ExtendedSkillInfo();
        einfo.setLevel(1);
        einfo.setSkill(skill);
        einfo.setSkillInfo(skillTree.getSkills().get(skill.getName()));
        character.addSkill(skill.getName().toLowerCase(), einfo);

        putInSaveQueue(character.getCharacterBase());
        return 0;
    }

    /**
     * @param character
     * @param skill
     * @param nClass
     * @return 1 - if character has not a single skillpoint in the skill
     * 2 - if one or more skills are on a path in a skilltree after the skill.
     * 3 - SkillRefundEvent was cancelled
     * 0 - ok
     */
    public int refundSkill(IActiveCharacter character, ISkill skill, NClass nClass) {
        ExtendedSkillInfo skillInfo = character.getSkillInfo(skill);
        if (skillInfo == null)
            return 1;
        SkillTree skillTree = nClass.getSkillTree();
        SkillInfo info = skillTree.getSkills().get(skill.getName());
        for (SkillInfo info1 : info.getDepending()) {
            ExtendedSkillInfo e = character.getSkill(info1.getSkillName());
            if (e != null)
                return 2;
        }
        CancellableEvent event = new SkillRefundEvent(character, skill);
        game.getEventManager().post(event);
        if (event.isCancelled()) {
            return 3;
        }
        int level = skillInfo.getLevel();
        skill.skillRefund(character);
        short skillPoints = character.getCharacterBase().getSkillPoints();
        character.getCharacterBase().setSkillPoints((short) (skillPoints + level));
        character.getCharacterBase().setUsedSkillPoints((short) (skillPoints - level));
        putInSaveQueue(character.getCharacterBase());
        return 0;
    }

    /**
     * Resets character's skilltrees, and gives back all allocated skillpoints.
     *
     * @param character to be reseted
     * @param force
     * @return 1 - if character cant be reseted or force argument is false
     *         0 - ok;
     */
    public int characterResetSkills(IActiveCharacter character, boolean force) {
        CharacterBase characterBase = character.getCharacterBase();
        if (characterBase.isCanResetskills() || force) {
            characterBase.setCanResetskills(false);
            characterBase.setLastReset(new Date(System.currentTimeMillis()));
            characterBase.getSkills().clear();
            character.getRemoveAllSkills();
            characterBase.getCooldowns().clear();
            short usedSkillPoints = characterBase.getUsedSkillPoints();
            characterBase.setUsedAttributePoints((short) 0);
            characterBase.setSkillPoints((short) (characterBase.getSkillPoints() + usedSkillPoints));
            putInSaveQueue(characterBase);
            return 0;
        }
        return 1;
    }

    public void characterSetMaxHealth(IActiveCharacter character, float newHealht) {
        double health = character.getHealth().getValue();
        double max = character.getHealth().getMaxValue();
        double percent = Utils.getPercentage(health,max);
        character.getHealth().setMaxValue(newHealht);
        character.getHealth().setValue(newHealht/percent);
    }

    public void updateWalkSpeed(IActiveCharacter character) {
        double speed = character.getCharacterProperty(DefaultProperties.walk_speed);
        character.setWalkSpeed(speed);
        AttributeData attributeData  = character.getPlayer().get(AttributeData.class).get();
        attributeData.attributes().base(Attributes.GENERIC_MOVEMENT_SPEED).set(speed);
        character.getPlayer().offer(attributeData);
    }

    public void characterAddPoints(IActiveCharacter character, short skillpoint, short attributepoint) {
        character.setSkillPoints((short) (character.getSkillPoints()+skillpoint));
        character.setAttributePoints((short) (character.getAttributePoints()+attributepoint));
        putInSaveQueue(character.getCharacterBase());
    }
}
