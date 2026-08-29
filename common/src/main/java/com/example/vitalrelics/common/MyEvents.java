package com.example.vitalrelics.common;

import com.example.vitalrelics.common.platform.MyLivingEntity;
import com.example.vitalrelics.common.platform.MyUtils;

import java.util.List;

public class MyEvents {

	public static void onLivingEntityTick(
			MyLivingEntity myLivingEntity, final int currentTick, List<Relic> relics) {


		MyUtils.removeImmuneEffects(myLivingEntity, relics, MyLivingEntity.MyEffectCategory.ALL);
		MyUtils.applyRelicEffects(myLivingEntity, relics);

	}

}
