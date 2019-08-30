package csmp.bot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import csmp.utl.CsmpUtil;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.Category;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.GuildChannel;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.PermissionSet;
import discord4j.core.object.util.Snowflake;

/**
 * シノビガミシナリオセットアップbot.
 * @author kgmas
 *
 */
@SuppressWarnings("unchecked")
public class ShinobigamiScenarioSetupBot {

	/**
	 * discordクライアントインスタンス.
	 */
	private static DiscordClient client;

	/**
	 * ギルドシナリオ情報.そのうちDBへ.
	 */
	private static Map<Snowflake, Map<Object, Object>> guildScenarioInfo;

	/**
	 * メイン.
	 * botを起動して待機する.
	 * @param args
	 */
	public static void main(String args[]) {

		System.out.println("Botを起動中...");

		String token = System.getenv("DISCORD_BOT_TOKEN");

		guildScenarioInfo = new HashMap<>();
		client = new DiscordClientBuilder(token).build();

		client.getEventDispatcher().on(ReadyEvent.class)
		.subscribe(ready -> {
			System.out.println("Logged in as " + ready.getSelf().getUsername());
		});

		client.getEventDispatcher().on(MessageCreateEvent.class)
		.subscribe(event -> {
			Message message = event.getMessage();

			if (message.getContent().isPresent()) {
				String text = "";
				try {
					text = message.getContent().get();
					if (text.startsWith("/sgssend ")) {
						sendSecret(message);
					} else if (text.startsWith("/sgss") || text.startsWith("/sgsread")) {
						setup(message);
					} else if (text.equals("/sgsclear")) {
						clear(message);
					} else if (text.equals("/sgshelp")) {
						String helpText = "コマンド一覧：\r\n" +
								"　/sgss <シナリオシートURL>：\r\n　　　シナリオシートの情報を読み込んでチャンネル、権限を設定する。\r\n" +
								"　/sgsread <シナリオシートURL>：\r\n　　　シナリオシートの情報の読み込みだけ行う。\r\n" +
								"　/sgsclear：\r\n　　　シナリオシートの情報、チャンネル、権限を削除する。\r\n" +
								"　/sgssend <秘密名> <PC名>：\r\n　　　PC名のチャンネルに指定された秘密を張り付ける。";

						message.getChannel().block()
							.createMessage(helpText).block();
					}
				} catch (Throwable e) {
					System.out.println("error command:" + text);
					message.getChannel().block().createMessage("エラーが発生しました。Twitter ID:@kg_masashigeまで連絡してください。");
					e.printStackTrace();
				}
			}

		 });

		client.login().block();
	}

	/**
	 * シナリオ情報をクリアする. /sgsclear.
	 * @param message メッセージ.
	 * @throws Exception
	 */
	private static void clear(Message message) throws Exception {
		Guild guild = message.getGuild().block();

		Map<Object, Object> secretMap = guildScenarioInfo.remove(guild.getId());
		if (secretMap == null) {
			message.getChannel().block().createMessage("シナリオ情報が設定されていません。").block();
			return;
		}
		List<Map<String, Object>> pcList = (List<Map<String, Object>>)secretMap.get("pc");
		List<GuildChannel> channelList = guild.getChannels().collectList().block();
		List<Role> roleList = guild.getRoles().collectList().block();
		for (GuildChannel channel : channelList) {
			for (Map<String, Object> pcInfo : pcList) {
				String roleName = "PC" + CsmpUtil.text(pcInfo, "name");
				if (roleName.toLowerCase().equals(channel.getName().toLowerCase())) {
					channel.delete().block();
				}
			}
		}
		for (Role role : roleList) {
			for (Map<String, Object> pcInfo : pcList) {
				String roleName = "PC" + CsmpUtil.text(pcInfo, "name");
				if (roleName.equals(role.getName())) {
					role.delete().block();
					break;
				}
			}
		}
		message.getChannel().block()
			.createMessage("役職とチャンネルを削除しました。").block();

	}

	/**
	 * 秘密を送信する。 /sgssend 秘密名 チャンネル.
	 * @param message メッセージ
	 * @throws Exception
	 */
	private static void sendSecret(Message message) throws Exception {
		// /sgssend 秘密名 チャンネル
		String[] commandArray = message.getContent().get().split(" ");
		if (commandArray.length != 3) {
			message.getChannel().block().createMessage("コマンドは「/sgssend <秘密名> <送る先のチャンネル名>」と入力してください。").block();
			return;
		}
		Guild guild = message.getGuild().block();
		Map<Object, Object> secretMap = guildScenarioInfo.get(guild.getId());
		if (secretMap == null) {
			message.getChannel().block().createMessage("シナリオ情報が設定されていません。").block();
			return;
		}
		String secretName = commandArray[1];
		String roleName = commandArray[2];
		String textMessage = null;
		List<Map<String, Object>> pcList = (List<Map<String, Object>>)secretMap.get("pc");
		if (secretName.toUpperCase().startsWith("PC")) {
			for (Map<String, Object> map : pcList) {
				String name = "PC" +CsmpUtil.text(map, "name");
				if (secretName.equals(name)) {
					textMessage = "■" + name + "の秘密：\r\n" + CsmpUtil.text(map, "secret");
					break;
				}
			}
		}
		if (textMessage == null) {
			List<Map<String, Object>> npcList = (List<Map<String, Object>>)secretMap.get("npc");
			for (Map<String, Object> map : npcList) {
				String name =CsmpUtil.text(map, "name");
				if (secretName.equals(name)) {
					textMessage = "■" + name + "の秘密：\r\n" +CsmpUtil.text(map, "secret");
					break;
				}
			}
		}
		if (textMessage == null) {
			message.getChannel().block().createMessage(secretName + "の秘密がシナリオ情報内に見つかりません。").block();
			return;
		}
		TextChannel tc = getTextChannelByName(guild, roleName);
		if (tc == null) {
			message.getChannel().block().createMessage(roleName + "のチャンネルがサーバ内に見つかりません。").block();
			return;
		}

		tc.createMessage(textMessage).block();

	}

