package br.net.fabiozumbi12.UltimateChat.Sponge;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MutableMessageChannel;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;

public class UCListener {
	
	private void sendTell(CommandSource sender, Optional<CommandSource> receiver, Text msg){
		if (!receiver.isPresent() || (receiver.get() instanceof Player && (!((Player)receiver.get()).isOnline() 
				|| (sender instanceof Player && receiver.get() instanceof Player && !((Player)sender).canSee((Player)receiver.get()))))
				){
			UChat.get().getLang().sendMessage(sender, "listener.invalidplayer");
			return;
		}
		UChat.get().respondTell.put(receiver.get().getName(),sender.getName());
		UCMessages.sendFancyMessage(new String[0], msg, null, sender, receiver.get());			
	}
	
	@Listener(order = Order.LATE)
	public void onChat(MessageChannelEvent.Chat e, @First Player p){

        UChat.get().getLogger().timings(UCLogger.timingType.START, "UCListener#onChat()|Listening AsyncPlayerChatEvent");

		if (UChat.get().tellPlayers.containsKey(p.getName()) && (!UChat.get().tempTellPlayers.containsKey("CONSOLE") || !UChat.get().tempTellPlayers.get("CONSOLE").equals(p.getName()))){		
			String recStr = UChat.get().tellPlayers.get(p.getName());
			Optional<CommandSource> tellreceiver = Optional.ofNullable(Sponge.getServer().getPlayer(recStr).orElse(null));	
			sendTell(p, tellreceiver, e.getRawMessage());
			e.setMessageCancelled(true);			
		}

		else if (UChat.get().command.contains(p.getName()) || UChat.get().command.contains("CONSOLE")){
			if (UChat.get().tempTellPlayers.containsKey("CONSOLE")){
				String recStr = UChat.get().tempTellPlayers.get("CONSOLE");	
				Optional<CommandSource> pRec = Optional.ofNullable(Sponge.getServer().getPlayer(recStr).orElse(null));
				if (pRec.isPresent() && p.equals(pRec.get())){
					sendTell(Sponge.getServer().getConsole(), pRec, e.getRawMessage());
					UChat.get().tempTellPlayers.remove("CONSOLE");
					UChat.get().command.remove("CONSOLE");
				}				
			} else if (UChat.get().tempTellPlayers.containsKey(p.getName())){
				String recStr = UChat.get().tempTellPlayers.get(p.getName());
				if (recStr.equals("CONSOLE")){
					sendTell(p, Optional.of(Sponge.getServer().getConsole()), e.getRawMessage());
				} else {
					sendTell(p, Optional.ofNullable(Sponge.getServer().getPlayer(recStr).orElse(null)), e.getRawMessage());
				}		
				UChat.get().tempTellPlayers.remove(p.getName());
				UChat.get().command.remove(p.getName());
			} else if (UChat.get().respondTell.containsKey(p.getName())){
				String recStr = UChat.get().respondTell.get(p.getName());
				if (recStr.equals("CONSOLE")){
					sendTell(p, Optional.of(Sponge.getServer().getConsole()), e.getRawMessage());
				} else {
					sendTell(p, Optional.ofNullable(Sponge.getServer().getPlayer(recStr).orElse(null)), e.getRawMessage());
				}
				//UChat.get().respondTell.remove(p.getName());
				UChat.get().command.remove(p.getName());
			}
			e.setMessageCancelled(true);
		} 
		
		else {
			UCChannel ch = UChat.get().getPlayerChannel(p);
			if (UChat.get().tempChannels.containsKey(p.getName()) && !UChat.get().tempChannels.get(p.getName()).equals(ch.getAlias())){
				ch = UChat.get().getChannel(UChat.get().tempChannels.get(p.getName()));
				UChat.get().tempChannels.remove(p.getName());
			}
			
			if (UChat.get().mutes.contains(p.getName()) || ch.isMuted(p.getName())){
				if (UChat.get().timeMute.containsKey(p.getName())) {
					UChat.get().getLang().sendMessage(p, UChat.get().getLang().get("channel.tempmuted").replace("{time}", String.valueOf(UChat.get().timeMute.get(p.getName()))));
				} else {
					UChat.get().getLang().sendMessage(p, "channel.muted");
				}
				e.setMessageCancelled(true);
				return;
			}			
			
			if (ch.isCmdAlias()){
				String start = ch.getAliasCmd();
				if (start.startsWith("/")){
					start = start.substring(1);
				}
				if (ch.getAliasSender().equalsIgnoreCase("console")){
					Sponge.getCommandManager().process(Sponge.getServer().getConsole(),start+" "+e.getRawMessage());
				} else {
					Sponge.getCommandManager().process(p, start+" "+e.getRawMessage());
				}				
				e.setMessageCancelled(true);
			} else {
				e.setMessageCancelled(true);				
				MutableMessageChannel msgCh = UCMessages.sendFancyMessage(new String[]{
						TextSerializers.FORMATTING_CODE.serialize(e.getFormatter().getHeader().format()),
						TextSerializers.FORMATTING_CODE.serialize(e.getFormatter().getBody().format()),
						TextSerializers.FORMATTING_CODE.serialize(e.getFormatter().getFooter().format())
						}, e.getRawMessage(), ch, p, null);					
				if (msgCh != null){					
					e.setChannel(msgCh);
				}										
			}
		}				
	}
	
