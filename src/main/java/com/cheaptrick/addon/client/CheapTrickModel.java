package com.cheaptrick.addon.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.Model;

/**
 * Модель Cheap Trick (по аниме): жёлто-коричневый человечек сидит на спине носителя горбом за плечом, а его крупная голова
 * лежит прямо на плече (без шеи, с наклоном к шее игрока) и смотрит вперёд; руки держатся за плечи игрока.
 * Слой масштабирует модель до 0,7. Голова лысая, с розовым «мозгом» на макушке, синими глазами с трещинами-слезами,
 * оливковый шлем-маска с зубастой пастью и наушниками, синие браслеты, коричневая «юбка» с подтёками, четырёхпалые руки.
 * Геометрия и текстура сгенерированы скриптом; координаты в пикселях в системе тела игрока
 * (тело: x -4..4, y 0..12, z -2..2; спина — z = +2). Текстура 64x64: textures/entity/cheap_trick.png.
 */
public class CheapTrickModel extends Model {
    private final ModelRenderer root;
    private final ModelRenderer head;
    private final ModelRenderer armR;
    private final ModelRenderer armL;

    public CheapTrickModel() {
        super(RenderType::entityCutoutNoCull);
        texWidth = 64;
        texHeight = 64;

        root = new ModelRenderer(this);
        root.setPos(0.0F, 0.0F, 0.0F);
        ModelRenderer body = new ModelRenderer(this);
        body.setPos(6.57F, -6.14F, 1.43F);
        body.texOffs(12, 11).addBox(-3.0F, 3.0F, 2.0F, 6.0F, 8.0F, 3.0F, 0.0F);
        body.texOffs(30, 11).addBox(-2.5F, 11.0F, 2.0F, 5.0F, 2.0F, 3.0F, 0.0F);
        body.texOffs(0, 22).addBox(-3.5F, 10.5F, 2.0F, 7.0F, 2.0F, 5.0F, 0.0F);
        body.texOffs(58, 29).addBox(3.0F, 3.0F, 4.0F, 1.0F, 3.0F, 1.0F, 0.0F);
        body.texOffs(24, 22).addBox(-3.0F, 12.0F, 6.3F, 1.0F, 2.0F, 1.0F, 0.0F);
        body.texOffs(24, 22).addBox(-1.0F, 12.0F, 6.3F, 1.0F, 2.0F, 1.0F, 0.0F);
        body.texOffs(24, 22).addBox(1.0F, 12.0F, 6.3F, 1.0F, 2.0F, 1.0F, 0.0F);
        body.texOffs(24, 22).addBox(2.0F, 12.0F, 6.3F, 1.0F, 2.0F, 1.0F, 0.0F);
        body.texOffs(24, 22).addBox(-3.5F, 12.0F, 2.5F, 1.0F, 2.0F, 1.0F, 0.0F);
        body.texOffs(24, 22).addBox(-3.5F, 12.0F, 4.5F, 1.0F, 2.0F, 1.0F, 0.0F);
        body.texOffs(24, 22).addBox(2.5F, 12.0F, 2.5F, 1.0F, 2.0F, 1.0F, 0.0F);
        body.texOffs(24, 22).addBox(2.5F, 12.0F, 4.5F, 1.0F, 2.0F, 1.0F, 0.0F);
        // Руки: плечи на торсе, предплечья идут вверх к плечам игрока; пальцы обхватывают плечо сзади
        armR = new ModelRenderer(this);
        armR.setPos(1.43F, 8.71F, 3.57F);
        armR.xRot = -0.06F;
        armR.zRot = -2.21F;
        armR.texOffs(44, 22).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F, 0.0F);
        armR.texOffs(28, 22).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F, 0.0F);
        ModelRenderer armRFore = new ModelRenderer(this);
        armRFore.setPos(5.42F, 5.72F, 3.29F);
        armRFore.xRot = -0.39F;
        armRFore.zRot = 2.81F;
        armRFore.texOffs(44, 22).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F, 0.0F);
        armRFore.texOffs(36, 22).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 4.0F, 2.0F, 0.0F);
        ModelRenderer armRHand = new ModelRenderer(this);
        armRHand.setPos(4.21F, 2.22F, 1.77F);
        armRHand.xRot = -1.21F;
        armRHand.zRot = -0.0F;
        armRHand.texOffs(20, 29).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 1.0F, 3.0F, 0.0F);
        armRHand.texOffs(0, 29).addBox(-2.0F, 0.0F, -1.0F, 4.0F, 2.0F, 2.0F, 0.0F);
        ModelRenderer armRF0 = new ModelRenderer(this);
        armRF0.setPos(-1.5F, 2.0F, 0.0F);
        armRF0.zRot = 0.3F;
        armRF0.texOffs(12, 29).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F, 0.0F);
        armRF0.texOffs(16, 29).addBox(-0.5F, 3.0F, -0.5F, 1.0F, 1.0F, 1.0F, 0.3F);
        armRHand.addChild(armRF0);
        ModelRenderer armRF1 = new ModelRenderer(this);
        armRF1.setPos(-0.5F, 2.0F, 0.0F);
        armRF1.zRot = 0.1F;
        armRF1.texOffs(12, 29).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F, 0.0F);
        armRF1.texOffs(16, 29).addBox(-0.5F, 3.0F, -0.5F, 1.0F, 1.0F, 1.0F, 0.3F);
        armRHand.addChild(armRF1);
        ModelRenderer armRF2 = new ModelRenderer(this);
        armRF2.setPos(0.5F, 2.0F, 0.0F);
        armRF2.zRot = -0.1F;
        armRF2.texOffs(12, 29).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F, 0.0F);
        armRF2.texOffs(16, 29).addBox(-0.5F, 3.0F, -0.5F, 1.0F, 1.0F, 1.0F, 0.3F);
        armRHand.addChild(armRF2);
        ModelRenderer armRF3 = new ModelRenderer(this);
        armRF3.setPos(1.5F, 2.0F, 0.0F);
        armRF3.zRot = -0.3F;
        armRF3.texOffs(12, 29).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F, 0.0F);
        armRF3.texOffs(16, 29).addBox(-0.5F, 3.0F, -0.5F, 1.0F, 1.0F, 1.0F, 0.3F);
        armRHand.addChild(armRF3);
        body.addChild(armR);
        body.addChild(armRFore);
        body.addChild(armRHand);
        armL = new ModelRenderer(this);
        armL.setPos(-2.86F, 8.71F, 3.57F);
        armL.xRot = 0.0F;
        armL.zRot = 1.68F;
        armL.texOffs(44, 22).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F, 0.0F);
        armL.texOffs(28, 22).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F, 0.0F);
        ModelRenderer armLFore = new ModelRenderer(this);
        armLFore.setPos(-7.83F, 8.19F, 3.57F);
        armLFore.xRot = -0.15F;
        armLFore.zRot = 1.72F;
        armLFore.texOffs(44, 22).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F, 0.0F);
        armLFore.texOffs(36, 22).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 4.0F, 2.0F, 0.0F);
        ModelRenderer armLHand = new ModelRenderer(this);
        armLHand.setPos(-11.74F, 7.61F, 2.99F);
        armLHand.xRot = -0.35F;
        armLHand.zRot = 2.68F;
        armLHand.texOffs(20, 29).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 1.0F, 3.0F, 0.0F);
        armLHand.texOffs(0, 29).addBox(-2.0F, 0.0F, -1.0F, 4.0F, 2.0F, 2.0F, 0.0F);
        ModelRenderer armLF0 = new ModelRenderer(this);
        armLF0.setPos(-1.5F, 2.0F, 0.0F);
        armLF0.zRot = 0.3F;
        armLF0.texOffs(12, 29).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F, 0.0F);
        armLF0.texOffs(16, 29).addBox(-0.5F, 3.0F, -0.5F, 1.0F, 1.0F, 1.0F, 0.3F);
        armLHand.addChild(armLF0);
        ModelRenderer armLF1 = new ModelRenderer(this);
        armLF1.setPos(-0.5F, 2.0F, 0.0F);
        armLF1.zRot = 0.1F;
        armLF1.texOffs(12, 29).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F, 0.0F);
        armLF1.texOffs(16, 29).addBox(-0.5F, 3.0F, -0.5F, 1.0F, 1.0F, 1.0F, 0.3F);
        armLHand.addChild(armLF1);
        ModelRenderer armLF2 = new ModelRenderer(this);
        armLF2.setPos(0.5F, 2.0F, 0.0F);
        armLF2.zRot = -0.1F;
        armLF2.texOffs(12, 29).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F, 0.0F);
        armLF2.texOffs(16, 29).addBox(-0.5F, 3.0F, -0.5F, 1.0F, 1.0F, 1.0F, 0.3F);
        armLHand.addChild(armLF2);
        ModelRenderer armLF3 = new ModelRenderer(this);
        armLF3.setPos(1.5F, 2.0F, 0.0F);
        armLF3.zRot = -0.3F;
        armLF3.texOffs(12, 29).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F, 0.0F);
        armLF3.texOffs(16, 29).addBox(-0.5F, 3.0F, -0.5F, 1.0F, 1.0F, 1.0F, 0.3F);
        armLHand.addChild(armLF3);
        body.addChild(armL);
        body.addChild(armLFore);
        body.addChild(armLHand);
        ModelRenderer legR = new ModelRenderer(this);
        legR.setPos(2.2F, 12.0F, 3.8F);
        legR.zRot = 0.15F;
        legR.texOffs(32, 29).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F, 0.0F);
        ModelRenderer legRShin = new ModelRenderer(this);
        legRShin.setPos(0.0F, 4.0F, 0.0F);
        legRShin.texOffs(44, 22).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F, 0.0F);
        legRShin.texOffs(40, 29).addBox(-1.0F, 0.5F, -1.0F, 2.0F, 4.0F, 2.0F, 0.0F);
        ModelRenderer legRFoot = new ModelRenderer(this);
        legRFoot.setPos(0.0F, 4.0F, 0.0F);
        legRFoot.texOffs(48, 29).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 3.0F, 0.0F);
        legRShin.addChild(legRFoot);
        legR.addChild(legRShin);
        body.addChild(legR);
        ModelRenderer legL = new ModelRenderer(this);
        legL.setPos(-2.2F, 12.0F, 3.8F);
        legL.zRot = -0.15F;
        legL.texOffs(32, 29).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F, 0.0F);
        ModelRenderer legLShin = new ModelRenderer(this);
        legLShin.setPos(0.0F, 4.0F, 0.0F);
        legLShin.texOffs(44, 22).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F, 0.0F);
        legLShin.texOffs(40, 29).addBox(-1.0F, 0.5F, -1.0F, 2.0F, 4.0F, 2.0F, 0.0F);
        ModelRenderer legLFoot = new ModelRenderer(this);
        legLFoot.setPos(0.0F, 4.0F, 0.0F);
        legLFoot.texOffs(48, 29).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 3.0F, 0.0F);
        legLShin.addChild(legLFoot);
        legL.addChild(legLShin);
        body.addChild(legL);
        root.addChild(body);
        // Голова лежит на плече, прямо на верху туловища (без шеи), наклонена к шее игрока и смотрит вперёд
        head = new ModelRenderer(this);
        head.setPos(7.71F, -6.86F, 6.0F);
        head.zRot = 0.3F;
        head.texOffs(0, 0).addBox(-3.0F, -3.0F, -2.5F, 6.0F, 6.0F, 5.0F, 0.8F);
        head.texOffs(22, 0).addBox(-3.0F, 0.0F, -4.5F, 6.0F, 3.0F, 2.0F, 0.8F);
        head.texOffs(38, 0).addBox(3.0F, -0.5F, -2.0F, 1.0F, 3.0F, 3.0F, 0.8F);
        head.texOffs(46, 0).addBox(-1.5F, -3.6F, -1.5F, 3.0F, 1.0F, 3.0F, 0.5F);
        root.addChild(head);
    }

    /** Лёгкое покачивание головы — стенд выглядит живым (малая амплитуда, чтобы не врезаться в игрока). */
    public void animate(float ageInTicks) {
        head.xRot = (float) Math.sin(ageInTicks * 0.08F) * 0.04F;
        head.zRot = (float) Math.sin(ageInTicks * 0.05F + 1.0F) * 0.03F;
    }

    @Override
    public void renderToBuffer(MatrixStack matrixStack, IVertexBuilder buffer, int packedLight, int packedOverlay,
            float red, float green, float blue, float alpha) {
        root.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
