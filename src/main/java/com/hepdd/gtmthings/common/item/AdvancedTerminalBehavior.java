package com.hepdd.gtmthings.common.item;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.common.block.CoilBlock;
import com.gregtechceu.gtceu.common.item.TerminalBehavior;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.hepdd.gtmthings.api.gui.widget.TerminalInputWidget;
import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.factory.HeldItemUIFactory;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.hepdd.gtmthings.api.pattern.AdvancedBlockPattern.getAdvancedBlockPattern;

public class AdvancedTerminalBehavior extends TerminalBehavior {

    @Persisted
    private AutoBuildSetting autoBuildSetting;
    private ItemStack itemStack;

    public AdvancedTerminalBehavior() {
        autoBuildSetting = new AutoBuildSetting();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            Level level = context.getLevel();
            BlockPos blockPos = context.getClickedPos();
            if (context.getPlayer() != null &&
                    MetaMachine.getMachine(level, blockPos) instanceof IMultiController controller) {
                if (!controller.isFormed()) {
                    if (!level.isClientSide) {
                        getAdvancedBlockPattern(controller.getPattern()).autoBuild(context.getPlayer(), controller.getMultiblockState(),autoBuildSetting);
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public ModularUI createUI(HeldItemUIFactory.HeldItemHolder holder, Player entityPlayer) {
        return new ModularUI(176, 166, holder, entityPlayer).widget(createWidget());
    }

    private Widget createWidget() {
        var group = new WidgetGroup(0, 0, 182 + 8, 117 + 8);
        int rowIndex = 1;
        group.addWidget(
                new DraggableScrollableWidgetGroup(4, 4, 182, 117)
                        .setBackground(GuiTextures.DISPLAY)
                        .setYScrollBarWidth(2)
                        .setYBarStyle(null, ColorPattern.T_WHITE.rectTexture().setRadius(1))
                        .addWidget(new LabelWidget(40, 5, Component.translatable("item.gtmthings.advanced_terminal.setting.title").getString()))
                        .addWidget(new LabelWidget(4, 5 + 16 * rowIndex, Component.translatable("item.gtmthings.advanced_terminal.setting.1"))
                                .setHoverTooltips(Component.translatable("item.gtmthings.advanced_terminal.setting.1.tooltip")))
                        .addWidget(new TerminalInputWidget(140, 5 + 16 * rowIndex++, 20, 16, autoBuildSetting::getCoilTier,
                                this::setCoilTier)
                                .setMin(0).setMax(GTCEuAPI.HEATING_COILS.size() - 1))
                        .addWidget(new LabelWidget(4, 5 + 16 * rowIndex, Component.translatable("item.gtmthings.advanced_terminal.setting.2"))
                                .setHoverTooltips(Component.translatable("item.gtmthings.advanced_terminal.setting.2.tooltip")))
                        .addWidget(new TerminalInputWidget(140, 5 + 16 * rowIndex++, 20, 16, autoBuildSetting::getRepeatCount,
                                this::setRepeatCount)
                                .setMin(0).setMax(99))
                        .addWidget(new LabelWidget(4, 5 + 16 * rowIndex, Component.translatable("item.gtmthings.advanced_terminal.setting.3"))
                                .setHoverTooltips(Component.translatable("item.gtmthings.advanced_terminal.setting.3.tooltip")))
                        .addWidget(new TerminalInputWidget(140, 5 + 16 * rowIndex++, 20, 16, autoBuildSetting::getNoHatchMode,
                                this::setIsBuildHatches).setMin(0).setMax(1)));
        group.setBackground(GuiTextures.BACKGROUND_INVERSE);
        return group;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Item item, Level level, Player player, InteractionHand usedHand) {
        if (!ConfigHolder.INSTANCE.gameplay.enableCompass) {
            ItemStack heldItem = player.getItemInHand(usedHand);
            if (player instanceof ServerPlayer serverPlayer) {
                if (this.itemStack == null){
                    this.itemStack = heldItem;
                    var tag = this.itemStack.getTag();
                    if (tag!=null && !tag.isEmpty()) {
                        this.autoBuildSetting.setCoilTier(tag.getInt("CoilTier"));
                        this.autoBuildSetting.setRepeatCount(tag.getInt("RepeatCount"));
                        this.autoBuildSetting.setNoHatchMode(tag.getInt("NoHatchMode"));
                    } else {
                        this.autoBuildSetting.setCoilTier(0);
                        this.autoBuildSetting.setRepeatCount(0);
                        this.autoBuildSetting.setNoHatchMode(0);
                    }
                }
                HeldItemUIFactory.INSTANCE.openUI(serverPlayer, usedHand);
            }
            return InteractionResultHolder.success(heldItem);
        }

        return super.use(item, level, player, usedHand);
    }

    private void setCoilTier(int coilTier) {
        autoBuildSetting.setCoilTier(coilTier);
        var tag = this.itemStack.getTag();
        if (tag == null) tag = new CompoundTag();
        tag.putInt("CoilTier",coilTier);
        this.itemStack.setTag(tag);
    }

    private void setRepeatCount(int repeatCount) {
        autoBuildSetting.setRepeatCount(repeatCount);
        var tag = this.itemStack.getTag();
        if (tag == null) tag = new CompoundTag();
        tag.putInt("RepeatCount",repeatCount);
        this.itemStack.setTag(tag);
    }

    private void setIsBuildHatches(int isBuildHatches) {
        autoBuildSetting.setNoHatchMode(isBuildHatches);
        var tag = this.itemStack.getTag();
        if (tag == null) tag = new CompoundTag();
        tag.putInt("NoHatchMode",isBuildHatches);
        this.itemStack.setTag(tag);
    }

    public static class AutoBuildSetting {

        final String[] HATCH_NAMES = {"input_hatch","output_hatch","input_bus","output_bus","laser_target","laser_source",
            "transmitter_hatch","receiver_hatch","maintenance_hatch","parallel_hatch"};

        @Getter
        @Setter
        private int coilTier, repeatCount, noHatchMode;

        public AutoBuildSetting() {
            this.coilTier = 0;
            this.repeatCount = 0;
            this.noHatchMode = 0;
        }

        public List<ItemStack> apply(BlockInfo[] blockInfos) {
            List<ItemStack> candidates = new ArrayList<>();
            if (blockInfos != null) {
                if (Arrays.stream(blockInfos).anyMatch(
                        info -> info.getBlockState().getBlock() instanceof CoilBlock)) {
                    var tier = Math.max(coilTier, blockInfos.length - 1);
                    candidates.add(blockInfos[tier].getItemStackForm());
                    for (int i = 0; i < tier; i++) {
                        candidates.add(blockInfos[i].getItemStackForm());
                    }
                    for (int i = tier + 1; i < blockInfos.length - 1; i++) {
                        candidates.add(blockInfos[i].getItemStackForm());
                    }
                    return candidates;
                }
                for (BlockInfo info : blockInfos) {
                    if (info.getBlockState().getBlock() != Blocks.AIR) candidates.add(info.getItemStackForm());
                }
            }
            return candidates;
        }

        public boolean isPlaceHatch(BlockInfo[] blockInfos) {

            if (this.noHatchMode == 0) return true;
            if (blockInfos != null && blockInfos.length > 0) {
                var blockInfo = blockInfos[0];
                if (blockInfo.getBlockState().getBlock() instanceof MetaMachineBlock machineBlock) {
                    var id = machineBlock.getDefinition().getDescriptionId();
                    return Arrays.stream(HATCH_NAMES).noneMatch(id::contains);
                }
                return true;
            }
            return true;
        }

    }
}
