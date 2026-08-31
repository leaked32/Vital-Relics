package com.example.vitalrelics.client;

import com.example.vitalrelics.common.Manifest;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class ExternalTextures {
	private static final Path TEXTURE_DIR =
			FMLPaths.CONFIGDIR.get().resolve("vitalrelics/textures");
	private static final Map<String, ResourceLocation> CACHE = new HashMap<>();

	private ExternalTextures() {}

	public static ResourceLocation texture(final String filename) {
		return CACHE.computeIfAbsent(filename, ExternalTextures::load);
	}

	private static ResourceLocation load(final String filename) {
		final Path file = TEXTURE_DIR.resolve(filename).normalize();
		if (!file.startsWith(TEXTURE_DIR) || !Files.isRegularFile(file))
			return packaged(filename);

		try (final InputStream stream = Files.newInputStream(file)) {
			final NativeImage image = NativeImage.read(stream);
			final DynamicTexture texture = new DynamicTexture(image);
			return Minecraft.getInstance().getTextureManager().register(
					Manifest.MODID + "/external/" + safeName(filename), texture
			);
		}
		catch (final IOException exception) {
			return packaged(filename);
		}
	}

	private static ResourceLocation packaged(final String filename) {
		return new ResourceLocation(Manifest.MODID, "textures/" + filename);
	}

	private static String safeName(final String filename) {
		return filename.replace('\\', '/').replace('/', '_').replace('.', '_');
	}
}
