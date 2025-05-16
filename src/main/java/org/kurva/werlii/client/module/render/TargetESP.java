package org.kurva.werlii.client.module.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.kurva.werlii.client.module.Module;
import org.kurva.werlii.client.module.combat.KillAura;
import org.lwjgl.glfw.GLFW;
import org.joml.Matrix4f;

public class TargetESP extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private static TargetESP instance;

    public TargetESP() {
        super("TargetESP", "Рисует красный кружок на цели KillAura", Category.RENDER);
        instance = this;
        this.setKeyCode(GLFW.GLFW_KEY_U);
        this.registerKeybinding("Werlii Render");
    }

    public static TargetESP getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    public void onRender2D(DrawContext context) {
        if (mc.player == null || mc.world == null) return;

        KillAura killAura = KillAura.getInstance();
        if (killAura == null || !killAura.isEnabled()) return;

        LivingEntity target = killAura.getTarget();
        if (target == null || !target.isAlive()) return;

        Vec3d screenPos = worldToScreen(target.getX(), target.getEyeY(), target.getZ());
        if (screenPos == null) return;

        drawCircle(context, (float) screenPos.x, (float) screenPos.y, 5.0f);
    }

    private Vec3d worldToScreen(double x, double y, double z) {
        Vec3d pos = new Vec3d(x, y, z);
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        pos = pos.subtract(cameraPos);

        Matrix4f projectionMatrix = new Matrix4f(mc.gameRenderer.getBasicProjectionMatrix(mc.options.getFov().getValue()));
        Matrix4f modelViewMatrix = new Matrix4f().setLookAt(
                (float) cameraPos.x, (float) cameraPos.y, (float) cameraPos.z,
                (float) (cameraPos.x + mc.player.getRotationVec(1.0f).x),
                (float) (cameraPos.y + mc.player.getRotationVec(1.0f).y),
                (float) (cameraPos.z + mc.player.getRotationVec(1.0f).z),
                0, 1, 0
        );

        float[] vec = new float[]{(float) pos.x, (float) pos.y, (float) pos.z, 1.0f};
        float[] modelViewResult = new float[4];
        modelViewMatrix.get(modelViewResult);
        float[] transformed = new float[4];
        for (int i = 0; i < 4; i++) {
            transformed[i] = vec[0] * modelViewResult[i] +
                    vec[1] * modelViewResult[i + 4] +
                    vec[2] * modelViewResult[i + 8] +
                    vec[3] * modelViewResult[i + 12];
        }
        vec = transformed;
        float[] projectionResult = new float[4];
        projectionMatrix.get(projectionResult);
        transformed = new float[4];
        for (int i = 0; i < 4; i++) {
            transformed[i] = vec[0] * projectionResult[i] +
                    vec[1] * projectionResult[i + 4] +
                    vec[2] * projectionResult[i + 8] +
                    vec[3] * projectionResult[i + 12];
        }
        vec = transformed;

        if (vec[3] <= 0.0f) return null;

        float w = vec[3];
        float screenX = (vec[0] / w + 1.0f) * 0.5f * mc.getWindow().getScaledWidth();
        float screenY = (1.0f - vec[1] / w) * 0.5f * mc.getWindow().getScaledHeight();

        if (screenX < 0 || screenX > mc.getWindow().getScaledWidth() || screenY < 0 || screenY > mc.getWindow().getScaledHeight()) {
            return null;
        }

        return new Vec3d(screenX, screenY, vec[2] / w);
    }

    private void drawCircle(DrawContext context, float x, float y, float radius) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 0.0f, 0.0f, 1.0f); // Красный цвет

        // Используем шейдер для линий
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINE_STRIP, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

        int segments = 32;
        for (int i = 0; i <= segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            float posX = x + (float) (Math.cos(angle) * radius);
            float posY = y + (float) (Math.sin(angle) * radius);
            bufferBuilder.vertex(matrix, posX, posY, 0.0f).color(1.0f, 0.0f, 0.0f, 1.0f);
        }

        RenderSystem.lineWidth(1.0f);
        BuiltBuffer builtBuffer = bufferBuilder.end();
        BufferRenderer.draw(builtBuffer);
        builtBuffer.close();
        RenderSystem.lineWidth(1.0f);
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
}