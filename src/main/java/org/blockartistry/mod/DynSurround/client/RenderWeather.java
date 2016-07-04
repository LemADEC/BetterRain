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

package org.blockartistry.mod.DynSurround.client;

import java.util.ArrayList;
import java.util.List;

import org.blockartistry.mod.DynSurround.client.EnvironStateHandler.EnvironState;
import org.blockartistry.mod.DynSurround.client.aurora.AuroraRenderer;
import org.blockartistry.mod.DynSurround.client.storm.StormRenderer;
import org.blockartistry.mod.DynSurround.client.storm.StormSplashRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraftforge.client.IRenderHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class RenderWeather extends IRenderHandler {

	private static final List<IAtmosRenderer> renderList = new ArrayList<IAtmosRenderer>();
	private static final RenderWeather instance = new RenderWeather();

	private IRenderHandler parent;

	public static void register(final IAtmosRenderer renderer) {
		renderList.add(renderer);
	}

	static {
		register(new StormRenderer());
		register(new AuroraRenderer());
	}
	
	public static IRenderHandler setParent(IRenderHandler parentRenderer) {
		instance.parent = parentRenderer;
		return instance;
	}

	/*
	 * Render rain particles.
	 * 
	 * Redirect from EntityRenderer.
	 */
	public static void addRainParticles(final EntityRenderer theThis) {
		StormSplashRenderer.renderStormSplashes(EnvironState.getDimensionId(), theThis);
	}

	/*
	 * Render atmospheric effects.
	 */
	@Override
	public void render(float partialTicks, WorldClient world, Minecraft mc) {
		for (final IAtmosRenderer renderer : renderList)
			renderer.render(partialTicks, world, mc, this.parent);
	}
}
