/*    
 *     Copyright (c) 2015, NeumimTo https://github.com/NeumimTo
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *     
 */

package cz.neumimto.damage;

import cz.neumimto.ioc.Inject;
import cz.neumimto.ioc.PostProcess;
import cz.neumimto.ioc.Singleton;
import cz.neumimto.players.IActiveCharacter;
import cz.neumimto.players.properties.DefaultProperties;
import cz.neumimto.skills.ISkill;
import cz.neumimto.skills.NDamageType;
import cz.neumimto.utils.ItemStackUtils;
import org.hibernate.cfg.annotations.reflection.XMLContext;
import org.spongepowered.api.Game;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.entity.damage.DamageModifierBuilder;
import org.spongepowered.api.event.cause.entity.damage.DamageType;
import org.spongepowered.api.event.cause.entity.damage.DamageTypes;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSourceBuilder;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSources;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.DoubleBinaryOperator;

/**
 * Created by NeumimTo on 4.8.15.
 */
@Singleton
public class DamageService {

    public BiFunction<Double,Double,Double> DamageArmorReductionFactor = (damage,armor) -> armor/(armor+10*damage);

    private Map<ItemType, Short> map = new HashMap<>();

    public double getCharacterItemDamage(IActiveCharacter character, ItemType type) {
        if (character.isStub())
            return 1;
        double base = character.getBaseWeaponDamage(type) + character.getCharacterProperty(DefaultProperties.weapon_damage_bonus);
        base += character.getCharacterProperty(map.get(type));
        if (ItemStackUtils.isSword(type)) {
            base *= character.getCharacterProperty(DefaultProperties.swords_damage_mult);
        } else if (ItemStackUtils.isAxe(type)) {
            base *= character.getCharacterProperty(DefaultProperties.axes_damage_mult);
        } else if (ItemStackUtils.isPickaxe(type)) {
            base *= character.getCharacterProperty(DefaultProperties.pickaxes_damage_mult);
        } else if (ItemStackUtils.isHoe(type)) {
            base *= character.getCharacterProperty(DefaultProperties.hoes_damage_mult);
        } else if (ItemStackUtils.isBow(type)) {
            base *= character.getCharacterProperty(DefaultProperties.bows_meele_damage_mult);
        } else if (ItemStackUtils.isStaff(type)) {
            base *= character.getCharacterProperty(DefaultProperties.staffs_damage_mult);
        }
        return base;
    }

    public void recalculateCharacterWeaponDamage(IActiveCharacter character) {
        double damage = getCharacterItemDamage(character, character.getMainHand().getItemType());
        damage += character.getMainHand().getDamage() + character.getOffHand().getDamage();
        character.setWeaponDamage(damage);
    }

    public double getCharacterResistance(IActiveCharacter character, DamageType source) {
        if (source == DamageTypes.ATTACK)
            return character.getCharacterProperty(DefaultProperties.physical_damage_protection_mult);
        if (source == DamageTypes.FIRE)
            return character.getCharacterProperty(DefaultProperties.fire_damage_protection_mult);
        if (source == DamageTypes.MAGIC)
            return character.getCharacterProperty(DefaultProperties.magic_damage_protection_mult);
        if (source == NDamageType.LIGHTNING)
            return character.getCharacterProperty(DefaultProperties.lightning_damage_protection_mult);
        if (source == NDamageType.ICE)
            return character.getCharacterProperty(DefaultProperties.ice_damage_protection_mult);
        return 1;
    }

    public double getCharacterProjectileDamage(IActiveCharacter character, ProjectileType type) {
        if (character.isStub())
            return 0;
        return 20;
    }

    public void skillDamage(IActiveCharacter caster,double damage,ISkill skill, Living entity) {
        Living l;
    }


    public double getCharacterBonusDamage(IActiveCharacter character, DamageType source) {
        if (source == DamageTypes.ATTACK)
            return character.getCharacterProperty(DefaultProperties.physical_damage_bonus_mult);
        if (source == DamageTypes.FIRE)
            return character.getCharacterProperty(DefaultProperties.fire_damage_bonus_mult);
        if (source == DamageTypes.MAGIC)
            return character.getCharacterProperty(DefaultProperties.magic_damage_bonus_mult);
        if (source == NDamageType.LIGHTNING)
            return character.getCharacterProperty(DefaultProperties.lightning_damage_bonus_mult);
        if (source == NDamageType.ICE)
            return character.getCharacterProperty(DefaultProperties.ice_damage_bonus_mult);
        return 0;
    }

    @PostProcess(priority = 6)
    public void buildPropertiesMap() {
        map.put(ItemTypes.DIAMOND_SWORD, DefaultProperties.diamond_sword_bonus_damage);
        map.put(ItemTypes.GOLDEN_SWORD, DefaultProperties.golden_sword_bonus_damage);
        map.put(ItemTypes.IRON_SWORD, DefaultProperties.iron_sword_bonus_damage);
        map.put(ItemTypes.WOODEN_SWORD, DefaultProperties.wooden_sword_bonus_damage);

        map.put(ItemTypes.DIAMOND_AXE, DefaultProperties.diamond_axe_bonus_damage);
        map.put(ItemTypes.GOLDEN_AXE, DefaultProperties.golden_axe_bonus_damage);
        map.put(ItemTypes.IRON_AXE, DefaultProperties.iron_axe_bonus_damage);
        map.put(ItemTypes.WOODEN_AXE, DefaultProperties.wooden_axe_bonus_damage);

        map.put(ItemTypes.DIAMOND_PICKAXE, DefaultProperties.diamond_pickaxe_bonus_damage);
        map.put(ItemTypes.GOLDEN_PICKAXE, DefaultProperties.golden_pickaxe_bonus_damage);
        map.put(ItemTypes.IRON_PICKAXE, DefaultProperties.iron_pickaxe_bonus_damage);
        map.put(ItemTypes.WOODEN_PICKAXE, DefaultProperties.wooden_pickaxe_bonus_damage);


        map.put(ItemTypes.DIAMOND_HOE, DefaultProperties.diamond_hoe_bonus_damage);
        map.put(ItemTypes.GOLDEN_HOE, DefaultProperties.golden_hoe_bonus_damage);
        map.put(ItemTypes.IRON_HOE, DefaultProperties.iron_hoe_bonus_damage);
        map.put(ItemTypes.WOODEN_HOE, DefaultProperties.wooden_hoe_bonus_damage);

        map.put(ItemTypes.BOW, DefaultProperties.bow_meele_bonus_damage);
    }


}