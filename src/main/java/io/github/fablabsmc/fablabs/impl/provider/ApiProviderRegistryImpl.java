package io.github.fablabsmc.fablabs.impl.provider;

import io.github.fablabsmc.fablabs.api.provider.v1.ApiAccess;
import io.github.fablabsmc.fablabs.api.provider.v1.ApiProviderRegistry;
import io.github.fablabsmc.fablabs.api.provider.v1.BlockApiProvider;
import io.github.fablabsmc.fablabs.api.provider.v1.BlockEntityApiProvider;
import io.github.fablabsmc.fablabs.mixin.provider.BlockEntityTypeAccessor;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

public class ApiProviderRegistryImpl implements ApiProviderRegistry {
    private final Map<ApiAccess<?>, Map<Block, BlockApiProvider<?>>> blockProviders = new Reference2ObjectOpenHashMap<>();
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public <Api> @Nullable Api getFromBlock(ApiAccess<Api> apiAccess, World world, BlockPos pos, @NotNull Direction direction) {
        if(apiAccess != null) {
            Map<Block, BlockApiProvider<?>> providers = blockProviders.get(apiAccess);
            if(providers != null) {
                Block block = world.getBlockState(pos).getBlock();
                BlockApiProvider<?> provider = providers.get(block);
                if(provider != null) {
                    return (Api) provider.get(world, pos, direction);
                }
            }
        }
        return null;
    }

    @Override
    public <Api> void registerForBlock(ApiAccess<Api> apiAccess, BlockApiProvider<Api> provider, Block... blocks) {
        if(apiAccess != null) {
            Objects.requireNonNull(provider, "encountered null BlockApiProvider");

            for(final Block block : blocks) {
                Objects.requireNonNull(block, "encountered null block while registering a block API provider mapping");

                blockProviders.putIfAbsent(apiAccess, new Reference2ReferenceOpenHashMap<>());

                if(blockProviders.get(apiAccess).putIfAbsent(block, provider) != null) {
                    LOGGER.warn("Encountered duplicate API provider registration for block: " + Registry.BLOCK.getId(block));
                }
            }
        }
    }

    @Override
    public <Api> void registerForBlockEntity(ApiAccess<Api> apiAccess, BlockEntityApiProvider<Api> provider, BlockEntityType<?>... types) {
        if(apiAccess != null) {
            Objects.requireNonNull(provider, "encountered null BlockEntityApiProvider");

            for(final BlockEntityType<?> bet : types) {
                Objects.requireNonNull(bet, "encountered null block entity type while registering a block entity API provider mapping");

                Block[] blocks = ((BlockEntityTypeAccessor) bet).getBlocks().toArray(new Block[0]);
                BlockApiProvider<Api> blockProvider = (world, pos, direction) -> {
                    BlockEntity be = world.getBlockEntity(pos);
                    if(be == null) {
                        return null;
                    } else {
                        return provider.get(be, direction);
                    }
                };
                registerForBlock(apiAccess, blockProvider, blocks);
            }
        }
    }
}
