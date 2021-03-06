package br.net.fabiozumbi12.UltimateChat.Bukkit;

import br.net.fabiozumbi12.UltimateChat.Bukkit.API.SendChannelMessageEvent;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

class UCChatProtection implements Listener{
	
	private final HashMap<Player,String> chatSpam = new HashMap<>();
	private final HashMap<String,Integer> msgSpam = new HashMap<>();
	private final HashMap<Player,Integer> UrlSpam = new HashMap<>();

	@EventHandler
	public void onPlayerChat(SendChannelMessageEvent e){
		if (!(e.getSender() instanceof Player)){
			return;
		}
		
		final Player p = (Player) e.getSender();
		String msg = e.getMessage();
		UCChannel ch = e.getChannel();
		
		if (msg.length() <= 1){
			return;
		}
		
		//mute check
		if (UChat.get().mutes.contains(p.getName())){
			UChat.get().getLang().sendMessage(p, UChat.get().getUCConfig().getProtMsg("chat-protection.anti-ip.punish.mute-msg"));
			e.setCancelled(true);
			return;
		}
		
		//antispam
		if (UChat.get().getUCConfig().getProtBool("chat-protection.antispam.enabled") && !UCPerms.hasPermission(p, "uchat.bypass-spam")
				&& (ch == null || !UChat.get().getUCConfig().getProtStringList("chat-protection.antispam.disable-on-channels").contains(ch.getName()))){
			
			//check spam messages
			if (!chatSpam.containsKey(p)){
				chatSpam.put(p, msg);				
				Bukkit.getScheduler().scheduleSyncDelayedTask(UChat.get(), () -> {
                    if (chatSpam.containsKey(p)){
                        chatSpam.remove(p);
                    }
                },UChat.get().getUCConfig().getProtInt("chat-protection.antispam.time-between-messages")*20);
			} else if (!chatSpam.get(p).equalsIgnoreCase(msg)){				
				UChat.get().getLang().sendMessage(p, UChat.get().getUCConfig().getProtMsg("chat-protection.antispam.cooldown-msg"));
				e.setCancelled(true);
				return;
			}
			
			//check same message frequency
			if (!msgSpam.containsKey(msg)){
				msgSpam.put(msg, 1);
				final String nmsg = msg;
				Bukkit.getScheduler().scheduleSyncDelayedTask(UChat.get(), () -> {
                    if (msgSpam.containsKey(nmsg)){
                        msgSpam.remove(nmsg);
                    }
                },UChat.get().getUCConfig().getProtInt("chat-protection.antispam.time-between-same-messages")*20);
			} else {
				msgSpam.put(msg, msgSpam.get(msg)+1);
				e.setCancelled(true);				
				if (msgSpam.get(msg) >= UChat.get().getUCConfig().getProtInt("chat-protection.antispam.count-of-same-message")){
					UCUtil.performCommand(p, UChat.get().getServer().getConsoleSender(),UChat.get().getUCConfig().getProtString("chat-protection.antispam.cmd-action").replace("{player}", p.getName()));
					msgSpam.remove(msg);
				} else {
					UChat.get().getLang().sendMessage(p, UChat.get().getUCConfig().getProtMsg("chat-protection.antispam.wait-message"));
				}
				return;
			}			
		}
				
		//censor
		if (UChat.get().getUCConfig().getProtBool("chat-protection.censor.enabled") && !UCPerms.hasPermission(p, "uchat.bypass-censor")
				&& (ch == null || !UChat.get().getUCConfig().getProtStringList("chat-protection.censor.disable-on-channels").contains(ch.getName()))){
			int act = 0;
			for (String word:UChat.get().getUCConfig().getProtReplecements().getKeys(false)){
				if (!Pattern.compile(word).matcher(msg).find()){
					continue;
				} 				
				String replaceby = UChat.get().getUCConfig().getProtString("chat-protection.censor.replace-words."+word);
				if (UChat.get().getUCConfig().getProtBool("chat-protection.censor.replace-by-symbol")){
					replaceby = word.replaceAll("(?s).", UChat.get().getUCConfig().getProtString("chat-protection.censor.by-symbol"));
				}

				if (UChat.get().getUCConfig().getProtBool("chat-protection.censor.use-pre-actions")){
					if (!UChat.get().getUCConfig().getProtBool("chat-protection.censor.replace-partial-word")){
						msg = msg.replaceAll("(?i)"+"\\b"+Pattern.quote(word)+"\\b", replaceby);
						if (UChat.get().getUCConfig().getProtBool("chat-protection.censor.action.on-partial-words")){
							act++;
						}
					} else {
						msg = msg.replaceAll("(?i)"+word, replaceby);
						act++;
					}
				} else {
					msg = msg.replaceAll(word, replaceby);
					act++;
				}
			}
			if (act > 0){
				String action = UChat.get().getUCConfig().getProtString("chat-protection.censor.action.cmd");
				if (!action.isEmpty()){
					List<String> chs = UChat.get().getUCConfig().getProtStringList("chat-protection.censor.action.only-on-channels");
					if (!chs.isEmpty()){
						for (String cha:chs){
							if (cha.equalsIgnoreCase(e.getChannel().getName()) || cha.equalsIgnoreCase(e.getChannel().getAlias())){
								UCUtil.performCommand(p, UChat.get().getServer().getConsoleSender(), action.replace("{player}", p.getName()));
								break;
							}
						}
					} else {
						UCUtil.performCommand(p, UChat.get().getServer().getConsoleSender(), action.replace("{player}", p.getName()));
					}					
				}
			}
		}
		
		String regexIP = UChat.get().getUCConfig().getProtString("chat-protection.anti-ip.custom-ip-regex");
		String regexUrl = UChat.get().getUCConfig().getProtString("chat-protection.anti-ip.custom-url-regex");
		
		//check ip and website
		if (UChat.get().getUCConfig().getProtBool("chat-protection.anti-ip.enabled") && !UCPerms.hasPermission(p, "uchat.bypass-anti-ip")
				&& (ch == null || !UChat.get().getUCConfig().getProtStringList("chat-protection.anti-ip.disable-on-channels").contains(ch.getName()))){
			
			//check whitelist
			int cont = 0;
			for (String check:UChat.get().getUCConfig().getProtStringList("chat-protection.anti-ip.whitelist-words")){
				if (Pattern.compile(check).matcher(msg).find()){	
					cont++;
				}
			}
			
			//continue
			if (UChat.get().getUCConfig().getProtStringList("chat-protection.anti-ip.whitelist-words").isEmpty() || cont == 0){
				if (Pattern.compile(regexIP).matcher(msg).find()){	
					addURLspam(p);
					if (UChat.get().getUCConfig().getProtString("chat-protection.anti-ip.cancel-or-replace").equalsIgnoreCase("cancel")){
						e.setCancelled(true);
						UChat.get().getLang().sendMessage(p, UChat.get().getUCConfig().getProtMsg("chat-protection.anti-ip.cancel-msg"));
						return;
					} else {
						msg = msg.replaceAll(regexIP, UChat.get().getUCConfig().getProtMsg("chat-protection.anti-ip.replace-by-word"));
					}
				}
				if (Pattern.compile(regexUrl).matcher(msg).find()){
					addURLspam(p);
					if (UChat.get().getUCConfig().getProtString("chat-protection.anti-ip.cancel-or-replace").equalsIgnoreCase("cancel")){
						e.setCancelled(true);
						UChat.get().getLang().sendMessage(p, UChat.get().getUCConfig().getProtMsg("chat-protection.anti-ip.cancel-msg"));
						return;
					} else {
						msg = msg.replaceAll(regexUrl, UChat.get().getUCConfig().getProtMsg("chat-protection.anti-ip.replace-by-word"));
					}
				}
				
				for (String word:UChat.get().getUCConfig().getProtStringList("chat-protection.anti-ip.check-for-words")){
					if (Pattern.compile("(?i)"+"\\b"+word+"\\b").matcher(msg).find()){
						addURLspam(p);
						if (UChat.get().getUCConfig().getProtString("chat-protection.anti-ip.cancel-or-replace").equalsIgnoreCase("cancel")){
							e.setCancelled(true);
							UChat.get().getLang().sendMessage(p, UChat.get().getUCConfig().getProtMsg("chat-protection.anti-ip.cancel-msg"));
							return;
						} else {
							msg = msg.replaceAll("(?i)"+word, UChat.get().getUCConfig().getProtMsg("chat-protection.anti-ip.replace-by-word"));
						}
					}
				}
			}					
		}	
		
		//capitalization verify
		if (UChat.get().getUCConfig().getProtBool("chat-protection.chat-enhancement.enabled") && !UCPerms.hasPermission(p, "uchat.bypass-enhancement")
				&& (ch == null || !UChat.get().getUCConfig().getProtStringList("chat-protection.chat-enhancement.disable-on-channels").contains(ch.getName()))){
			int length = UChat.get().getUCConfig().getProtInt("chat-protection.chat-enhancement.minimum-length");
			if (!Pattern.compile(regexIP).matcher(msg).find() && !Pattern.compile(regexUrl).matcher(msg).find() && msg.length() > length){
				msg = msg.replaceAll("([.!?])\\1+", "$1").replaceAll(" +", " ").substring(0, 1).toUpperCase()+msg.substring(1);
				if (UChat.get().getUCConfig().getProtBool("chat-protection.chat-enhancement.end-with-dot") && !msg.endsWith("?") && !msg.endsWith("!") && !msg.endsWith(".") && msg.split(" ").length > 2){
					msg = msg+".";
				}
			}				
		}
		
		//anti-caps
		if (UChat.get().getUCConfig().getProtBool("chat-protection.caps-filter.enabled") && !UCPerms.hasPermission(p, "uchat.bypass-enhancement")
				&& (ch == null || !UChat.get().getUCConfig().getProtStringList("chat-protection.caps-filter.disable-on-channels").contains(ch.getName()))){
			int length = UChat.get().getUCConfig().getProtInt("chat-protection.caps-filter.minimum-length");
			int msgUppers = msg.replaceAll("\\p{P}", "").replaceAll("[a-z ]+", "").length();
			if (!Pattern.compile(regexIP).matcher(msg).find() && !Pattern.compile(regexUrl).matcher(msg).find() && msgUppers >= length){
				msg = msg.substring(0, 1).toUpperCase()+msg.substring(1).toLowerCase();
			}
		}
		
		//antiflood
		if (UChat.get().getUCConfig().getProtBool("chat-protection.anti-flood.enable")
				&& (ch == null || !UChat.get().getUCConfig().getProtStringList("chat-protection.anti-flood.disable-on-channels").contains(ch.getName()))){
			for (String flood:UChat.get().getUCConfig().getProtStringList("chat-protection.anti-flood.whitelist-flood-characs")){
				if (Pattern.compile("(["+flood+"])\\1+").matcher(msg).find()){
					e.setMessage(msg);	
					return;
				}
			}
			msg = msg.replaceAll("([A-Za-z])\\1+", "$1$1");
		}
		e.setMessage(msg);	
	}	
	