	@Listener(order = Order.POST)
	public void onCommand(SendCommandEvent e, @First CommandSource p){
		if (UChat.get().getUCJDA() != null){
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
			UChat.get().getUCJDA().sendCommandsToDiscord(UChat.get().getLang().get("discord.command")
					.replace("{player}", p.getName())
					.replace("{cmd}", "/"+e.getCommand()+" "+e.getArguments())
                    .replace("{time-now}",sdf.format(cal.getTime())));
		}		
	}	
	
	@Listener
	public void onDeath(DestructEntityEvent.Death e, @Getter("getTargetEntity") Player p){
		if (UChat.get().getUCJDA() != null){
			UChat.get().getUCJDA().sendRawToDiscord(UChat.get().getLang().get("discord.death").replace("{player}", p.getName()));				
		}
	}
	
	@Listener
	public void onJoin(ClientConnectionEvent.Join e){
		Player p = e.getTargetEntity();

		if (!UChat.get().getConfig().root().general.persist_channels){
			UChat.get().getDefChannel().addMember(p);
		}

		if (UChat.get().getUCJDA() != null){
			UChat.get().getUCJDA().sendRawToDiscord(UChat.get().getLang().get("discord.join").replace("{player}", p.getName()));
			if (UChat.get().getConfig().root().discord.update_status){
				UChat.get().getUCJDA().updateGame(UChat.get().getLang().get("discord.game").replace("{online}", String.valueOf(Sponge.getServer().getOnlinePlayers().size())));
			}
		}
		if (UChat.get().getConfig().root().general.spy_enabled_onjoin && p.hasPermission("uchat.cmd.spy") && !UChat.get().isSpy.contains(p.getName())){
			UChat.get().isSpy.add(p.getName());
			UChat.get().getLang().sendMessage(p, "cmd.spy.enabled");
		}
	}
		
	@Listener
	public void onQuit(ClientConnectionEvent.Disconnect e){
		Player p = e.getTargetEntity();

        if (!UChat.get().getConfig().root().general.persist_channels){
            UChat.get().getPlayerChannel(p).removeMember(p);
        }

		List<String> toRemove = new ArrayList<>();
		for (String play:UChat.get().tellPlayers.keySet()){
			if (play.equals(p.getName()) || UChat.get().tellPlayers.get(play).equals(p.getName())){
				toRemove.add(play);				
			}
		}	
		for (String remove:toRemove){
			UChat.get().tellPlayers.remove(remove);
		}
		List<String> toRemove2 = new ArrayList<>();
		for (String play:UChat.get().respondTell.keySet()){
			if (play.equals(p.getName()) || UChat.get().respondTell.get(play).equals(p.getName())){
				toRemove2.add(play);				
			}
		}	
		for (String remove:toRemove2){
			UChat.get().respondTell.remove(remove);
		}
        if (UChat.get().tempChannels.containsKey(p.getName())){
            UChat.get().tempChannels.remove(p.getName());
        }
		if (UChat.get().getUCJDA() != null){
			UChat.get().getUCJDA().sendRawToDiscord(UChat.get().getLang().get("discord.leave").replace("{player}", p.getName()));
			if (UChat.get().getConfig().root().discord.update_status){
				UChat.get().getUCJDA().updateGame(UChat.get().getLang().get("discord.game").replace("{online}", String.valueOf(Sponge.getServer().getOnlinePlayers().size()-1)));
			}
		}
	}			
}
