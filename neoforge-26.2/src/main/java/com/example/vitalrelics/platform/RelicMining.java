package com.example.vitalrelics.platform;

import com.example.vitalrelics.Utils;
import com.example.vitalrelics.common.*;
import com.example.vitalrelics.common.relics.Loader;
import com.example.vitalrelics.common.relics.Relic;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Native mining/arrow primitives. Selection and durability rules live in common. */
public final class RelicMining {
    private RelicMining() {}
    private static final Set<UUID> EXPANDING = new HashSet<>();
    private static final Map<AbstractArrow, ArrowDurability> ARROWS = new HashMap<>();

    public static void afterBreak(ServerPlayer player, BlockPos position, BlockState original, java.util.function.BooleanSupplier canceled) {
        if (EXPANDING.contains(player.getUUID()) || player.isShiftKeyDown()) return;
        var relics = Utils.gatherRelics(player);
        boolean tree = original.is(BlockTags.LOGS) && Loader.levelOfSuchPassiveSkill(relics, Relic.PASSIVE_SKILL_TREE_FELLER) > 0;
        double range = Loader.levelOfSuchPassiveSkill(relics, Relic.PASSIVE_SKILL_AREA_MINING);
        if (!tree && !(range > 0)) return;
        ServerLevel level = (ServerLevel) player.level();
        BlockPos origin = position.immutable();
        ItemStack tool = player.getMainHandItem();
        boolean hadTool = !tool.isEmpty();
        // BreakEvent is pre-removal. Wait and verify removal, including cancellation by later listeners.
        Scheduler.INSTANCE().addDelayedTask(player.getUUID(), new Scheduler.DelayTask(1, 1, () -> {
            if (canceled.getAsBoolean() || !level.hasChunkAt(origin) || level.getBlockState(origin) == original || player.isRemoved() || !player.isAlive()) return;
            MiningSkills.Context context = new MiningSkills.Context() {
                public boolean valid() {
                    return player.isAlive() && !player.isRemoved() && player.level() == level
                        && player.getMainHandItem() == tool && (!hadTool || !tool.isEmpty());
                }
                public boolean isLog(MiningSkills.Pos pos) {
                    BlockPos nativePos = new BlockPos(pos.x(), pos.y(), pos.z());
                    return level.hasChunkAt(nativePos) && level.getBlockState(nativePos).is(BlockTags.LOGS);
                }
                public boolean breakBlock(MiningSkills.Pos pos) {
                    return valid() && destroy(player, new BlockPos(pos.x(), pos.y(), pos.z()));
                }
            };
            if (!context.valid()) return;
            EXPANDING.add(player.getUUID());
            try { MiningSkills.apply(context, new MiningSkills.Pos(origin.getX(), origin.getY(), origin.getZ()), tree, range); }
            finally { EXPANDING.remove(player.getUUID()); }
        }), level.getServer().getTickCount());
    }

