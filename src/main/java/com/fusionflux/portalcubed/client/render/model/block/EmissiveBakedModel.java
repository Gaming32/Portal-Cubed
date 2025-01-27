package com.fusionflux.portalcubed.client.render.model.block;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.MaterialFinder;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.random.RandomGenerator;
import net.minecraft.world.BlockRenderView;

public final class EmissiveBakedModel extends ForwardingBakedModel {

	private static final Map<Identifier, Function<BakedModel, EmissiveBakedModel>> wrappers = new Object2ObjectOpenHashMap<>();

	public static void register(Identifier modelId) {
		wrappers.put(modelId, bakedModel -> new EmissiveBakedModel(bakedModel));
	}

	public static Optional<BakedModel> wrap(Identifier modelId, BakedModel model) {
		final Function<BakedModel, EmissiveBakedModel> wrapper = wrappers.get(new Identifier(modelId.getNamespace(), modelId.getPath()));
		if (wrapper != null) return Optional.of(wrapper.apply(model));
		return Optional.empty();
	}


	private static final MaterialFinder MATERIAL_FINDER = RendererAccess.INSTANCE.getRenderer().materialFinder();

	private Pair<BlockState, Mesh> cachedMesh = Pair.of(null, null);

	EmissiveBakedModel(BakedModel model) {
		this.wrapped = model;
	}

	@Override
	public boolean isVanillaAdapter() {
		return false;
	}

	@Override
	public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<RandomGenerator> randomSupplier, RenderContext context) {
		final ModelObjects objects = ModelObjects.get();
		objects.cullingCache.prepare(pos, state);
		buildMesh(objects, state, randomSupplier);
		context.pushTransform(quad -> {
			return !objects.cullingCache.shouldCull(quad, blockView);
		});
		context.meshConsumer().accept(cachedMesh.getValue());
		context.popTransform();
	}

	@Override
	public void emitItemQuads(ItemStack stack, Supplier<RandomGenerator> randomSupplier, RenderContext context) {
		final ModelObjects objects = ModelObjects.get();
		buildMesh(objects, null, randomSupplier);
		context.meshConsumer().accept(cachedMesh.getValue());
	}


	private void buildMesh(ModelObjects objects, @Nullable BlockState state, Supplier<RandomGenerator> randomSupplier) {
		boolean shouldBuild = true;
		if (state != null) shouldBuild = cachedMesh.getKey() != state;
		if (!shouldBuild) return;

		final QuadEmitter emitter = objects.meshBuilder.getEmitter();

		for (int i = 0; i <= ModelHelper.NULL_FACE_ID; i++) {
			final Direction cullFace = ModelHelper.faceFromIndex(i);
			final List<BakedQuad> quads = wrapped.getQuads(state, cullFace, randomSupplier.get());

			for (BakedQuad quad : quads) {
				boolean isQuadEmissive = EmissiveSpriteRegistry.isEmissive(quad.getSprite().getId());
				MATERIAL_FINDER.emissive(0, isQuadEmissive);
				MATERIAL_FINDER.disableDiffuse(0, isQuadEmissive);
				MATERIAL_FINDER.disableAo(0, isQuadEmissive);

				BlendMode blendMode = BlendMode.DEFAULT;
				if (state != null) {
					blendMode = BlendMode.fromRenderLayer(RenderLayers.getBlockLayer(state));
					if (blendMode == BlendMode.SOLID) blendMode = BlendMode.CUTOUT_MIPPED;
				}
				MATERIAL_FINDER.blendMode(0, blendMode);

				emitter.fromVanilla(quad, MATERIAL_FINDER.find(), cullFace);
				emitter.cullFace(cullFace);
				emitter.emit();
			}
		}

		cachedMesh = Pair.of(state, objects.meshBuilder.build());
	}


	private static class ModelObjects {

		private static final ThreadLocal<ModelObjects> INSTANCE = ThreadLocal.withInitial(ModelObjects::new);

		private final CullingCache cullingCache = new CullingCache();

		private final MeshBuilder meshBuilder = RendererAccess.INSTANCE.getRenderer().meshBuilder();

		private static ModelObjects get() {
			return INSTANCE.get();
		}

	}

}
