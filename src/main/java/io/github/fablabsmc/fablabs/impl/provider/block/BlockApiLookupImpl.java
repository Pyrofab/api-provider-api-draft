package io.github.fablabsmc.fablabs.impl.provider.block;

import io.github.fablabsmc.fablabs.api.provider.v1.ApiProviderMap;
import io.github.fablabsmc.fablabs.api.provider.v1.ContextKey;
import io.github.fablabsmc.fablabs.api.provider.v1.block.BlockApiLookup;
import io.github.fablabsmc.fablabs.mixin.provider.BlockEntityTypeAccessor;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class BlockApiLookupImpl<T, C> implements BlockApiLookup<T, C> {
    private static final Logger LOGGER = LogManager.getLogger();
    private final ApiProviderMap<Block, BlockApiProvider<?, ?>> providerMap = ApiProviderMap.create();
    private final Identifier id;
    private final ContextKey<C> contextKey;

    BlockApiLookupImpl(Identifier apiId, ContextKey<C> contextKey) {
        this.id = apiId;
        this.contextKey = contextKey;
    }

    @Override
    public @Nullable T get(World world, BlockPos pos, C context) {
        @SuppressWarnings("unchecked") BlockApiProvider<T, C> provider = (BlockApiProvider<T, C>) providerMap.get(world.getBlockState(pos).getBlock());
        if(provider != null) {
            return provider.get(world, pos, context);
        } else {
            return null;
        }
    }

    @Override
    public void registerForBlocks(BlockApiProvider<T, C> provider, Block... blocks) {
        Objects.requireNonNull(provider, "encountered null BlockApiProvider");

        for(final Block block : blocks) {
            Objects.requireNonNull(block, "encountered null block while registering a block API provider mapping");

            if(providerMap.putIfAbsent(block, provider) != null) {
                LOGGER.warn("Encountered duplicate API provider registration for block: " + Registry.BLOCK.getId(block));
            }
        }
    }

    @Override
    public void registerForBlockEntities(BlockEntityApiProvider<T, C> provider, BlockEntityType<?>... blockEntityTypes) {
        Objects.requireNonNull(provider, "encountered null BlockEntityApiProvider");

        for(final BlockEntityType<?> bet : blockEntityTypes) {
            Objects.requireNonNull(bet, "encountered null block entity type while registering a block entity API provider mapping");

            Block[] blocks = ((BlockEntityTypeAccessor) bet).getBlocks().toArray(new Block[0]);
            BlockApiProvider<T, C> blockProvider = (world, pos, context) -> {
                BlockEntity be = world.getBlockEntity(pos);
                if(be == null) {
                    return null;
                } else {
                    return provider.get(be, context);
                }
            };
            registerForBlocks(blockProvider, blocks);
        }
    }

    @Override
    public @NotNull Identifier getApiId() {
        return id;
    }

    @Override
    public @NotNull ContextKey<C> getContextKey() {
        return contextKey;
    }
}
