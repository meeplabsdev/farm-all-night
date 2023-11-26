package meeplabsdev.farmutils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.function.Predicate;

public class InvUtils {
    private static void syncSelectedSlot() {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        int i = minecraft.player.getInventory().selectedSlot;
        minecraft.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(i));
    }

    public static boolean swap(int slot) {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        if (slot < 0 || slot > 8) return false;

        minecraft.player.getInventory().selectedSlot = slot;
        syncSelectedSlot();
        return true;
    }

    public static Integer findInHotbar(Predicate<ItemStack> isGood) {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        int slot = -1;

        for (int i = 0; i <= 8; i++) {
            ItemStack stack = minecraft.player.getInventory().getStack(i);
            if (isGood.test(stack)) {
                if (slot == -1) slot = i;
            }
        }

        return slot;
    }

    public static boolean interact(BlockPos pos, Integer itemSlot) {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        if (itemSlot == -1) return false;

        boolean wasSneaking = minecraft.player.input.sneaking;
        minecraft.player.input.sneaking = false;
        int prevSlot = minecraft.player.getInventory().selectedSlot;
        swap(itemSlot);
        minecraft.interactionManager.interactBlock(minecraft.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false));
        minecraft.player.swingHand(Hand.MAIN_HAND);
        minecraft.player.input.sneaking = wasSneaking;
        minecraft.player.getInventory().selectedSlot = prevSlot;

        return true;
    }
}
