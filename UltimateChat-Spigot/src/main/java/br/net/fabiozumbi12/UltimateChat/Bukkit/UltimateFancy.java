package br.net.fabiozumbi12.UltimateChat.Bukkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import br.net.fabiozumbi12.UltimateChat.Bukkit.UCLogger.timingType;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**Class to generate JSON elements to use with UltimateChat.
 * @author FabioZumbi12
 *
 */
public class UltimateFancy {

	private ChatColor lastColor = ChatColor.WHITE;
	private JsonArray constructor;
	private HashMap<String, Boolean> lastformats;
	private List<JsonObject> workingGroup;
	private List<ExtraElement> pendentElements;
	
	public UltimateFancy(){
		constructor = new JsonArray();
		workingGroup = new ArrayList<JsonObject>();
		lastformats = new HashMap<String, Boolean>();
		pendentElements = new ArrayList<ExtraElement>();
	}
	
	public UltimateFancy(String text){
		constructor = new JsonArray();
		workingGroup = new ArrayList<JsonObject>();
		lastformats = new HashMap<String, Boolean>();
		pendentElements = new ArrayList<ExtraElement>();
		text(text);
	}

	public UltimateFancy text(String text){
		for (String part:text.split("(?="+ChatColor.COLOR_CHAR+")")){
			JsonObject workingText = new JsonObject();	
			
			//fix colors before
			filterColors(workingText);
			
			Matcher match = Pattern.compile("^"+ChatColor.COLOR_CHAR+"([0-9a-fk-or]).*$").matcher(part);
			if (match.find()){
				lastColor = ChatColor.getByChar(match.group(1).charAt(0));
				//fix colors from latest
				filterColors(workingText);
				if (part.length() == 2) continue;
			}
			//continue if empty
			if (ChatColor.stripColor(part).isEmpty()){
				continue;
			}
			workingText.addProperty("text", ChatColor.stripColor(part));
						
			//fix colors after
			filterColors(workingText);
			
			if (!workingText.has("color")){
				workingText.addProperty("color", "white");
			}
			workingGroup.add(workingText);
		}			
		return this;
	}
	
	private JsonObject filterColors(JsonObject obj){
		for (Entry<String, Boolean> format:lastformats.entrySet()){
			obj.addProperty(format.getKey(), format.getValue());
		}
		if (lastColor.isFormat()){
			lastformats.put(lastColor.name().toLowerCase(), true);
			obj.addProperty(lastColor.name().toLowerCase(), true);
		}			
		if (lastColor.isColor()){
			obj.addProperty("color", lastColor.name().toLowerCase());
		}
		if (lastColor.equals(ChatColor.RESET)){
			obj.addProperty("color", "white");
			for (String format:lastformats.keySet()){
				lastformats.put(format, false);
				obj.addProperty(format, false);
			}
		}
		return obj;
	}
	
	public void send(CommandSender to){
		next();
		if (to instanceof Player){
			if (UChat.get().getUCConfig().getBool("general.json-events")){
				UChat.get().getUCLogger().timings(timingType.END, "FancyMessage#send()|json-events:true|before tellraw");
				UCUtil.performCommand((Player)to, Bukkit.getConsoleSender(), "tellraw " + to.getName() + " " + constructor.toString());
				UChat.get().getUCLogger().timings(timingType.END, "FancyMessage#send()|json-events:true|after tellraw");
			} else {
				UChat.get().getUCLogger().timings(timingType.END, "FancyMessage#send()|json-events:false|before send");
				to.sendMessage(toOldFormat());
				UChat.get().getUCLogger().timings(timingType.END, "FancyMessage#send()|json-events:false|after send");
			}			
		} else {
			to.sendMessage(toOldFormat());
		}		
		UChat.get().getUCLogger().severe("JSON: "+constructor.toString());
	}
	
