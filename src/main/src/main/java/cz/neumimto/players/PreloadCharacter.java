package cz.neumimto.players;

import cz.neumimto.configuration.Settings;
import cz.neumimto.effects.IEffect;
import cz.neumimto.players.groups.Guild;
import cz.neumimto.players.groups.NClass;
import cz.neumimto.players.groups.Race;
import cz.neumimto.players.parties.Party;
import cz.neumimto.players.properties.DefaultProperties;
import cz.neumimto.skills.ExtendedSkillInfo;
import cz.neumimto.skills.ISkill;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.potion.PotionEffect;
import org.spongepowered.api.potion.PotionEffectType;

import java.util.*;

/**
 * Created by NeumimTo on 23.7.2015.
 */
public class PreloadCharacter implements IActiveCharacter {

    IReservable mana = new Mana(this);
    static float[] characterProperties = new float[Settings.CHARACTER_PROPERTIES];
    UUID uuid;
    long time = System.currentTimeMillis();
    Health health = new HealthStub(this);

    public PreloadCharacter(UUID uuid) {
        this.uuid = uuid;
        mana.setMaxValue(0);
    }

    @Override
    public void setCharacterLevelProperty(int index, float value) {

    }

    @Override
    public float getCharacterPropertyWithoutLevel(int index) {
        return 0;
    }

    @Override
    public double getBaseWeaponDamage(ItemType id) {
        return 0;
    }

    @Override
    public String getName() {
        return "PreloadChar";
    }

    @Override
    public int getLevel() {
        return 0;
    }

    @Override
    public boolean isStub() {
        return true;
    }

    @Override
    public float[] getCharacterProperties() {
        return characterProperties;
    }

    @Override
    public float getCharacterProperty(int index) {
        if (index == DefaultProperties.walk_speed) { //lets player move around even without character
            return 0.2f;
        }
        return 0;
    }

    @Override
    public void setCharacterProperty(int index, float value) {

    }

    @Override
    public double getMaxMana() {
        return 0;
    }

    @Override
    public void setMaxMana(float mana) {

    }

    @Override
    public void setMaxHealth(float maxHealth) {

    }

    @Override
    public void setHealth(float mana) {

    }

    @Override
    public IReservable getMana() {
        return mana;
    }

    @Override
    public void setMana(IReservable mana) {

    }

    @Override
    public Health getHealth() {
        return health;
    }

    @Override
    public void setHealth(Health health) {

    }

    @Override
    public double getExperiencs() {
        return 0;
    }

    @Override
    public void addExperiences(float exp) {

    }

    @Override
    public Player getPlayer() {
        return null;
    }

    @Override
    public void setPlayer(Player pl) {

    }

    @Override
    public void onRightClickBlock(int slotId) {

    }

    @Override
    public void resetRightClicks() {

    }

    @Override
    public short getSkillPoints() {
        return 0;
    }

    @Override
    public void setSkillPoints(short skillPoints) {

    }

    @Override
    public short getAttributePoints() {
        return 0;
    }

    @Override
    public void setAttributePoints(short attributePoints) {

    }

    @Override
    public ExtendedNClass getPrimaryClass() {
        return ExtendedNClass.Default;
    }

    @Override
    public void setPrimaryClass(NClass clazz) {

    }

    @Override
    public Map<String, Long> getCooldowns() {
        return Collections.EMPTY_MAP;
    }

    @Override
    public boolean hasCooldown(String thing) {
        return true;
    }


    @Override
    public Set<ItemType> getAllowedArmor() {
        return Collections.emptySet();
    }

    @Override
    public boolean canWear(ItemStack armor) {
        return false;
    }

    @Override
    public Map<ItemType, Double> getAllowedWeapons() {
        return Collections.EMPTY_MAP;
    }

    @Override
    public Set<ExtendedNClass> getClasses() {
        return Collections.EMPTY_SET;
    }

    @Override
    public NClass getNClass(int index) {
        return NClass.Default;
    }

    @Override
    public Race getRace() {
        return Race.Default;
    }

    @Override
    public void setRace(Race race) {

    }

    @Override
    public Guild getGuild() {
        return Guild.Default;
    }

    @Override
    public void setGuild(Guild guild) {

    }

    @Override
    public IActiveCharacter updateItemRestrictions() {
        return this;
    }

    @Override
    public CharacterBase getCharacterBase() {
        return new CharacterBase();
    }

    @Override
    public void setClass(NClass nclass, int slot) {

    }

    @Override
    public Collection<IEffect> getEffects() {
        return Collections.EMPTY_SET;
    }

    @Override
    public IEffect getEffect(Class<? extends IEffect> cl) {
        return null;
    }

    @Override
    public boolean hasEffect(Class<? extends IEffect> cl) {
        return false;
    }

    @Override
    public void addEffect(IEffect effect) {

    }

    @Override
    public void removeEffect(Class<? extends IEffect> cl) {

    }

    @Override
    public void addPotionEffect(PotionEffectType p, int amplifier, long duration) {

    }

    @Override
    public void addPotionEffect(PotionEffectType p, int amplifier, long duration, boolean partciles) {

    }

    @Override
    public void removePotionEffect(PotionEffectType type) {

    }

    @Override
    public boolean hasPotionEffect(PotionEffectType type) {
        return false;
    }

    @Override
    public void removeAllTempEffects() {

    }

    @Override
    public void addPotionEffect(PotionEffect e) {

    }

    @Override
    public void sendMessage(String message) {

    }

    @Override
    public Map<String, ExtendedSkillInfo> getSkills() {
        return Collections.EMPTY_MAP;
    }

    @Override
    public ExtendedSkillInfo getSkillInfo(ISkill skill) {
        return ExtendedSkillInfo.Empty;
    }

    @Override
    public boolean hasSkill(String name) {
        return false;
    }

    @Override
    public ExtendedSkillInfo getSkillInfo(String s) {
        return ExtendedSkillInfo.Empty;
    }

    @Override
    public boolean isSilenced() {
        return true;
    }

    @Override
    public void addSkill(String name, ExtendedSkillInfo info) {

    }

    @Override
    public ExtendedSkillInfo getSkill(String skillName) {
        return ExtendedSkillInfo.Empty;
    }

    @Override
    public void getRemoveAllSkills() {

    }

    @Override
    public boolean hasParty() {
        return false;
    }

    @Override
    public boolean isInPartyWith(IActiveCharacter character) {
        return false;
    }

    @Override
    public void setWalkSpeed(double speed) {

    }

    @Override
    public Party getParty() {
        return null;
    }

    @Override
    public void setParty(Party party) {

    }
}
