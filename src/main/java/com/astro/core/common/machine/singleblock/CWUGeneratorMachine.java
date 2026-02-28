package com.astro.core.common.machine.singleblock;

import com.astro.core.common.machine.trait.cwu.ILocalCWUProvider;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IUIMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableEnergyContainer;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import lombok.Getter;


import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CWUGeneratorMachine extends MetaMachine implements ILocalCWUProvider, IUIMachine {

    // ── Tier tables (indices 0–4: ULV..EV) ────────────────────────────────────
    private static final int[] CWU_PER_TICK   = { 0, 0,  2,  4,  8 };
    private static final int[] LUBE_PER_CYCLE = { 0, 0, 10, 40, 80 };
    private static final int   LUBE_TANK_MB   = 2000;

    // ── Static helpers called by AstroSingleBlocks during registration ─────────

    public static long getEUtForTier(int tier) {
        return GTValues.VA[tier];
    }

    public static int getCWUForTier(int tier) {
        return tier < CWU_PER_TICK.length ? CWU_PER_TICK[tier] : 0;
    }

    public static int getLubricantForTier(int tier) {
        int perCycle = tier < LUBE_PER_CYCLE.length ? LUBE_PER_CYCLE[tier] : 0;
        return perCycle / 20;
    }

    // ── Instance fields ────────────────────────────────────────────────────────

    private final int  tier;
    private final int  cwuPerTick;
    private final int  lubePerCycle;
    private final long euPerTick;

    private int tickBudget = 0;

    @Getter
    private boolean providerActive = false;

    @Nullable
    private TickableSubscription tickSub;

    private final NotifiableEnergyContainer energyContainer;
    private final NotifiableFluidTank       lubeTank;

    // ── Constructor ────────────────────────────────────────────────────────────

    public CWUGeneratorMachine(IMachineBlockEntity holder, int tier) {

        super(holder);
        this.tier         = tier;
        this.cwuPerTick   = getCWUForTier(tier);
        this.lubePerCycle = tier < LUBE_PER_CYCLE.length ? LUBE_PER_CYCLE[tier] : 0;
        this.euPerTick    = GTValues.VA[tier];

        this.energyContainer = NotifiableEnergyContainer.receiverContainer(
                this,
                GTValues.V[tier] * 64L,
                GTValues.V[tier],
                2L);

        this.lubeTank = new NotifiableFluidTank(this, 1, LUBE_TANK_MB, IO.IN, IO.NONE);
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) {
            tickSub = subscribeServerTick(this::tickMachine);
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (tickSub != null) {
            tickSub.unsubscribe();
            tickSub = null;
        }
    }

    // ── Tick logic ─────────────────────────────────────────────────────────────

    private void tickMachine() {
        if (isRemote()) return;

        // Refresh per-tick budget based on last cycle's active state.
        tickBudget = providerActive ? cwuPerTick : 0;

        // Every 20 ticks: consume resources for the next cycle.
        if (getOffsetTimer() % 20 == 0) {
            boolean hasEnergy = energyContainer.getEnergyStored() >= euPerTick * 20L;
            boolean hasLube   = lubeTank.getFluidInTank(0).getAmount() >= lubePerCycle;

            if (hasEnergy && hasLube) {
                energyContainer.removeEnergy(euPerTick * 20L);
                lubeTank.drain(
                        new FluidStack(getLubricantFluid(), lubePerCycle),
                        IFluidHandler.FluidAction.EXECUTE);
                providerActive = true;
                tickBudget     = cwuPerTick;
            } else {
                providerActive = false;
                tickBudget     = 0;
            }
        }
    }

    private net.minecraft.world.level.material.Fluid getLubricantFluid() {
        return com.gregtechceu.gtceu.common.data.GTMaterials.Lubricant.getFluid();
    }

    // ── ILocalCWUProvider ──────────────────────────────────────────────────────

    @Override
    public int requestCWUt(int cwut, boolean simulate) {
        if (!providerActive) return 0;
        int provided = Math.min(cwut, tickBudget);
        if (!simulate) tickBudget -= provided;
        return provided;
    }

    @Override
    public int getMaxCWUt() {
        return cwuPerTick;
    }

    // ── GUI ────────────────────────────────────────────────────────────────────

    @Override
    public ModularUI createUI(Player player) {
        return new ModularUI(176, 80, this, player)
                .widget(new LabelWidget(8, 8,
                        Component.translatable("astrogreg.machine.cwu_generator.tooltip.0").getString()))
                .widget(new LabelWidget(8, 20, () ->
                        Component.translatable("astrogreg.machine.cwu_generator.producing",
                                providerActive ? cwuPerTick : 0).getString()))
                .widget(new LabelWidget(8, 32, () ->
                        Component.translatable("astrogreg.machine.cwu_generator.lubricant",
                                getLubricantForTier(tier)).getString()))
                .widget(new LabelWidget(8, 44, () ->
                        Component.translatable("astrogreg.machine.cwu_generator.available",
                                tickBudget).getString()));
    }
}