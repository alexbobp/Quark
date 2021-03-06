package vazkii.quark.management.feature;

import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiShulkerBox;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import vazkii.quark.api.IItemSearchBar;
import vazkii.quark.base.module.Feature;
import vazkii.quark.management.client.gui.GuiButtonChest;

public class ChestSearchBar extends Feature {

	static String text = "";
	GuiTextField searchBar;
	boolean skip;
	boolean moveToCenterBar;
	
	private long lastClick;
	private int matched;
	
	@Override
	public void setupConfig() {
		boolean invtweaks = loadPropBool("Avoid Invtweaks Buttons", "Automatically move the search bar if Inventory Tweaks is loaded so it doesn't end up in the same place as their buttons.", true);
		moveToCenterBar = loadPropBool("Move to Center Bar", "Set to true to move to the center bar, next to the \"Inventory\" text.", false);
		moveToCenterBar |= (invtweaks && Loader.isModLoaded("inventorytweaks"));
	}
	
	@SubscribeEvent
	public void initGui(GuiScreenEvent.InitGuiEvent.Post event) {
		GuiScreen gui = event.getGui();
		boolean callback = gui instanceof IItemSearchBar;
		if(callback || gui instanceof GuiChest || gui instanceof GuiShulkerBox) {
			GuiContainer chest = (GuiContainer) gui;
			searchBar = new GuiTextField(12831, gui.mc.fontRenderer, chest.getGuiLeft() + 81, chest.getGuiTop() + 6, 88, 10);
			if(moveToCenterBar)
				searchBar.y = chest.getGuiTop() + chest.getYSize() - 95;
			
			searchBar.setText(text);
			searchBar.setFocused(false);
			searchBar.setMaxStringLength(32);
			searchBar.setEnableBackgroundDrawing(false);
			
			if(callback)
				((IItemSearchBar) gui).onSearchBarAdded(searchBar);
		} else searchBar = null;
	}
	
	@SubscribeEvent
	public void onKeypress(GuiScreenEvent.KeyboardInputEvent.Pre event) {
		if(searchBar != null && searchBar.isFocused() && Keyboard.getEventKeyState()) {
	        char eventChar = Keyboard.getEventCharacter();
	        int eventCode = Keyboard.getEventKey();
	        
			searchBar.textboxKeyTyped(eventChar, eventCode);
			text = searchBar.getText();
			
			event.setCanceled(eventCode != 1);
		}
	}
	
	@SubscribeEvent
	public void onMouseclick(GuiScreenEvent.MouseInputEvent.Pre event) {
		if(searchBar != null && Mouse.getEventButtonState()) {
			Minecraft mc = Minecraft.getMinecraft();
			GuiScreen gui = event.getGui();
			
	        int x = Mouse.getEventX() * gui.width / mc.displayWidth;
	        int y = gui.height - Mouse.getEventY() * gui.height / mc.displayHeight - 1;
			int button = Mouse.getEventButton();
			
			searchBar.mouseClicked(x, y, button);
			
			long time = System.currentTimeMillis();
			long delta = time - lastClick;
			if(delta < 200 && searchBar.isFocused()) {
				searchBar.setText("");
				text = "";
			}
			
			lastClick = time;
		}
	}
	
	@SubscribeEvent
	public void onRender(GuiScreenEvent.DrawScreenEvent.Post event) {
		if(searchBar != null && !skip)
			renderElements(event.getGui());
		skip = false;
	}
	
	@SubscribeEvent
	public void drawTooltipEvent(RenderTooltipEvent.Pre event) {
		if(searchBar != null) {
			renderElements(Minecraft.getMinecraft().currentScreen);
			skip = true;
		}
	}
	
	private void renderElements(GuiScreen gui) {
		drawBackground(gui, searchBar.x - 2, searchBar.y - 2);
		
		if(!text.isEmpty()) {
			if(gui instanceof GuiContainer) {
				GuiContainer guiContainer = (GuiContainer) gui;
				Container container = guiContainer.inventorySlots;
				
				int guiLeft = guiContainer.getGuiLeft();
				int guiTop = guiContainer.getGuiTop();
				
				matched = 0;
				for(Slot s : container.inventorySlots) {
					ItemStack stack = s.getStack();
					if(!namesMatch(stack, text)) {
						int x = guiLeft + s.xPos;
						int y = guiTop + s.yPos;
						
						GlStateManager.disableDepth();
						guiContainer.drawRect(x, y, x + 16, y + 16, 0xAA000000);
					} else matched++;
				}
			}
		}
		
		if(matched == 0 && !text.isEmpty())
			searchBar.setTextColor(0xFF5555);
		else searchBar.setTextColor(0xFFFFFF);
		searchBar.drawTextBox();
	}
	
	private void drawBackground(GuiScreen gui, int x, int y) {
		if(gui instanceof IItemSearchBar && ((IItemSearchBar) gui).renderBackground(x, y))
			return;
		
		GlStateManager.color(1F, 1F, 1F, 1F);
		GlStateManager.disableLighting();
		gui.mc.getTextureManager().bindTexture(GuiButtonChest.GENERAL_ICONS_RESOURCE);
		Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 244, 90, 12, 256, 256);
	}
	
	private boolean namesMatch(ItemStack stack, String search) {
		if(stack.isEmpty())
			return false;
		
		String name = stack.getDisplayName();
		search = TextFormatting.getTextWithoutFormattingCodes(search.trim().toLowerCase());
		name = TextFormatting.getTextWithoutFormattingCodes(name.trim().toLowerCase());
		
		StringMatcher matcher = (s1, s2) -> s1.contains(s2);
		
		if(search.length() >= 3 && search.startsWith("\"") && search.endsWith("\"")) {
			search = search.substring(1, search.length() - 1);
			matcher = (s1, s2) -> s1.equals(s2);
		}
		
		if(search.length() >= 3 && search.startsWith("/") && search.endsWith("/")) {
			search = search.substring(1, search.length() - 1);
			matcher = (s1, s2) -> Pattern.compile(s2).matcher(s1).find();
		}

		if(stack.isItemEnchanted()) {
			Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(stack);
			for(Enchantment e : enchants.keySet())
				if(matcher.matches(e.getTranslatedName(enchants.get(e)).toLowerCase(), search))
					return true;
		}
		
		if(stack.getItem() == Items.ENCHANTED_BOOK) {
			NBTTagList enchants = ItemEnchantedBook.getEnchantments(stack);
			for(int i = 0; i < enchants.tagCount(); i++) {
				NBTTagCompound cmp = enchants.getCompoundTagAt(i);
				int id = cmp.getInteger("id");
				int lvl = cmp.getInteger("lvl");
				Enchantment e = Enchantment.getEnchantmentByID(id);
				if(matcher.matches(e.getTranslatedName(lvl).toLowerCase(), search))
					return true;
			}
		}
		
		return matcher.matches(name, search);
	}
	
	@Override
	public boolean hasSubscriptions() {
		return isClient();
	}
	
	private static interface StringMatcher {
		boolean matches(String str1, String str2);
	}
	
}