    private static boolean destroy(ServerPlayer player, BlockPos pos) {
        ServerLevel level = (ServerLevel) player.level();
        if (!level.hasChunkAt(pos) || !level.mayInteract(player, pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0) return false;
        // Never synthesize drops: native destruction preserves Fortune, Silk Touch, XP,
        // tool wear, block entities, adventure restrictions, and loader break events.
        return player.gameMode.destroyBlock(pos) && level.getBlockState(pos) != state;
    }

    public static boolean launch(LivingEntity owner, double speed, double durability) {
        if (!(owner instanceof ServerPlayer) || !(owner.level() instanceof ServerLevel level)) return false;
        Arrow arrow = new Arrow(EntityTypes.ARROW, level) {
			@Override
			public void tick() {
				Vec3 from = position();
				super.tick();
				RelicMining.clearFluids(this, from, position());
			}

            @Override
            protected void onHitBlock(BlockHitResult hit) {
                if (!RelicMining.blockImpact(this, hit)) super.onHitBlock(hit);
            }
        };
        arrow.setOwner(owner);
        arrow.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        Vec3 look = owner.getLookAngle();
        arrow.shoot(look.x, look.y, look.z, (float) speed, 0);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        ARROWS.put(arrow, new ArrowDurability(durability));
        // Owner is assigned before joining: normal empowered-arrow hooks run exactly once.
        if (!level.addFreshEntity(arrow)) { ARROWS.remove(arrow); return false; }
        return true;
    }

	private static void clearFluids(AbstractArrow arrow, Vec3 from, Vec3 to) {
		if (!(arrow.getOwner() instanceof ServerPlayer owner) || !(arrow.level() instanceof ServerLevel level)) return;
		Vec3 movement = to.subtract(from);
		if (movement.lengthSqr() <= 1.0E-12) return;
		Vec3 direction = movement.normalize(), cursor = from;
		for (int count = 0; count < 128; count++) {
			BlockHitResult hit = level.clip(new ClipContext(cursor, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, arrow));
			if (hit.getType() != HitResult.Type.BLOCK) return;
			BlockPos pos = hit.getBlockPos();
			BlockState state = level.getBlockState(pos);
			if (!(state.getBlock() instanceof LiquidBlock)) return;
			if (!level.mayInteract(owner, pos)) { arrow.discard(); return; }
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
			cursor = hit.getLocation().add(direction.scale(1.0E-5));
			if (to.subtract(cursor).dot(direction) <= 0.0) return;
		}
		arrow.discard();
	}

    public static void tick() {
        var iterator = ARROWS.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            AbstractArrow arrow = entry.getKey();
            if (arrow.isRemoved() || !(arrow.getOwner() instanceof LivingEntity owner) || !owner.isAlive() || owner.isRemoved()
                    || owner.level() != arrow.level()) {
                arrow.discard(); iterator.remove();
            }
        }
    }

    /** True means suppress the vanilla block impact, including when we discard the arrow. */
    public static boolean blockImpact(AbstractArrow arrow, BlockHitResult firstHit) {
        ArrowDurability durability = ARROWS.get(arrow);
        if (durability == null) return false;
        if (!(arrow.getOwner() instanceof ServerPlayer owner) || owner.level() != arrow.level()) { arrow.discard(); return true; }
        ServerLevel level = (ServerLevel) arrow.level();
        BlockHitResult hit = firstHit;
        Vec3 end = arrow.position().add(arrow.getDeltaMovement());
        Vec3 direction = arrow.getDeltaMovement().normalize();
        for (int count = 0; count < 128; count++) {
            BlockPos pos = hit.getBlockPos();
            if (!level.hasChunkAt(pos)) { arrow.discard(); return true; }
            double hardness = level.getBlockState(pos).getDestroySpeed(level, pos);
            if (!durability.canBreak(hardness) || !destroyBorebolt(owner, pos)) { arrow.discard(); return true; }
            durability.spend(hardness);
            // Recheck the remaining segment: canceling one impact must not tunnel through a second wall.
            Vec3 from = hit.getLocation().add(direction.scale(0.00001));
            if (end.subtract(from).dot(direction) <= 0) return true;
            hit = level.clip(new ClipContext(from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, arrow));
            if (hit.getType() != HitResult.Type.BLOCK) return true;
        }
        arrow.discard(); // Bound zero-hardness traversal as well.
        return true;
    }

    public static void clear() {
        ARROWS.keySet().forEach(AbstractArrow::discard);
        ARROWS.clear(); EXPANDING.clear();
    }

    private static boolean destroyBorebolt(ServerPlayer player, BlockPos pos) {
        ServerLevel level = (ServerLevel) player.level();
        if (!level.hasChunkAt(pos) || !level.mayInteract(player, pos)) return false;
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0) return false;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        state.getBlock().playerWillDestroy(level, pos, state, player);
        if (!level.removeBlock(pos, false)) return false;
        state.getBlock().destroy(level, pos, state);
        // Borebolt has no held tool. Use an unenchanted Netherite Pickaxe only as
        // the vanilla loot context; never modify the player's item or enchantments.
        ItemStack tool = new ItemStack(Items.NETHERITE_PICKAXE);
        if (!state.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(state))
            state.getBlock().playerDestroy(level, player, pos, state, blockEntity, tool);
        return true;
    }
}
