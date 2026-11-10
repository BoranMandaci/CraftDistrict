package com.craftdistrict.mixin.client;

import com.craftdistrict.CraftDistrict;
import net.minecraft.client.renderer.block.model.multipart.Selector;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.AbstractMap;
import java.util.Map;
import java.util.function.Predicate;

/** One immutable condition per parsed CraftDistrict selector, discarded with its resource model on reload. */
@Mixin(Selector.class)
public abstract class MultipartPredicateMixin {
    // Publish the definition and predicate together; asynchronous model loaders must never see a mismatched pair.
    @Unique private volatile Map.Entry<StateDefinition<Block, BlockState>, Predicate<BlockState>> craftdistrict$predicate;

    @Inject(method = "getPredicate(Lnet/minecraft/world/level/block/state/StateDefinition;)Ljava/util/function/Predicate;",
            at = @At("HEAD"), cancellable = true)
    private void craftdistrict$reusePredicate(StateDefinition<Block, BlockState> definition,
                                             CallbackInfoReturnable<Predicate<BlockState>> cir) {
        var cached = craftdistrict$predicate;
        if (cached != null && cached.getKey() == definition) cir.setReturnValue(cached.getValue());
    }

    @Inject(method = "getPredicate(Lnet/minecraft/world/level/block/state/StateDefinition;)Ljava/util/function/Predicate;",
            at = @At("RETURN"))
    private void craftdistrict$rememberPredicate(StateDefinition<Block, BlockState> definition,
                                                CallbackInfoReturnable<Predicate<BlockState>> cir) {
        if (definition == null) return;
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(definition.getOwner());
        if (id != null && CraftDistrict.MOD_ID.equals(id.getNamespace()) && cir.getReturnValue() != null) {
            var cached = craftdistrict$predicate;
            if (cached == null || cached.getKey() != definition)
                craftdistrict$predicate = new AbstractMap.SimpleImmutableEntry<>(definition, cir.getReturnValue());
        }
    }
}
