package meeplabsdev.farmutils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.world.World;

public class ParticleSpawner {
    private World world;

    public void spawnParticle(ParticleEffect particleType, double px, double py, double pz, double dx, double dy, double dz) {
        this.world = MinecraftClient.getInstance().world;
        if (this.world != null) {
            world.addParticle(particleType, px, py, pz, dx, dy, dz);
        }
    }
}

