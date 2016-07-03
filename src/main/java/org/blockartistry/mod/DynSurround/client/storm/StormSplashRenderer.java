/*
 * This file is part of Dynamic Surroundings Unofficial, licensed under the MIT License (MIT).
 *
 * Copyright (c) OreCruncher, Abastro
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.blockartistry.mod.DynSurround.client.storm;

import org.apache.commons.lang3.StringUtils;
import org.blockartistry.mod.DynSurround.ModOptions;
import org.blockartistry.mod.DynSurround.client.EnvironStateHandler.EnvironState;
import org.blockartistry.mod.DynSurround.client.WeatherUtils;
import org.blockartistry.mod.DynSurround.client.fx.particle.ParticleFactory;
import org.blockartistry.mod.DynSurround.data.BiomeRegistry;
import org.blockartistry.mod.DynSurround.data.DimensionRegistry;
import org.blockartistry.mod.DynSurround.util.DiurnalUtils;
import org.blockartistry.mod.DynSurround.util.XorShiftRandom;

import gnu.trove.map.hash.TIntObjectHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.NoiseGeneratorSimplex;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class StormSplashRenderer {

	private static final TIntObjectHashMap<StormSplashRenderer> splashRenderers = new TIntObjectHashMap<StormSplashRenderer>();
	private static final StormSplashRenderer DEFAULT = new StormSplashRenderer();

	static {
		splashRenderers.put(0, DEFAULT);
		splashRenderers.put(-1, new NetherSplashRenderer());
		splashRenderers.put(1, new NullSplashRenderer());
	}

	public static void renderStormSplashes(final int dimensionId, final EntityRenderer renderer) {
		StormSplashRenderer splash = splashRenderers.get(dimensionId);
		if (splash == null)
			splash = DEFAULT;
		splash.addRainParticles(renderer);
	}

	protected StormSplashRenderer() {

	}

	protected static final int PARTICLE_SOUND_CHANCE = 20;
	protected static final int RANGE = 10;
	protected static final XorShiftRandom RANDOM = new XorShiftRandom();
	protected static final NoiseGeneratorSimplex GENERATOR = new NoiseGeneratorSimplex(RANDOM);

	protected static float calculateRainSoundVolume(final World world) {
		return MathHelper
				.clamp_float(
						(float) (StormProperties.getCurrentVolume()
								+ GENERATOR.getValue(world.getCelestialAngle(1.0f) * 240.0f - 60.0f, 1) / 5.0F),
						0.0F, 1.0F);
	}

	protected EntityFX getBlockParticleFX(final Block block, final boolean dust, final World world, final double x,
			final double y, final double z) {
		IParticleFactory factory = null;

		if (dust) {
			factory = null;
		} else if (block == Blocks.soul_sand) {
			factory = null;
		} else if (block == Blocks.netherrack && RANDOM.nextInt(20) == 0) {
			factory = ParticleFactory.lavaSpark;
		} else if (block.getMaterial() == Material.lava) {
			factory = ParticleFactory.smoke;
		} else if (block.getMaterial() != Material.air) {
			factory = ParticleFactory.rain;
		}

		return factory != null ? factory.getEntityFX(0, world, x, y, z, 0, 0, 0) : null;
	}

	protected String getBlockSoundFX(final Block block, final boolean hasDust, final World world) {
		if (hasDust)
			return StormProperties.getIntensity().getDustSound();
		if (block == Blocks.netherrack)
			return "minecraft:liquid.lavapop";
		return StormProperties.getIntensity().getStormSound();
	}

	protected BlockPos getPrecipitationHeight(final World world, final int range, final BlockPos pos) {
		return world.getPrecipitationHeight(pos);
	}

	protected void playSplashSound(final EntityRenderer renderer, final WorldClient world, final Entity player,
			double x, double y, double z) {
		final int theX = MathHelper.floor_double(x);
		final int theY = MathHelper.floor_double(y);
		final int theZ = MathHelper.floor_double(z);

		final BlockPos coord = new BlockPos(theX, theY, theZ);
		final boolean hasDust = WeatherUtils.biomeHasDust(world.getBiomeGenForCoords(coord));
		final Block block = world.getBlockState(coord.down()).getBlock();
		final String sound = getBlockSoundFX(block, hasDust, world);
		if (!StringUtils.isEmpty(sound)) {
			final float volume = calculateRainSoundVolume(world);
			float pitch = 1.0F;
			final int playerX = MathHelper.floor_double(player.posX);
			final int playerY = MathHelper.floor_double(player.posY);
			final int playerZ = MathHelper.floor_double(player.posZ);
			if (y > player.posY + 1.0D
					&& world.getPrecipitationHeight(new BlockPos(playerX, 0, playerZ)).getY() > playerY)
				pitch = 0.5F;
			renderer.mc.theWorld.playSound(x, y, z, sound, volume, pitch, false);
		}
	}

	public void addRainParticles(final EntityRenderer theThis) {
		if (theThis.mc.gameSettings.particleSetting == 2)
			return;

		if (!DimensionRegistry.hasWeather(EnvironState.getWorld()))
			return;

		float rainStrengthFactor = theThis.mc.theWorld.getRainStrength(1.0F);
		if (!theThis.mc.gameSettings.fancyGraphics)
			rainStrengthFactor /= 2.0F;

		if (rainStrengthFactor <= 0.0F)
			return;

		RANDOM.setSeed((long) theThis.rendererUpdateCount * 312987231L);
		final Entity entity = theThis.mc.getRenderViewEntity();
		final WorldClient worldclient = theThis.mc.theWorld;
		final int playerX = MathHelper.floor_double(entity.posX);
		final int playerY = MathHelper.floor_double(entity.posY);
		final int playerZ = MathHelper.floor_double(entity.posZ);
		double spawnX = 0.0D;
		double spawnY = 0.0D;
		double spawnZ = 0.0D;
		int particlesSpawned = 0;

		int particleCount = (int) (ModOptions.particleCountBase * rainStrengthFactor * rainStrengthFactor);

		if (theThis.mc.gameSettings.particleSetting == 1)
			particleCount >>= 1;

		BlockPos.MutableBlockPos posXZ = new BlockPos.MutableBlockPos();
		for (int j1 = 0; j1 < particleCount; ++j1) {
			final int locX = playerX + RANDOM.nextInt(RANGE) - RANDOM.nextInt(RANGE);
			final int locZ = playerZ + RANDOM.nextInt(RANGE) - RANDOM.nextInt(RANGE);
			posXZ.setPos(locX, 0, locZ);
			final BlockPos precipHeight = getPrecipitationHeight(worldclient, RANGE / 2, posXZ);
			final Biome biome = worldclient.getBiome(posXZ);
			final boolean hasDust = WeatherUtils.biomeHasDust(biome);

			if (precipHeight.getY() <= playerY + RANGE && precipHeight.getY() >= playerY - RANGE && (hasDust
					|| (BiomeRegistry.hasPrecipitation(biome) && biome.getFloatTemperature(precipHeight) >= 0.15F))) {

				final Block block = worldclient.getBlockState(precipHeight.down()).getBlock();
				final double posX = locX + RANDOM.nextFloat();
				final double posY = precipHeight.getY() + 0.1F - block.getBlockBoundsMinY();
				final double posZ = locZ + RANDOM.nextFloat();

				final EntityFX particle = getBlockParticleFX(block, hasDust, worldclient, posX, posY, posZ);
				if (particle != null)
					theThis.mc.effectRenderer.addEffect(particle);

				if (RANDOM.nextInt(++particlesSpawned) == 0) {
					spawnX = posX;
					spawnY = posY;
					spawnZ = posZ;
				}
			}
		}

		if (particlesSpawned > 0 && RANDOM.nextInt(PARTICLE_SOUND_CHANCE) < theThis.rainSoundCounter++) {
			theThis.rainSoundCounter = 0;
			playSplashSound(theThis, worldclient, entity, spawnX, spawnY, spawnZ);
		}
	}
}
