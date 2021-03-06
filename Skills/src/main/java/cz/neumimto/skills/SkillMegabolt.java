package cz.neumimto.skills;

import cz.neumimto.rpg.ResourceLoader;
import cz.neumimto.rpg.damage.SkillDamageSource;
import cz.neumimto.rpg.damage.SkillDamageSourceBuilder;
import cz.neumimto.rpg.players.IActiveCharacter;
import cz.neumimto.rpg.skills.*;
import cz.neumimto.rpg.utils.Utils;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.Living;

import java.util.Set;

/**
 * Created by NeumimTo on 29.12.2015.
 */
@ResourceLoader.Skill
public class SkillMegabolt extends ActiveSkill {

    public SkillMegabolt() {
        setName("Megabolt");
        setDamageType(NDamageType.LIGHTNING);
        SkillSettings settings = new SkillSettings();
        settings.addNode(SkillNodes.DAMAGE,10,10);
        settings.addNode(SkillNodes.RADIUS, 30, 30);
        super.settings = settings;
    }

    @Override
    public SkillResult cast(IActiveCharacter iActiveCharacter, ExtendedSkillInfo extendedSkillInfo,SkillModifier skillModifier) {
        int r = (int) settings.getLevelNodeValue(SkillNodes.RADIUS,extendedSkillInfo.getLevel());
        Set<Entity> nearbyEntities = Utils.getNearbyEntities(iActiveCharacter.getPlayer().getLocation(), r);
        float damage = settings.getLevelNodeValue(SkillNodes.DAMAGE,extendedSkillInfo.getLevel());
        SkillDamageSourceBuilder builder = new SkillDamageSourceBuilder();
        builder.setSkill(this);
        builder.setCaster(iActiveCharacter);
        builder.type(getDamageType());
        SkillDamageSource src = builder.build();
        for (Entity e : nearbyEntities) {
            if (Utils.isLivingEntity(e)) {
                Living l = (Living) e;
                l.damage(damage,src);
                l.getLocation().getExtent().createEntity(EntityTypes.LIGHTNING,l.getLocation().getPosition());
            }
        }
        return SkillResult.OK;
    }
}
