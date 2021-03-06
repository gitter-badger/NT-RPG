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

package cz.neumimto.rpg.listeners;

import cz.neumimto.core.ioc.Inject;
import cz.neumimto.rpg.IEntity;
import cz.neumimto.rpg.IEntityType;
import cz.neumimto.rpg.ResourceLoader;
import cz.neumimto.rpg.configuration.PluginConfig;
import cz.neumimto.rpg.damage.DamageService;
import cz.neumimto.rpg.damage.ISkillDamageSource;
import cz.neumimto.rpg.effects.EffectService;
import cz.neumimto.rpg.entities.EntityService;
import cz.neumimto.rpg.inventory.InventoryService;
import cz.neumimto.rpg.players.CharacterService;
import cz.neumimto.rpg.players.IActiveCharacter;
import cz.neumimto.rpg.skills.ISkill;
import cz.neumimto.rpg.skills.ProjectileProperties;
import cz.neumimto.rpg.skills.SkillService;
import cz.neumimto.rpg.utils.ItemStackUtils;
import cz.neumimto.rpg.utils.Utils;
import org.spongepowered.api.Game;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.entity.damage.DamageModifier;
import org.spongepowered.api.event.cause.entity.damage.DamageModifierTypes;
import org.spongepowered.api.event.cause.entity.damage.DamageType;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.IndirectEntityDamageSource;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.entity.Hotbar;

import java.util.Optional;

/**
 * Created by NeumimTo on 12.2.2015.
 */
@ResourceLoader.ListenerClass
public class BasicListener {

	@Inject
	private CharacterService characterService;

	@Inject
	private Game game;

	@Inject
	private InventoryService inventoryService;

	@Inject
	private EffectService effectService;

	@Inject
	private DamageService damageService;

	@Inject
	private EntityService entityService;

	@Inject
	private SkillService skillService;

	@Listener(order = Order.BEFORE_POST)
	public void onAttack(InteractEntityEvent.Primary event) {
		if (event.isCancelled())
			return;
		if (!Utils.isLivingEntity(event.getTargetEntity()))
			return;
		Optional<Player> first = event.getCause().first(Player.class);
		IActiveCharacter character = null;
		if (first.isPresent()) {
			character = characterService.getCharacter(first.get().getUniqueId());
			if (character.isStub())
				return;
			Hotbar query = first.get().getInventory().query(Hotbar.class);
			inventoryService.onLeftClick(character, query.getSelectedSlotIndex());
		}

		IEntity entity = entityService.get(event.getTargetEntity());

		if (entity.getType() == IEntityType.CHARACTER) {
			IActiveCharacter target = characterService.getCharacter(event.getTargetEntity().getUniqueId());
			if (target.isStub() && !PluginConfig.ALLOW_COMBAT_FOR_CHARACTERLESS_PLAYERS) {
				event.setCancelled(true);
				return;
			}
			if (first.isPresent()) {
				if (character.getParty() == target.getParty() && !character.getParty().isFriendlyfire()) {
					event.setCancelled(true);
				}
			}
		}
	}

	@Listener
	public void onRightClick(InteractEntityEvent.Secondary event, @First(typeFilter = Player.class) Player pl) {

		Optional<ItemStack> itemInHand = pl.getItemInHand(HandTypes.MAIN_HAND);
		if (itemInHand.isPresent()) {
			ItemStack itemStack = itemInHand.get();
			IActiveCharacter character = characterService.getCharacter(pl.getUniqueId());
			if (ItemStackUtils.any_armor.contains(itemStack.getItem())) {
				event.setCancelled(true); //restrict armor equip on rightclick
			} else {
				if (character.isStub())
					return;
				inventoryService.onRightClick(character, 0);
			}
		}

	}

	@Listener
	public void onBlockClick(InteractBlockEvent.Primary event) {
		Optional<Player> first = event.getCause().first(Player.class);
		if (first.isPresent()) {
			Player pl = first.get();
			IActiveCharacter character = characterService.getCharacter(pl.getUniqueId());
			if (character.isStub())
				return;
			Hotbar h = pl.getInventory().query(Hotbar.class);
			inventoryService.onLeftClick(character, h.getSelectedSlotIndex());
		}
	}

