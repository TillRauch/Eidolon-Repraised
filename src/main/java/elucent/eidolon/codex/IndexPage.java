package elucent.eidolon.codex;

import elucent.eidolon.Eidolon;
import elucent.eidolon.api.deity.Deity;
import elucent.eidolon.api.research.Research;
import elucent.eidolon.api.spells.Sign;
import elucent.eidolon.capability.IReputation;
import elucent.eidolon.util.KnowledgeUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IndexPage extends Page {
    public static final ResourceLocation BACKGROUND = new ResourceLocation(Eidolon.MODID, "textures/gui/codex_index_page.png");
    final List<IndexEntry> entries = new ArrayList<>();

    public static class IndexEntry {
        final Chapter chapter;
        final ItemStack icon;
        boolean alwaysRender = false;

        public IndexEntry(Chapter chapter, ItemStack icon) {
            this.chapter = chapter;
            this.icon = icon;
        }

        public IndexEntry(Chapter chapter, ItemStack icon, boolean alwaysRender) {
            this.chapter = chapter;
            this.icon = icon;
            this.alwaysRender = alwaysRender;
        }

        @OnlyIn(Dist.CLIENT)
        public boolean isUnlocked() {
            return true;
        }
    }

    public static class SignLockedEntry extends IndexEntry {
        final Sign[] signs;

        public SignLockedEntry(Chapter chapter, ItemStack icon, Sign... signs) {
            super(chapter, icon);
            this.signs = signs;
        }

        @Override
        public boolean isUnlocked() {
            for (Sign sign : signs) if (!KnowledgeUtil.knowsSign(Eidolon.proxy.getPlayer(), sign)) return false;
            return true;
        }
    }

    public static class FactLockedEntry extends IndexEntry {
        final ResourceLocation[] facts;

        public FactLockedEntry(Chapter chapter, ItemStack icon, ResourceLocation... facts) {
            super(chapter, icon);
            this.facts = facts;
        }

        @Override
        public boolean isUnlocked() {
            return Arrays.stream(facts).allMatch((fact) -> KnowledgeUtil.knowsFact(Eidolon.proxy.getPlayer(), fact));
        }
    }

    public static class ResearchLockedEntry extends IndexEntry {
        final Research[] researchs;

        public ResearchLockedEntry(Chapter chapter, ItemStack icon, Research... researchs) {
            super(chapter, icon);
            this.researchs = researchs;
        }

        @Override
        public boolean isUnlocked() {
            return Arrays.stream(researchs).allMatch((research) -> KnowledgeUtil.knowsResearch(Eidolon.proxy.getPlayer(), research.getRegistryName()));
        }
    }

    public static class ReputationLockedEntry extends IndexEntry {
        final int reputation;
        final ResourceLocation deity;

        public ReputationLockedEntry(Chapter chapter, ItemStack icon, int reputation, Deity deity) {
            super(chapter, icon);
            this.reputation = reputation;
            this.deity = deity.getId();
        }

        @Override
        public boolean isUnlocked() {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return true;
            return server.overworld().getCapability(IReputation.INSTANCE).resolve().get().getReputation(Eidolon.proxy.getPlayer(), deity) >= reputation;
        }
    }

    public IndexPage(IndexEntry... pages) {
        super(BACKGROUND);
        this.entries.addAll(List.of(pages));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean click(CodexGui gui, int x, int y, int mouseX, int mouseY) {
        for (int i = 0; i < entries.size(); i++)
            if (entries.get(i).isUnlocked()) {
                if (mouseX >= x + 2 && mouseX <= x + 124 && mouseY >= y + 8 + i * 20 && mouseY <= y + 26 + i * 20) {
                    gui.changeChapter(entries.get(i).chapter);
                    assert Minecraft.getInstance().player != null;
                    Minecraft.getInstance().player.playNotifySound(SoundEvents.UI_BUTTON_CLICK.get(), SoundSource.NEUTRAL, 1.0f, 1.0f);
                    return true;
                }
            }
        return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void render(CodexGui gui, @NotNull GuiGraphics mStack, ResourceLocation bg, int x, int y, int mouseX, int mouseY) {
        for (int i = 0; i < entries.size(); i++) {
            IndexEntry entry = entries.get(i);
            boolean unlocked = entry.isUnlocked();
            if (entry.alwaysRender || unlocked)
                mStack.blit(bg, x + 1, y + 7 + i * 20, 128, unlocked ? 0 : 96, 122, 18);
            if (unlocked) {
                mStack.renderItem(entry.icon, x + 2, y + 8 + i * 20);
                drawText(mStack, I18n.get(entry.chapter.titleKey), x + 24, y + 20 + i * 20 - Minecraft.getInstance().font.lineHeight);
            }
        }
    }
}