	private void addURLspam(final Player p){
		if (UChat.get().getUCConfig().getProtBool("chat-protection.anti-ip.punish.enable")){
			if (!UrlSpam.containsKey(p)){
				UrlSpam.put(p, 1);
			} else {
				UrlSpam.put(p, UrlSpam.get(p)+1);
				//p.sendMessage("UrlSpam: "+UrlSpam.get(p));
				if (UrlSpam.get(p) >= UChat.get().getUCConfig().getProtInt("chat-protection.anti-ip.punish.max-attempts")){
					if (UChat.get().getUCConfig().getProtString("chat-protection.anti-ip.punish.mute-or-cmd").equalsIgnoreCase("mute")){
						
						int time = UChat.get().getUCConfig().getProtInt("chat-protection.anti-ip.punish.mute-duration");
						UChat.get().mutes.add(p.getName());
						UChat.get().muteInAllChannels(p.getName());
						p.sendMessage(UChat.get().getUCConfig().getProtMsg("chat-protection.anti-ip.punish.mute-msg"));
						
						//mute counter
						new MuteCountDown(p.getName(), time).runTaskTimer(UChat.get(), 20, 20);							
					} else {
						UCUtil.performCommand(p, UChat.get().getServer().getConsoleSender(), UChat.get().getUCConfig().getProtString("chat-protection.anti-ip.punish.cmd-punish").replace("{player}", p.getName()));
					}	
					UrlSpam.remove(p);
				}
			}
		}		
	}
}
