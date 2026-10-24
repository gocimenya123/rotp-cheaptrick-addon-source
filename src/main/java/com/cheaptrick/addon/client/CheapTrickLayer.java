package com.cheaptrick.addon.client;

import com.cheaptrick.addon.CheapTrickAddon;
import com.cheaptrick.addon.config.CheapTrickConfig;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.IEntityRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.util.ResourceLocation;

/**
 * Слой рендера игрока: модель рисуется в системе координат ТЕЛА игрока (body.translateAndRotate),
 * поэтому она физически «приклеена» к спине и автоматически повторяет приседание, плавание, полёт на элитрах,
 * повороты тела и анимации. Отдельной сущности нет, летающего следования нет.
 * Для каждого клиента носитель определяется по UUID из пакета синхронизации — дублей быть не может.
 */
public class CheapTrickLayer extends LayerRenderer<AbstractClientPlayerEntity, PlayerModel<AbstractClientPlayerEntity>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(CheapTrickAddon.MOD_ID, "textures/entity/cheap_trick.png");
    private final CheapTrickModel model = new CheapTrickModel();

    /** Масштаб всей модели (положение у плеча заложено в самой модели). */
    private static final float SCALE = 0.7F;

    public CheapTrickLayer(IEntityRenderer<AbstractClientPlayerEntity, PlayerModel<AbstractClientPlayerEntity>> renderer) {
        super(renderer);
    }

    @Override
    public void render(MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight, AbstractClientPlayerEntity player,
            float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!CheapTrickConfig.visualModel() || !ClientState.isCarrier(player.getUUID())
                || player.isInvisible() || player.isSpectator()) {
            return;
        }
        matrixStack.pushPose();
        // Система координат тела: учитывает приседание/плавание/элитры/поворот корпуса
        getParentModel().body.translateAndRotate(matrixStack);
        // Нагрудник добавляет ~1px толщины на спине — отодвигаем модель, чтобы не утонула, но без зазора
        float standoff = player.getItemBySlot(EquipmentSlotType.CHEST).isEmpty() ? 0.0F : 1.0F / 16.0F;
        matrixStack.translate(0.0D, 0.0D, standoff);
        matrixStack.scale(SCALE, SCALE, SCALE);
        model.animate(ageInTicks);
        IVertexBuilder vertexBuilder = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        model.renderToBuffer(matrixStack, vertexBuilder, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        matrixStack.popPose();
    }
}
