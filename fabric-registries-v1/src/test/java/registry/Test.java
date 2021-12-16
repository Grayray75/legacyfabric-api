package registry;

import net.fabricmc.api.ModInitializer;
import net.minecraft.block.Block;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class Test implements ModInitializer {
    @Override
    public void onInitialize() {
        System.out.println("register");
        Block block = Registry.BLOCK.method_19956(new Identifier("test", "block"));
    }
}
