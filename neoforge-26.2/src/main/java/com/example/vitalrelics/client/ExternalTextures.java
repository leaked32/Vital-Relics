package com.example.vitalrelics.client;

import com.example.vitalrelics.common.Manifest;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class ExternalTextures {
	private static final Path TEXTURE_DIR =
			FMLPaths.CONFIGDIR.get().resolve("vitalrelics/textures");
	private static final Map<String, Identifier> CACHE = new HashMap<>();

	private ExternalTextures() {}

	public static Identifier texture(final String filename) {
		return CACHE.computeIfAbsent(filename, ExternalTextures::load);
	}

	private static Identifier load(final String filename) {
		final Path file = TEXTURE_DIR.resolve(filename).normalize();
		if (!file.startsWith(TEXTURE_DIR))
			return packaged(filename);

		if (Files.isRegularFile(file)) {
			try (final InputStream stream = Files.newInputStream(file)) {
				return register(filename, stream);
			}
			catch (final IOException exception) {
				// Fall through to the packaged texture.
			}
		}

		return packaged(filename);
	}

	private static Identifier packaged(final String filename) {
		try (final InputStream stream = ExternalTextures.class.getClassLoader()
				.getResourceAsStream("vitalrelics/textures/" + filename)) {
			if (stream != null)
				return register("packaged/" + filename, stream);
		}
		catch (final IOException exception) {
			// Minecraft will show its missing texture for invalid bundled files.
		}

		return Identifier.fromNamespaceAndPath("minecraft", "textures/missingno.png");
	}

	private static Identifier register(
			final String name,
			final InputStream stream) throws IOException {
		final NativeImage image = NativeImage.read(stream);
		final Identifier location = Identifier.fromNamespaceAndPath(
				Manifest.MODID,
				"external/" + safeName(name));

		final DynamicTexture texture =
				new DynamicTexture(() -> location.toString(), image);

		Minecraft.getInstance().getTextureManager().register(location, texture);
		return location;
	}

	private static String safeName(final String filename) {
		return filename.replace('\\', '/').replace('/', '_').replace('.', '_');
	}
}
