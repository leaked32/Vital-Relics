package com.example.vitalrelics.client.guide;

import com.example.vitalrelics.common.relics.Acquisition;
import com.example.vitalrelics.common.relics.Loader;
import com.example.vitalrelics.common.guide.GuideBook;
import net.minecraft.client.Minecraft;

public final class GuideBookClient {
	private GuideBookClient() {}

	public static void open() {
		final GuideBook guideBook = new GuideBook(
				Loader.get(),
				Acquisition.get()
		);

		Minecraft.getInstance().setScreen(
				new GuideBookScreen(guideBook)
		);
	}
}
