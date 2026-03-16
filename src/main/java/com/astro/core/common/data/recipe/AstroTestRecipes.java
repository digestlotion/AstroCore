package com.astro.core.common.data.recipe;

import com.astro.core.common.data.machine.conditions.PlanetaryResearchCondition;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.*;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.*;

public class AstroTestRecipes {

    public static final String TEST_PLANET_ID = "astrogreg:test_planet";

    public static void init(Consumer<FinishedRecipe> provider) {
        AstroRecipeTypes.ASTROPORT_RECIPES.recipeBuilder("test_rocket_frame")
                .inputItems(ChemicalHelper.get(plate, GTMaterials.Steel, 4))
                .inputItems(ChemicalHelper.get(rod, GTMaterials.Steel, 4))
                .inputItems(GTItems.ELECTRIC_MOTOR_HV.asStack())
                .outputItems(Items.DIAMOND)
                .duration(400).EUt(VA[HV])
                .stationResearch(b -> b
                        .researchStack(ChemicalHelper.get(plate, GTMaterials.Steel))
                        .CWUt(16)
                        .EUt(VA[IV]))
                .addCondition(new PlanetaryResearchCondition().setPlanetId(TEST_PLANET_ID))
                .addData(PlanetaryResearchCondition.RECIPE_DATA_KEY, TEST_PLANET_ID)
                .save(provider);
    }
}