	/**
	 * シナリオ情報をセットアップする。 /sgss シナリオURL.
	 * @param message メッセージ
	 * @throws Exception
	 */
	private static void setup(Message message) throws Exception {
		String[] commandArray = message.getContent().get().split(" ");
		MessageChannel messageChannel = message.getChannel().block();
		if (commandArray.length < 2) {
			messageChannel.createMessage("コマンドは「/sgss <シナリオシートURL>」と入力してください。").block();
			return;
		}
		Guild guild = message.getGuild().block();
		Category category = ((TextChannel)message.getChannel().block()).getCategory().block();

		if (guildScenarioInfo.containsKey(guild.getId())) {
			messageChannel.createMessage("このサーバーは既にシナリオが登録されています。「/sgsclear」コマンドを使ってシナリオをクリアしてください。").block();
			return;
		}

		// シナリオ情報取得
		Map<Object, Object> secretMap = CsmpUtil.getScenarioSheetInfo(commandArray[1]);
		if (secretMap == null) {
			messageChannel.createMessage("シナリオシートのURLが誤っているか、公開中にチェックが入っていません。").block();
			return;
		}
		guildScenarioInfo.put(guild.getId(), secretMap);
		String scenarioName = (String)((Map<String, Object>)secretMap.get("base")).get("name");
		// シナリオ名のメッセージ出力
		messageChannel.createMessage("シナリオ：" + scenarioName).block();
		System.out.println("シナリオ名：" + scenarioName);
		if ("/sgsread".equals(commandArray[0])) {
			return;
		}

		Date current = Calendar.getInstance().getTime();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		guild.edit(spec -> {
			spec.setName(scenarioName + "_" + sdf.format(current));
		}).block();

		Map<String, Role> roleMap = new HashMap<>();
		for (Role role : guild.getRoles().collectList().block()) {
			roleMap.put(role.getName().toLowerCase(), role);
		}

		Map<String, TextChannel> channelMap = new HashMap<>();
		List<GuildChannel> channels = guild.getChannels().collectList().block();
		for (GuildChannel channel : channels) {
			if (channel instanceof TextChannel) {
				channelMap.put(channel.getName().toLowerCase(), (TextChannel)channel);
			}
		}

		// PC情報の取得
		List<Map<String, Object>> pcList = (List<Map<String, Object>>)secretMap.get("pc");
		for (Map<String, Object> pcInfo : pcList) {
			String roleName = "PC" + CsmpUtil.text(pcInfo, "name");

			Role role = roleMap.get(roleName.toLowerCase());
			if (role == null) {
				role = guild.createRole(spec -> {
					spec.setName(roleName);
				}).block();
				roleMap.put(roleName.toLowerCase(), role);
			}
			Set<PermissionOverwrite> permission = getPrivateChannelPermission(role, guild);
			TextChannel tc = channelMap.get(roleName.toLowerCase());
			if (tc == null) {
				tc = message.getGuild().block().createTextChannel(spec ->{
					spec.setName(roleName);
					if (category != null) {
						spec.setParentId(category.getId());
					}
					spec.setPermissionOverwrites(permission);
				}).block();
				channelMap.put(roleName.toLowerCase(), tc);
			}

			String textMessage = "■" + roleName + "　推奨：" +CsmpUtil.text(pcInfo, "recommend") + "\r\n" +
					"・使命：【" +CsmpUtil.text(pcInfo, "mission") + "】" + "\r\n" +
					"・導入：\r\n" +
					CsmpUtil.text(pcInfo, "intro") + "\r\n" +
					"・秘密：\r\n" +
					CsmpUtil.text(pcInfo, "secret");

			tc.createMessage(textMessage).block();

		}

	}

	/**
	 * チャンネル名を指定してテキストチャンネルを取得する.
	 * @param guild サーバ
	 * @param name 名前
	 * @return
	 */
	private static TextChannel getTextChannelByName(Guild guild, String name) {
		List<GuildChannel> channels = guild.getChannels().collectList().block();
		for (GuildChannel channel : channels) {
			if (name.toLowerCase().equals(channel.getName().toLowerCase())) {
				if (channel instanceof TextChannel) {
					return (TextChannel)channel;
				}
			}
		}
		return null;
	}

	/**
	 * プライベートチャンネル用の権限を設定する.
	 * @param role 役割
	 * @param guild サーバ
	 * @return 権限情報
	 */
	private static Set<PermissionOverwrite> getPrivateChannelPermission(Role role, Guild guild) {
		Set<PermissionOverwrite> permissionOverwrites = new HashSet<>();
		PermissionSet viewPs = PermissionSet.of(Permission.VIEW_CHANNEL);
		PermissionOverwrite po = PermissionOverwrite.forRole(role.getId(), viewPs, PermissionSet.none());
		PermissionOverwrite everyonePO = PermissionOverwrite.forRole(guild.getEveryoneRole().block().getId(), PermissionSet.none(), viewPs);
		permissionOverwrites.add(po);
		permissionOverwrites.add(everyonePO);

		return permissionOverwrites;
	}


}