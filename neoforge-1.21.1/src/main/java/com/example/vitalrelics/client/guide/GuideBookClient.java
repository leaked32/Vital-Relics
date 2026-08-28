package com.example.vitalrelics.client.guide;

import com.example.vitalrelics.VitalRelics;
import com.example.vitalrelics.common.RelicAcquisitionLoader;
import com.example.vitalrelics.common.RelicLoader;
import com.example.vitalrelics.common.guide.GuideBook;
import net.minecraft.client.Minecraft;

public final class GuideBookClient {
	private GuideBookClient() {}

	public static void open() {
		final GuideBook guideBook = new GuideBook(
				RelicLoader.INSTANCE,
				RelicAcquisitionLoader.INSTANCE
		);

		Minecraft.getInstance().setScreen(
				new GuideBookScreen(guideBook)
		);
	}
}