	public UltimateFancy next(){
		for (JsonObject obj:workingGroup){
			if (obj.has("text") && obj.get("text").toString().length() > 0){					
				for (ExtraElement element:pendentElements){							
					obj.add(element.getAction(), element.getJson());
				}
				constructor.add(obj);
			}
		}
		workingGroup = new ArrayList<JsonObject>();
		pendentElements = new ArrayList<ExtraElement>();		
		return this;
	}
	
	public UltimateFancy clickRunCmd(String cmd){
		pendentElements.add(new ExtraElement("clickEvent",parseJson("run_command", cmd)));
		return this;
	}
	
	public UltimateFancy clickSuggestCmd(String cmd){
		pendentElements.add(new ExtraElement("clickEvent",parseJson("suggest_command", cmd)));
		return this;
	}
	
	public UltimateFancy clickOpenURL(String url){
		pendentElements.add(new ExtraElement("clickEvent",parseJson("open_url", url)));
		return this;
	}
	
	public UltimateFancy hoverShowText(String text){
		pendentElements.add(new ExtraElement("hoverEvent",parseHoverText(text)));
		return this;
	}
	
	public UltimateFancy hoverShowItem(ItemStack item){
		pendentElements.add(new ExtraElement("hoverEvent",parseHoverItem(item)));
		return this;
	}
	
	public String toOldFormat(){
		StringBuilder result = new StringBuilder();
		for (JsonElement array:constructor){	
			JsonObject json = array.getAsJsonObject();
			if (!json.has("text")) continue;	
			//get format
			for (ChatColor frmt:ChatColor.values()){
				if (!frmt.isFormat()) continue;
				if (json.has(frmt.name().toLowerCase()) && json.get(frmt.name().toLowerCase()).getAsBoolean()){
					result.append(String.valueOf(frmt));
				}
			}
			//get color
			String colorStr = json.get("color").getAsString();
			if (ChatColor.valueOf(colorStr.toUpperCase()) != null){				
				ChatColor color = ChatColor.valueOf(colorStr.toUpperCase());
				if (color.equals(ChatColor.WHITE)){
					result.append(String.valueOf(ChatColor.RESET));
				} else {
					result.append(String.valueOf(color));
				}
			}
			result.append(json.get("text").getAsString());	
		}
		return result.toString();
	}
	
	private JsonObject parseHoverText(String text){
		JsonArray extraArr = addColorToArray(text);		
		JsonObject objExtra = new JsonObject();
		objExtra.addProperty("text", "");
		objExtra.add("extra", extraArr);
		JsonObject obj = new JsonObject();
		obj.addProperty("action", "show_text");	
		obj.add("value", objExtra);	
		return obj;
	}
	
	private JsonObject parseJson(String action, String value){
		JsonObject obj = new JsonObject();
		obj.addProperty("action", action);
		obj.addProperty("value", value);
		return obj;
	}
	/*
	private String parseHoverItemString(ItemStack item){	
		StringBuilder itemBuild = new StringBuilder();
		if (item.hasItemMeta()){
			ItemMeta itemMeta = item.getItemMeta();
			if (itemMeta.hasDisplayName()){
				itemBuild.append(itemMeta.getDisplayName()+"\n");
			} else if (itemMeta.hasLocalizedName()){
				itemBuild.append(itemMeta.getLocalizedName()+"\n");
			} else {
				itemBuild.append(capitalize(item.getType().name())+"\n");
			}			
			if (itemMeta.hasEnchants()){
				for (Entry<Enchantment, Integer> ench:itemMeta.getEnchants().entrySet()){					
					itemBuild.append("&7"+capitalize(ench.getKey().getName().replace("_", " "))+" "+ench.getValue()+"\n");
				}
			}
			if (itemMeta.hasLore()){
				for (String lore:itemMeta.getLore()){
					itemBuild.append("&5&o"+lore+"\n");
				}
			}
		} else {
			itemBuild.append(capitalize(item.getType().name())+"\n");
		}
		return ChatColor.translateAlternateColorCodes('&', itemBuild.toString());
	}
	
	private String capitalize(String text){
		StringBuilder cap = new StringBuilder();
		for (String t:text.split(" ")){
			cap.append(t.substring(0, 1).toUpperCase() + t.substring(1)+" ");			
		}
		return cap.substring(0, cap.length()-1);
	}*/
	
