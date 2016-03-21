package crazypants.enderio.jei;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import mezz.jei.Internal;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IDrawableAnimated;
import mezz.jei.api.gui.IDrawableStatic;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.BlankRecipeCategory;
import mezz.jei.api.recipe.BlankRecipeWrapper;
import mezz.jei.api.recipe.IRecipeWrapper;
import mezz.jei.gui.Focus;
import mezz.jei.gui.Focus.Mode;
import mezz.jei.gui.ingredients.GuiIngredientGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameData;
import crazypants.enderio.EnderIO;
import crazypants.enderio.Log;
import crazypants.enderio.ModObject;
import crazypants.enderio.gui.GuiContainerBaseEIO;
import crazypants.enderio.machine.IMachineRecipe;
import crazypants.enderio.machine.IMachineRecipe.ResultStack;
import crazypants.enderio.machine.MachineRecipeInput;
import crazypants.enderio.machine.MachineRecipeRegistry;
import crazypants.enderio.machine.painter.GuiPainter;
import crazypants.enderio.machine.painter.recipe.BasicPainterTemplate;
import crazypants.enderio.machine.power.PowerDisplayUtil;

import static crazypants.util.NbtValue.SOURCE_BLOCK;
import static crazypants.util.NbtValue.SOURCE_META;

public class PainterRecipeCategory extends BlankRecipeCategory {

  public static final @Nonnull String UID = "Painter";

  // ------------ Recipes

  private static List<PainterRecipeWrapper> splitRecipe(@Nonnull BasicPainterTemplate<?> recipe, List<ItemStack> validItems) {
    List<PainterRecipeWrapper> recipes = new ArrayList<PainterRecipeWrapper>();
    
    for (ItemStack target : validItems) {
      if (recipe.isValidTarget(target) && target != null) {
        MachineRecipeInput recipeInput0 = new MachineRecipeInput(0, target);
        List<ItemStack> results = new ArrayList<ItemStack>();
        List<ItemStack> paints = new ArrayList<ItemStack>();
        for (ItemStack paint : validItems) {
          try {
            MachineRecipeInput recipeInput1 = new MachineRecipeInput(1, paint);
            if (recipe.isRecipe(recipeInput0, recipeInput1)) {
              ResultStack[] recipeResults = recipe.getCompletedResult(1f, recipeInput0, recipeInput1);
              for (ResultStack result : recipeResults) {
                results.add(result.item);
                paints.add(paint);
              }
            }
          } catch (Exception e) {
            Log.warn("Error while accessing item '" + paint + "': " + e);
            e.printStackTrace();
          }
        }

        if (!results.isEmpty()) {
          recipes.add(new PainterRecipeWrapper(recipe, target, paints, results));
        }
      }
    }

    return recipes;
  }

  public static class PainterRecipeWrapper extends BlankRecipeWrapper {

    private final int energyRequired;
    private final @Nonnull ItemStack target;
    private final @Nonnull List<ItemStack> paints;
    private final @Nonnull List<ItemStack> results;

    public PainterRecipeWrapper(@Nonnull BasicPainterTemplate<?> recipe, @Nonnull ItemStack target, @Nonnull List<ItemStack> paints,
        @Nonnull List<ItemStack> results) {
      this.energyRequired = recipe.getEnergyRequired();
      this.target = target;
      this.paints = paints;
      this.results = results;
    }

    public long getEnergyRequired() { 
      return energyRequired;
    }

    @Override
    public @Nonnull List<?> getInputs() {
      List<ItemStack> list = new ArrayList<ItemStack>(paints);
      list.add(target);
      return list;
    }

    @Override
    public @Nonnull List<?> getOutputs() {
      return results;
    }   
    
    @Override
    public void drawInfo(@Nonnull Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {           
      String energyString = PowerDisplayUtil.formatPower(energyRequired) + " " + PowerDisplayUtil.abrevation();
      minecraft.fontRendererObj.drawString(energyString, 6, 36, 0x808080, false);    
      GlStateManager.color(1,1,1,1);      
    }
  }
 
  public static void register(IModRegistry registry, IGuiHelper guiHelper) {
    long start = crazypants.util.Profiler.client.start_always();
    registry.addRecipeCategories(new PainterRecipeCategory(guiHelper));
    registry.addRecipeHandlers(new BaseRecipeHandler<PainterRecipeWrapper>(PainterRecipeWrapper.class, PainterRecipeCategory.UID));
    registry.addRecipeClickArea(GuiPainter.class, 155, 42, 16, 16, PainterRecipeCategory.UID);
    
    List<PainterRecipeWrapper> result = new ArrayList<PainterRecipeWrapper>(); 
    List<ItemStack> validItems = getValidItems();
    Map<String, IMachineRecipe> recipes = MachineRecipeRegistry.instance.getRecipesForMachine(ModObject.blockPainter.unlocalisedName);
    for (IMachineRecipe recipe : recipes.values()) {
      if (recipe instanceof BasicPainterTemplate) {
        result.addAll(splitRecipe((BasicPainterTemplate) recipe, validItems));
      }
    }
    if (!result.isEmpty()) {
      registry.addRecipes(result);
    }
    crazypants.util.Profiler.client.stop(start, "JEI: Painter Recipes");
  }

  // ------------ Category