	@Listener
	public void onBlockRightClick(InteractBlockEvent.Secondary event, @First(typeFilter = Player.class) Player pl) {

		IActiveCharacter character = characterService.getCharacter(pl.getUniqueId());
		Optional<ItemStack> itemInHand = pl.getItemInHand(HandTypes.MAIN_HAND);
		if (itemInHand.isPresent() && ItemStackUtils.any_armor.contains(itemInHand.get().getItem())) {
			event.setCancelled(true); //restrict armor equip on rightclick
		}
		if (character.isStub())
			return;
		Hotbar h = pl.getInventory().query(Hotbar.class);
		inventoryService.onRightClick(character, h.getSelectedSlotIndex());
	}

/*
    @Listener
    public void onChunkDespawn(UnloadChunkEvent event) {
        entityService.remove(event.getTargetChunk().getEntities(Utils::isLivingEntity));
    }
*/


	@Listener
	public void onPreDamage(DamageEntityEvent event) {
		final Cause cause = event.getCause();
		Optional<EntityDamageSource> first = cause.first(EntityDamageSource.class);
		if (first.isPresent()) {
			Entity targetEntity = event.getTargetEntity();
			EntityDamageSource entityDamageSource = first.get();
			Entity source = entityDamageSource.getSource();
			if (source.get(Keys.HEALTH).isPresent()) {
				targetEntity.offer(Keys.INVULNERABILITY_TICKS, 0);
				//attacker
				IEntity entity = entityService.get(source);
				double newdamage = 0;
				if (entity.getType() == IEntityType.CHARACTER) {
					IActiveCharacter character = (IActiveCharacter) entity;
					newdamage = character.getWeaponDamage();
					newdamage *= damageService.getCharacterBonusDamage(character, entityDamageSource.getType());
				} else {
					if (!PluginConfig.OVERRIDE_MOBS) {
						newdamage = entityService.getMobDamage(source.getType());
					}
				}
				//defende
		        /*
                if (targetEntity.getType() == EntityTypes.PLAYER) {
                    IActiveCharacter tcharacter = characterService.getCharacter(targetEntity.getUniqueId());
                    double armor = tcharacter.getArmorValue();
                    final double damagefactor = damageService.DamageArmorReductionFactor.apply(newdamage, armor);
                    PlayerCombatEvent ce = new PlayerCombatEvent(character, tcharacter, damage, damagefactor);
                    event.setBaseDamage(ce.getDamage());
                    event.setDamage(DamageModifier.builder().cause(Cause.ofNullable(null)).type(DamageModifierTypes.ARMOR).build(), input -> input * ce.getDamagefactor());
                }*/
				event.setBaseDamage(newdamage);
			}
			Optional<IndirectEntityDamageSource> q = event.getCause().first(IndirectEntityDamageSource.class);
			if (q.isPresent()) {
				IndirectEntityDamageSource indirectEntityDamageSource = q.get();
				if (indirectEntityDamageSource.getSource() instanceof Projectile) {
					Projectile projectile = (Projectile) indirectEntityDamageSource.getSource();
					IEntity shooter = entityService.get((Entity) projectile.getShooter());
					IEntity target = entityService.get(targetEntity);
					ProjectileProperties projectileProperties = ProjectileProperties.cache.get(projectile);
					if (projectileProperties != null) {
						ProjectileProperties.cache.remove(projectile);
						projectileProperties.consumer.accept(shooter, target);
					}
				}
			}
			Optional<ISkillDamageSource> skilldamage = cause.first(ISkillDamageSource.class);
			if (skilldamage.isPresent()) {
				ISkillDamageSource iSkillDamageSource = skilldamage.get();
				IActiveCharacter caster = iSkillDamageSource.getCaster();
				ISkill skill = iSkillDamageSource.getSkill();
				DamageType type = skill.getDamageType();

				if (caster.hasPreferedDamageType()) {
					type = caster.getDamageType();
				}
				double finalDamage = damageService.getSkillDamage(caster, skill.getDamageType()) * damageService.getCharacterBonusDamage(caster, type);
				event.setBaseDamage(finalDamage);
				if (event.getTargetEntity().getType() == EntityTypes.PLAYER) {
					IActiveCharacter targetchar = characterService.getCharacter(event.getTargetEntity().getUniqueId());
					double target_resistence = damageService.getCharacterResistance(targetchar, type);
					event.setDamage(DamageModifier.builder().type(DamageModifierTypes.MAGIC).build(), input -> input * target_resistence);
				}
			}
		}
	}

	@Listener
	public void onRespawn(RespawnPlayerEvent event) {
		Entity type = event.getTargetEntity();
		if (type.getType() == EntityTypes.PLAYER) {
			IActiveCharacter character = characterService.getCharacter(type.getUniqueId());
			if (character.isStub())
				return;
			characterService.respawnCharacter(character, event.getTargetEntity());
		}
	}
}
