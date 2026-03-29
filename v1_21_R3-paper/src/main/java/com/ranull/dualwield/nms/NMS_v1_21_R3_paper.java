package com.ranull.dualwield.nms;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class NMS_v1_21_R3_paper implements NMS {

    @Override
    public void handAnimation(Player player, org.bukkit.inventory.EquipmentSlot equipmentSlot) {
        switch (equipmentSlot) {
            case HAND:
                player.swingMainHand();
                break;
            case OFF_HAND:
                player.swingOffHand();
                break;
            default:
                break;
        }
    }

    @Override
    public void blockBreakAnimation(Player player, org.bukkit.block.Block block, int animationID, int stage) {
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        ServerGamePacketListenerImpl connection = serverPlayer.connection;
        BlockPos blockPos = new BlockPos(block.getX(), block.getY(), block.getZ());
        connection.send(new ClientboundBlockDestructionPacket(animationID, blockPos, stage));
    }

    @Override
    public void blockCrackParticle(org.bukkit.block.Block block) {
        block.getWorld().spawnParticle(
                org.bukkit.Particle.BLOCK,
                block.getLocation().add(0.5, 0, 0.5),
                10,
                block.getBlockData()
        );
    }

    @Override
    public float getToolStrength(org.bukkit.block.Block block, org.bukkit.inventory.ItemStack itemStack) {
        if (itemStack.getAmount() != 0) {
            ItemStack nmsStack = CraftItemStack.asNMSCopy(itemStack);
            ServerLevel serverLevel = ((CraftWorld) block.getWorld()).getHandle();
            Block nmsBlock = serverLevel.getBlockState(
                    new BlockPos(block.getX(), block.getY(), block.getZ())
            ).getBlock();
            return nmsStack.getDestroySpeed(nmsBlock.defaultBlockState());
        }
        return 1;
    }

    @Override
    public double getAttackDamage(org.bukkit.inventory.ItemStack itemStack) {
        return getItemStackAttribute(itemStack, Attribute.ATTACK_DAMAGE);
    }

    @Override
    public double getAttackSpeed(org.bukkit.inventory.ItemStack itemStack) {
        return getItemStackAttribute(itemStack, Attribute.ATTACK_SPEED);
    }

    private double getItemStackAttribute(org.bukkit.inventory.ItemStack itemStack, Attribute attribute) {
        if (itemStack.getAmount() != 0) {
            org.bukkit.inventory.EquipmentSlot slot = itemStack.getType().getEquipmentSlot();
            for (org.bukkit.attribute.AttributeModifier modifier :
                    itemStack.getType().getDefaultAttributeModifiers(slot).values()) {
                if (attribute == Attribute.ATTACK_DAMAGE || attribute == Attribute.ATTACK_SPEED) {
                    return modifier.getAmount();
                }
            }
        }
        return 0;
    }

    @Override
    public Sound getHitSound(org.bukkit.block.Block block) {
        try {
            ServerLevel serverLevel = ((CraftWorld) block.getWorld()).getHandle();
            Block nmsBlock = serverLevel.getBlockState(
                    new BlockPos(block.getX(), block.getY(), block.getZ())
            ).getBlock();
            SoundType soundType = nmsBlock.defaultBlockState().getSoundType();
            return Sound.valueOf(
                    soundType.getHitSound().location().getPath()
                            .toUpperCase()
                            .replace(".", "_")
            );
        } catch (IllegalArgumentException ignored) {
        }
        return Sound.BLOCK_STONE_HIT;
    }

    @Override
    public float getBlockHardness(org.bukkit.block.Block block) {
        ServerLevel serverLevel = ((CraftWorld) block.getWorld()).getHandle();
        Block nmsBlock = serverLevel.getBlockState(
                new BlockPos(block.getX(), block.getY(), block.getZ())
        ).getBlock();
        return nmsBlock.defaultBlockState().getDestroySpeed(null, null);
    }

    @Override
    public boolean breakBlock(Player player, org.bukkit.block.Block block) {
        return player.breakBlock(block);
    }

    @Override
    public void setModifier(Player player, double damage, double speed, UUID uuid) {
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        AttributeInstance damageAttr = serverPlayer.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance speedAttr = serverPlayer.getAttribute(Attributes.ATTACK_SPEED);

        if (damageAttr != null) {
            damageAttr.addTransientModifier(new AttributeModifier(
                    ResourceLocation.tryParse("dualwield:weapon_modifier"),
                    damage,
                    AttributeModifier.Operation.ADD_VALUE
            ));
        }
        if (speedAttr != null) {
            speedAttr.addTransientModifier(new AttributeModifier(
                    ResourceLocation.tryParse("dualwield:weapon_modifier"),
                    speed,
                    AttributeModifier.Operation.ADD_VALUE
            ));
        }
    }

    @Override
    public void removeModifier(Player player, UUID uuid) {
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        AttributeInstance damageAttr = serverPlayer.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance speedAttr = serverPlayer.getAttribute(Attributes.ATTACK_SPEED);

        if (damageAttr != null) {
            damageAttr.removeModifiers();
        }
        if (speedAttr != null) {
            speedAttr.removeModifiers();
        }
    }

    @Override
    public void attack(Player player, org.bukkit.entity.Entity entity) {
        ((CraftPlayer) player).getHandle().attack(((CraftEntity) entity).getHandle());
    }
}