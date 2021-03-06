package br.net.fabiozumbi12.UltimateChat.Bukkit.API;

import br.net.fabiozumbi12.UltimateChat.Bukkit.UCChannel;
import br.net.fabiozumbi12.UltimateChat.Bukkit.UCMessages;
import br.net.fabiozumbi12.UltimateChat.Bukkit.UChat;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.*;

public class uChatAPI{

	public boolean registerNewTag(String tagName, String format, String clickCmd, List<String> hoverMessages, String clickUrl){
		if (UChat.get().getUCConfig().getString("tags."+tagName+".format") == null){
			UChat.get().getUCConfig().setConfig("tags."+tagName+".format", format);
			UChat.get().getUCConfig().setConfig("tags."+tagName+".click-cmd", clickCmd);
			UChat.get().getUCConfig().setConfig("tags."+tagName+".click-url", clickUrl);
			UChat.get().getUCConfig().setConfig("tags."+tagName+".hover-messages", hoverMessages);
			UChat.get().getUCConfig().save();
			return true;
		}
		return false;
	}
	
	public boolean registerNewChannel(UCChannel channel) throws IOException{	
		UChat.get().getUCConfig().addChannel(channel);
		UChat.get().reload();
		return true;
	}
	
	public boolean registerNewChannel(Map<String, Object> properties) throws IOException{
		UCChannel ch = new UCChannel(properties);		
		UChat.get().getUCConfig().addChannel(ch);
		UChat.get().reload();
		return true;
	}
			
	@Deprecated
	public boolean registerNewChannel(String chName, String chAlias, boolean crossWorlds, int distance, String color, String tagBuilder, boolean needFocus, boolean receiverMsg, double cost, String ddmode, String ddmcformat, String mcddformat, String ddhover, boolean ddallowcmds, boolean bungee) throws IOException{
		if (UChat.get().getChannel(chName) != null){
			return false;
		}
		if (tagBuilder == null || tagBuilder.equals("")){
			tagBuilder = UChat.get().getUCConfig().getString("general.default-tag-builder");
		}
		UCChannel ch = new UCChannel(chName, chAlias, crossWorlds, distance, color, tagBuilder, needFocus, receiverMsg, cost, bungee, false, false, "player", "", new ArrayList<>(), "", ddmode, ddmcformat, mcddformat, ddhover, ddallowcmds, true);
		UChat.get().getUCConfig().addChannel(ch);
		UChat.get().reload();
		return true;
	}	
	
	public UCChannel getChannel(String chName){
		return UChat.get().getChannel(chName);
	}
	
	public UCChannel getPlayerChannel(Player player){
		return UChat.get().getPlayerChannel(player);
	}
	
	public Collection<UCChannel> getChannels(){
		return UChat.get().getChannels().values();
	}
	
	public Chat getVaultChat(){
		return UChat.get().getVaultChat();
	}
	
	public Economy getVaultEco(){
		return UChat.get().getVaultEco();
	}
	
	public Permission getVaultPerms(){
		return UChat.get().getVaultPerms();
	}

    /**
     * Get formated tag format from config with placeholders already parsed.
     * @param tagname Tag name from {@code tags} config section.
     * @param sender The player to be the sender/owner of parsed tag.
     * @param receiver The player as receiver of tag. Use {@link Optional}.empty() to do not use a receiver.
     * @return Formatted tag or {@code null} if the tag is not on config.
     */
	public String getTagFormat(String tagname, Player sender, Optional<Player> receiver){
        if (UChat.get().getUCConfig().getString("tags."+tagname+".format") != null){
            String format = UChat.get().getUCConfig().getString("tags."+tagname+".format");
            return UCMessages.formatTags(tagname, format, sender, receiver.isPresent() ? receiver.get() : "", "", UChat.get().getPlayerChannel(sender));
        }
        return null;
	}
}