  //Offsets from full size gui, makes it much easier to get the location correct
  private final static int xOff = 34;
  private final static int yOff = 28;
  
  @Nonnull
  private final IDrawable background;

  @Nonnull
  protected final IDrawableAnimated arror;
  
  public PainterRecipeCategory(IGuiHelper guiHelper) {
    ResourceLocation backgroundLocation = GuiContainerBaseEIO.getGuiTexture("painter");
    background = guiHelper.createDrawable(backgroundLocation, xOff, yOff, 120, 50);

    IDrawableStatic flameDrawable = guiHelper.createDrawable(backgroundLocation, 176, 14, 24, 16);
    arror = guiHelper.createAnimatedDrawable(flameDrawable, 200, IDrawableAnimated.StartDirection.LEFT, false);
  }

  @Override
  public @Nonnull String getUid() {
    return UID;
  }

  @Override
  public @Nonnull String getTitle() {
    String localizedName = EnderIO.blockPainter.getLocalizedName();
    return localizedName != null ? localizedName : "ERROR";
  }

  @Override
  public @Nonnull IDrawable getBackground() {
    return background;
  }

  @Override
  public void drawAnimations(@Nonnull Minecraft minecraft) {
    arror.draw(minecraft, 88 - xOff, 34 - yOff);
  }  
  
  @Override
  public void setRecipe(@Nonnull IRecipeLayout recipeLayout, @Nonnull IRecipeWrapper recipeWrapper) {
    if(recipeWrapper instanceof PainterRecipeWrapper) {
      PainterRecipeWrapper currentRecipe = (PainterRecipeWrapper) recipeWrapper;

      IGuiItemStackGroup guiItemStacks = recipeLayout.getItemStacks();
      guiItemStacks.init(0, true, 67 - xOff - 1, 34 - yOff - 1);
      guiItemStacks.init(1, true, 38 - xOff - 1, 34 - yOff - 1);
      guiItemStacks.init(2, false, 121 - xOff - 1, 34 - yOff - 1);

      guiItemStacks.set(0, currentRecipe.target);

      // Not very nice, but the only way to get correct painting recipes into JEI, it seems.
      if (guiItemStacks instanceof GuiIngredientGroup) {
        try {
          List<ItemStack> paints = new ArrayList<ItemStack>();
          List<ItemStack> results = new ArrayList<ItemStack>();

          Field focusField = GuiIngredientGroup.class.getDeclaredField("focus");
          focusField.setAccessible(true);
          Focus focus = (Focus) focusField.get(guiItemStacks);
          if (focus != null && !focus.isBlank() && focus.getMode() != Mode.NONE) {
            ItemStack focused = focus.getStack();
            if (focus.getMode() == Mode.OUTPUT) {
              // JEI is focusing on the output item. Limit the recipe to only the paints that actually give this output item. Needs some extra comparison
              // because we told JEI to ignore paint information, which is ok for crafting and soul binding, but not here.
              for (int i = 0; i < currentRecipe.paints.size(); i++) {
                ItemStack resultStack = currentRecipe.results.get(i);
                ItemStack paintStack = currentRecipe.paints.get(i);
                if (Internal.getStackHelper().isEquivalent(focused, resultStack) && SOURCE_BLOCK.getString(focused).equals(SOURCE_BLOCK.getString(resultStack))
                    && SOURCE_META.getInt(focused) == SOURCE_META.getInt(resultStack)) {
                  paints.add(paintStack);
                  results.add(resultStack);
                }
              }
            } else if (!Internal.getStackHelper().isEquivalent(focused, currentRecipe.target)) {
              // JEI is focusing on the paint. Limit the output items to things that are painted with this paint.
              for (int i = 0; i < currentRecipe.paints.size(); i++) {
                ItemStack resultStack = currentRecipe.results.get(i);
                ItemStack paintStack = currentRecipe.paints.get(i);
                if (Internal.getStackHelper().isEquivalent(focused, paintStack)) {
                  paints.add(paintStack);
                  results.add(resultStack);
                }
              }
            } else {
              // JEI is focusing on a paintable item. If that item also can be used as a paint source, it will display "item+item=anything", which is somewhere
              // between weird and wrong. So remove the recipe "item+item" from the list to get "anything+item=anything".
              for (int i = 0; i < currentRecipe.paints.size(); i++) {
                ItemStack resultStack = currentRecipe.results.get(i);
                ItemStack paintStack = currentRecipe.paints.get(i);
                if (!Internal.getStackHelper().isEquivalent(focused, paintStack)) {
                  paints.add(paintStack);
                  results.add(resultStack);
                }
              }
            }
            if (!paints.isEmpty()) {
              guiItemStacks.set(1, paints);
              guiItemStacks.set(2, results);
              return;
            }
          }
        } catch (Throwable t) {
        }
      }

      guiItemStacks.set(1, currentRecipe.paints);
      guiItemStacks.set(2, currentRecipe.results);
    }
  }
  
  private static @Nonnull List<ItemStack> getValidItems() {
    List<ItemStack> list = new ArrayList<ItemStack>();
    for (Item item : GameData.getItemRegistry()) {
      for (CreativeTabs tab : item.getCreativeTabs()) {
        item.getSubItems(item, tab, list);
      }
    }
    return list;
  }

}
