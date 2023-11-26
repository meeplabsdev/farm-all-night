package meeplabsdev.farmutils;

import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldView;

import java.util.Arrays;

public class CropHelper {
    public static final Item[] SEEDS = {
            Items.WHEAT_SEEDS,
            Items.CARROT,
            Items.POTATO,
            Items.BEETROOT_SEEDS,
            Items.COCOA_BEANS,
            Items.MELON_SEEDS,
            Items.PUMPKIN_SEEDS,
            Items.SWEET_BERRIES,
            Items.NETHER_WART,
    };

    public static boolean till(BlockPos pos, Block block) {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        boolean moist = isWaterNearby(minecraft.world, pos);
        boolean tillable = block == Blocks.GRASS_BLOCK ||
                block == Blocks.DIRT_PATH ||
                block == Blocks.DIRT ||
                block == Blocks.COARSE_DIRT ||
                block == Blocks.ROOTED_DIRT;
        if (moist && tillable && minecraft.world.getBlockState(pos.up()).isAir()) {
            Integer hoeSlot = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof HoeItem);
            return InvUtils.interact(pos, hoeSlot);
        }
        return false;
    }

    public static void harvest(BlockPos pos, BlockState state, Block block) {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        if (!isMature(state, block)) return;
        if (block instanceof SweetBerryBushBlock)
            minecraft.interactionManager.interactBlock(minecraft.player, Hand.MAIN_HAND, new BlockHitResult(pos.toCenterPos(), Direction.UP, pos, false));
        else {
            for (int i = 0; i < 5; i++) {
                minecraft.interactionManager.updateBlockBreakingProgress(pos, Direction.UP);
            }
        }
    }

    public static void plant(BlockPos pos, Block block) {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        Integer plantSlot = -1;
        if (!minecraft.world.isAir(pos.up())) return;
        if (block instanceof FarmlandBlock) {
            plantSlot = InvUtils.findInHotbar(itemStack -> itemStack.getItem() != Items.NETHER_WART && Arrays.stream(SEEDS).toList().contains(itemStack.getItem()));
        } else if (block instanceof SoulSandBlock) {
            plantSlot = InvUtils.findInHotbar(itemStack -> itemStack.getItem() == Items.NETHER_WART);
        }

        if (plantSlot != -1) {
            place(pos.up(), plantSlot);
        }
    }

    public static void place(BlockPos blockPos, int slot) {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        if (slot < 0 || slot > 8) return;

        int prevSlot = minecraft.player.getInventory().selectedSlot;
        InvUtils.swap(slot);
        InvUtils.interact(blockPos, slot);
        minecraft.player.getInventory().selectedSlot = prevSlot;
    }

    public static boolean isWaterNearby(WorldView world, BlockPos pos) {
        for (BlockPos blockPos : BlockPos.iterate(pos.add(-4, 0, -4), pos.add(4, 1, 4))) {
            if (world.getFluidState(blockPos).isIn(FluidTags.WATER)) return true;
        }
        return false;
    }

    public static boolean isMature(BlockState state, Block block) {
        if (block instanceof CropBlock cropBlock) {
            return cropBlock.isMature(state);
        } else if (block instanceof CocoaBlock cocoaBlock) {
            return !cocoaBlock.hasRandomTicks(state);
        } else if (block instanceof StemBlock) {
            return state.get(StemBlock.AGE) == StemBlock.MAX_AGE;
        } else if (block instanceof SweetBerryBushBlock sweetBerryBushBlock) {
            return !sweetBerryBushBlock.hasRandomTicks(state);
        } else if (block instanceof NetherWartBlock netherWartBlock) {
            return !netherWartBlock.hasRandomTicks(state);
        }
        return false;
    }

    public static boolean isCrop(Block block) {
        return (block instanceof CropBlock
            || block instanceof CocoaBlock
            || block instanceof StemBlock
            || block instanceof SweetBerryBushBlock
            || block instanceof NetherWartBlock);
    }

    public static boolean isSeed(Item item) {
        return Arrays.stream(SEEDS).toList().contains(item);
    }

    public static int levelSeeds() {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        int max = 0;
        for (Item seed : SEEDS) {
            int num = minecraft.player.getInventory().count(seed);
            if (num > max) max = num;
        }

        return max;
    }
}
