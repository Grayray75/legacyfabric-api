package registry;

import net.fabricmc.api.ModInitializer;
import net.minecraft.item.Items;

public class Test implements ModInitializer {
	@Override
	public void onInitialize() {
		System.out.println("Hello from common code! " + Items.ACACIA_BOAT.getTranslationKey());
	}
}
