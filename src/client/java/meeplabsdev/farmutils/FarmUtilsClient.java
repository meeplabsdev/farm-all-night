package meeplabsdev.farmutils;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import meeplabsdev.farmutils.arguments.ActionArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class FarmUtilsClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("farmutils");
    private static final ParticleSpawner spawner = new ParticleSpawner();
    private ArrayList<Vec3d> blocksToHarvest = new ArrayList<>();
    private ArrayList<Vec3d> blocksToPlant = new ArrayList<>();

    // Options
    private static boolean running = false;
    private static boolean playSound = true;
    private static boolean shouldPickupItems = true;
    private static boolean logOnInvFull = false;
    private static boolean autoPlant = true;
    private static int harvestRange = 10;
    private static int perTick = 5;
    private static int batchSize = 3;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Loading FarmUtils...");

        ArgumentTypeRegistry.registerArgumentType(new Identifier("farmutils", "action"), ActionArgumentType.class, ConstantArgumentSerializer.of(ActionArgumentType::action));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> {
                dispatcher.register(ClientCommandManager.literal("farmutils")
                        .executes(context -> executeActionCommand(context, "help", 0))
                        .then(ClientCommandManager.literal("maxHarvestsPerTick")
                                .then(ClientCommandManager.argument("number", IntegerArgumentType.integer())
                                        .executes(context -> executeActionCommand(context, "maxHarvestsPerTick", IntegerArgumentType.getInteger(context, "number")))))
                        .then(ClientCommandManager.literal("autoPickupItems")
                                .then(ClientCommandManager.argument("active", BoolArgumentType.bool())
                                        .executes(context -> executeActionCommand(context, "autoPickupItems", BoolArgumentType.getBool(context, "active") ? 1 : 0))))
                        .then(ClientCommandManager.literal("harvestRange")
                                .then(ClientCommandManager.argument("number", IntegerArgumentType.integer())
                                        .executes(context -> executeActionCommand(context, "harvestRange", IntegerArgumentType.getInteger(context, "number")))))
                        .then(ClientCommandManager.literal("batchSize")
                                .then(ClientCommandManager.argument("number", IntegerArgumentType.integer())
                                        .executes(context -> executeActionCommand(context, "batchSize", IntegerArgumentType.getInteger(context, "number")))))
                        .then(ClientCommandManager.literal("harvestSound")
                                .then(ClientCommandManager.argument("active", BoolArgumentType.bool())
                                        .executes(context -> executeActionCommand(context, "harvestSound", BoolArgumentType.getBool(context, "active") ? 1 : 0))))
                        .then(ClientCommandManager.literal("logOnInvFull")
                                .then(ClientCommandManager.argument("active", BoolArgumentType.bool())
                                        .executes(context -> executeActionCommand(context, "logOnInvFull", BoolArgumentType.getBool(context, "active") ? 1 : 0))))
                        .then(ClientCommandManager.literal("autoPlant")
                                .then(ClientCommandManager.argument("active", BoolArgumentType.bool())
                                        .executes(context -> executeActionCommand(context, "autoPlant", BoolArgumentType.getBool(context, "active") ? 1 : 0))))
                        .then(ClientCommandManager.literal("start")
                                .executes(context -> executeActionCommand(context, "running", 1)))
                        .then(ClientCommandManager.literal("stop")
                                .executes(context -> executeActionCommand(context, "running", 0)))
            ); });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.world != null && !client.isPaused()) {
                tick();
            }
        });
    }

    private static int executeActionCommand(CommandContext<FabricClientCommandSource> context, String option, Integer value) {
        switch (option) {
            case "maxHarvestsPerTick" -> perTick = value;
            case "harvestRange" -> harvestRange = value;
            case "batchSize" -> batchSize = value;
            case "autoPickupItems" -> shouldPickupItems = (value == 1);
            case "harvestSound" -> playSound  = (value == 1);
            case "logOnInvFull" -> logOnInvFull  = (value == 1);
            case "autoPlant" -> autoPlant  = (value == 1);
            case "running" -> running  = (value == 1);
        }

        context.getSource().sendFeedback(Text.literal("" +
            "§r§f----------------------------------" + "\n" +
            "§f§lRunning: " + (running ? "§a§l" : "§c§l") + running + "\n"+
            "§r§fAuto-Pickup Items: " + (shouldPickupItems ? "§a" : "§c") + shouldPickupItems + "\n" +
            "§r§fAuto-Plant Seeds: " + (autoPlant ? "§a" : "§c") + autoPlant + "\n" +
            "§r§fSound on Harvest: " + (playSound ? "§a" : "§c") + playSound + "\n"+
            "§r§fLogout on Full Inventory: " + (logOnInvFull ? "§a" : "§c") + logOnInvFull + "\n"+
            "§r§fMaximum Harvest Per Tick: §9" + perTick + "\n" +
            "§r§fHarvesting Range: §9" + harvestRange + "\n" +
            "§r§fBatch Size: §9" + batchSize + "\n" +
            "§r§f----------------------------------"
        ));

        return 1;
    }

    public void tick() {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        if (minecraft.player == null || minecraft.world == null || !running) return;
        Vec3d pos = minecraft.player.getPos();

        for (int x = -harvestRange; x < harvestRange; x++) {
            for (int z = -harvestRange; z < harvestRange; z++) {
                for (int y = -harvestRange; y < harvestRange; y++) {
                    Vec3d targetPos = pos.add(x, y, z);
                    targetPos = new Vec3d((int)(targetPos.x), (int)(targetPos.y), (int)(targetPos.z));
                    targetPos = targetPos.add(0.5, 0.5, 0.5);

                    BlockState blockState = minecraft.world.getBlockState(BlockPos.ofFloored(targetPos));
                    Block block = blockState.getBlock();

                    if (blockState.isOf(Blocks.WHEAT)) {
                        if (CropHelper.isMature(blockState, block)) {
                            spawner.spawnParticle(ParticleTypes.ELECTRIC_SPARK, targetPos.x, targetPos.y + 0.4, targetPos.z, 0, 0, 0);

                            if (!blocksToHarvest.contains(targetPos)) blocksToHarvest.add(targetPos);
                        }
                    } else if (blockState.isOf(Blocks.FARMLAND) && autoPlant) {
                        BlockState cropState = minecraft.world.getBlockState(BlockPos.ofFloored(targetPos.add(0, 1, 0)));
                        if (cropState.isOf(Blocks.AIR)) {
                            spawner.spawnParticle(ParticleTypes.WAX_ON, targetPos.x, targetPos.y + 1.2, targetPos.z, 0, 0, 0);

//                            BlockPos blockPos = new BlockPos((int) targetPos.x, (int) targetPos.y, (int) targetPos.z);
//                            CropHelper.plant(blockPos, block);
                            if (!blocksToPlant.contains(targetPos)) blocksToPlant.add(targetPos);
                        }
                    }
                }
            }
        }

        ArrayList<Vec3d> toRemove = new ArrayList<>();
        for (Vec3d blockPos : blocksToHarvest) {
            BlockState blockState = minecraft.world.getBlockState(BlockPos.ofFloored(blockPos));
            Block block = blockState.getBlock();
            if (!CropHelper.isMature(blockState, block)) {
                toRemove.add(blockPos);
            }
        }
        for (Vec3d blockPos : toRemove) blocksToHarvest.remove(blockPos);

        toRemove = new ArrayList<>();
        for (Vec3d blockPos : blocksToPlant) {
            BlockState blockState = minecraft.world.getBlockState(BlockPos.ofFloored(blockPos));
            Block block = blockState.getBlock();
            BlockState cropState = minecraft.world.getBlockState(BlockPos.ofFloored(blockPos.add(0, 1, 0)));
            if (!(blockState.isOf(Blocks.FARMLAND) && cropState.isOf(Blocks.AIR))) {
                toRemove.add(blockPos);
            }
        }
        for (Vec3d blockPos : toRemove) blocksToPlant.remove(blockPos);

        if (blocksToPlant.size() >= 1) {
            for (int i = 0; i < Math.min(perTick, blocksToPlant.size()); i++) {
                Vec3d targetPos = blocksToPlant.remove(0);
                targetPos = targetPos.subtract(0.5, 0.5, 0.5);

                BlockState blockState = minecraft.world.getBlockState(BlockPos.ofFloored(targetPos));
                Block block = blockState.getBlock();
                BlockPos blockPos = new BlockPos((int) targetPos.x, (int) targetPos.y, (int) targetPos.z);

                if (blockPos.toCenterPos().distanceTo(minecraft.player.getPos()) <= 4 && minecraft.player.getInventory().count(Items.WHEAT_SEEDS) >= 3) {
                    minecraft.player.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, targetPos);
                    CropHelper.till(blockPos, block);
                    CropHelper.plant(blockPos, block);

                    if (playSound) {
                        minecraft.player.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 1, (float) 0.2);
                    }
                }
            }
        }

        if (blocksToHarvest.size() % 2 == 0 && blocksToHarvest.size() > 1) {
            Vec3d blockPos = blocksToHarvest.get(0);
            Vec3d direction = blockPos.subtract(minecraft.player.getPos());
            direction = direction.normalize();
            Vec3d result = direction.multiply(0.035 , 0.005, 0.035);

            minecraft.player.addVelocity(result);
        }

        if (blocksToHarvest.size() >= batchSize) {
            for (int i = 0; i < Math.min(perTick, blocksToHarvest.size()); i++) {
                Vec3d targetPos = blocksToHarvest.remove(0);
                targetPos = targetPos.subtract(0.5, 0.5, 0.5);

                BlockState blockState = minecraft.world.getBlockState(BlockPos.ofFloored(targetPos));
                Block block = blockState.getBlock();
                BlockPos blockPos = new BlockPos((int) targetPos.x, (int) targetPos.y, (int) targetPos.z);

                BlockState soilState = minecraft.world.getBlockState(BlockPos.ofFloored(targetPos.subtract(0, 1, 0)));
                Block soil = soilState.getBlock();
                BlockPos soilPos = new BlockPos((int) targetPos.x, (int) targetPos.y - 1, (int) targetPos.z);

                if (blockPos.toCenterPos().distanceTo(minecraft.player.getPos()) <= 4) {
                    minecraft.player.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, targetPos);
                    CropHelper.harvest(blockPos, blockState, block);
                    CropHelper.till(soilPos, soil);
                    CropHelper.plant(soilPos, soil);

                    if (playSound) {
                        minecraft.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                    }
                }
            }

            for (Vec3d blockPos : blocksToHarvest) {
                double distance = blockPos.distanceTo(minecraft.player.getPos());
                if (distance > 1) {
                    Vec3d direction = blockPos.subtract(minecraft.player.getPos());
                    direction = direction.normalize();
                    Vec3d result = direction.multiply(0.035 / blocksToHarvest.size() , 0.005, 0.035 / blocksToHarvest.size());

                    minecraft.player.addVelocity(result);
                }
            }
        } else if (shouldPickupItems) {
            int entityCount = 0;
            for (Entity entity : minecraft.world.getEntities()) {
                double distance = entity.getPos().distanceTo(minecraft.player.getPos());
                if (entity instanceof ItemEntity && distance <= harvestRange) {
                    entityCount++;
                }
            }
            if (entityCount > 0) {
                for (Entity entity : minecraft.world.getEntities()) {
                    double distance = entity.getPos().distanceTo(minecraft.player.getPos());
                    if (entity instanceof ItemEntity && distance <= harvestRange) {
                        Vec3d direction = entity.getPos().subtract(minecraft.player.getPos());
                        direction = direction.normalize();
                        Vec3d result = direction.multiply(0.055, 0.005, 0.055);

                        Vec3d lookHere = new Vec3d(entity.getX(), minecraft.player.getEyeY(), entity.getZ());

                        minecraft.player.addVelocity(result);
                        minecraft.player.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, lookHere);
                        break;
                    }
                }
            }

            if (logOnInvFull && minecraft.player.getInventory().size() >= 36) {
                minecraft.disconnect(new DisconnectedScreen(minecraft.currentScreen, Text.literal("Disconnected by FarmUtils"), Text.literal("Inventory Full"), Text.literal("Exit")));
            }
        }
    }
}