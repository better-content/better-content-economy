package com.bettercontent.economy.mixin;

import com.sammy.malum.common.item.spirit.SpiritShardItem;
import com.sammy.malum.core.systems.spirit.MalumSpiritType;
import java.util.function.Supplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = MalumSpiritType.class, remap = false)
public interface MalumSpiritTypeAccessor {
    @Mutable @Accessor("spiritShard")
    void betterContentEconomy$setSpiritShard(Supplier<SpiritShardItem> supplier);
}