	@SuppressWarnings("deprecation")
	private JsonObject parseHoverItem(ItemStack item){		
		StringBuilder itemBuild = new StringBuilder();
		StringBuilder itemTag = new StringBuilder();		
		//serialize itemstack
		String itemType = item.getType().toString()
				.replace("_ITEM", "")
				.replace("_SPADE", "_SHOVEL");		
		itemBuild.append("id:"+itemType+",Count:"+1+",Damage:"+item.getDurability()+",");		
		if (item.hasItemMeta()){
			ItemMeta meta = item.getItemMeta();
			if (meta.hasLore()){
				StringBuilder lore = new StringBuilder();
				for (String lorep:meta.getLore()){
					lore.append(lorep+",");
				}
				itemTag.append("display:{Lore:["+lore.toString().substring(0, lore.length()-1)+"]},");
			} else if (meta.hasDisplayName()){
				itemTag.append("display:{Name:"+meta.getDisplayName()+"},");
			} else if (meta.hasLore() && meta.hasDisplayName()){
				StringBuilder lore = new StringBuilder();
				for (String lorep:meta.getLore()){
					lore.append(lorep+",");
				}
				itemTag.append("display:{Name:"+meta.getDisplayName()+",Lore:["+lore.toString().substring(0, lore.length()-1)+"]},");
			}
			if (meta.hasEnchants()){
				StringBuilder itemEnch = new StringBuilder();
				itemEnch.append("ench:[");
				for (Entry<Enchantment, Integer> ench:meta.getEnchants().entrySet()){
					itemEnch.append("{id:"+ench.getKey().getId()+",lvl:"+ench.getValue()+"},");
				}
				itemTag.append(itemEnch.toString().substring(0, itemEnch.length()-1)+"],");
			}
		}		
		if (itemTag.length() > 0){
			itemBuild.append("tag:{"+itemTag.toString().substring(0, itemTag.length()-1)+"},");
		}		
		JsonObject obj = new JsonObject();
		obj.addProperty("action", "show_item");	
		obj.addProperty("value", ChatColor.stripColor("{"+itemBuild.toString().substring(0, itemBuild.length()-1).replace(" ", "_")+"}"));
		return obj;
	}
		
	private JsonArray addColorToArray(String text){
		JsonArray extraArr = new JsonArray();
		ChatColor color = ChatColor.WHITE;
		for (String part:text.split("(?="+ChatColor.COLOR_CHAR+"[0-9a-fk-or])")){	
			JsonObject objExtraTxt = new JsonObject();
			Matcher match = Pattern.compile("^"+ChatColor.COLOR_CHAR+"([0-9a-fk-or]).*$").matcher(part);			
			if (match.find()){
				color = ChatColor.getByChar(match.group(1).charAt(0));
				if (part.length() == 2) continue;
			} 		
			objExtraTxt.addProperty("text", ChatColor.stripColor(part));
			if (color.isColor()){	
				objExtraTxt.addProperty("color", color.name().toLowerCase());			
			}
			if (color.equals(ChatColor.RESET)){
				objExtraTxt.addProperty("color", "white");								
			}		
			if (color.isFormat()){
				objExtraTxt.addProperty(color.name().toLowerCase(), true);
			}
			extraArr.add(objExtraTxt);
		}		
		return extraArr;
	}
	
	private class ExtraElement{
		private String action;
		private JsonObject json;
		
		public ExtraElement(String action, JsonObject json){
			this.action = action;
			this.json = json;
		}		
		public String getAction(){
			return this.action;
		}
		public JsonObject getJson(){
			return this.json;
		}
	}

}