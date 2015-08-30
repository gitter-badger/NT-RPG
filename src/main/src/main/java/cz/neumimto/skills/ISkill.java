package cz.neumimto.skills;

import cz.neumimto.players.IActiveCharacter;

import java.util.Set;

/**
 * Created by NeumimTo on 1.1.2015.
 */
public interface ISkill {

    String getName();

    void init();

    void setName(String name);

    void skillLearn(IActiveCharacter character);

    void skillUpgrade(IActiveCharacter character, int level);

    void skillRefund(IActiveCharacter character);

    SkillSettings getDefaultSkillSettings();

    void onCharacterInit(IActiveCharacter c, int level);

    SkillResult onPreUse(IActiveCharacter character);

    Set<SkillType> getSkillTypes();

    SkillSettings getSettings();

    void setSettings(SkillSettings settings);

    public String getDescription();

    public void setDescription(String description);

    public boolean showsToPlayers();
}
