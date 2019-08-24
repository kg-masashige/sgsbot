package csmp.bot;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import net.arnx.jsonic.JSON;

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
					message.getChannel().block().createMessage("エラーが発生しました。再度実施してください。");
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
				String roleName = "PC" + (String)pcInfo.get("name");
				if (roleName.toLowerCase().equals(channel.getName().toLowerCase())) {
					channel.delete().block();
				}
			}
		}
		for (Role role : roleList) {
			for (Map<String, Object> pcInfo : pcList) {
				String roleName = "PC" + (String)pcInfo.get("name");
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
		// TODO /sgssend 秘密名 チャンネル
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
				String name = "PC" + text(map, "name");
				if (secretName.equals(name)) {
					textMessage = "■" + name + "の秘密：\r\n" + text(map, "secret");
					break;
				}
			}
		}
		if (textMessage == null) {
			List<Map<String, Object>> npcList = (List<Map<String, Object>>)secretMap.get("npc");
			for (Map<String, Object> map : npcList) {
				String name = text(map, "name");
				if (secretName.equals(name)) {
					textMessage = "■" + name + "の秘密：\r\n" + text(map, "secret");
					break;
				}
			}
		}
		if (textMessage == null) {
			message.getChannel().block().createMessage(secretName + "の秘密がシナリオ情報内に見つかりません。").block();
			return;
		}
		TextChannel tc = null;
		List<GuildChannel> channels = guild.getChannels().collectList().block();
		for (GuildChannel channel : channels) {
			if (roleName.toLowerCase().equals(channel.getName().toLowerCase())) {
				if (channel instanceof TextChannel) {
					tc = (TextChannel)channel;
					break;
				}
			}
		}
		if (tc == null) {
			message.getChannel().block().createMessage(roleName + "のチャンネルがサーバ内に見つかりません。").block();
			return;
		}

		tc.createMessage(textMessage).block();


		// TODO 感情が結ばれている場合、結ばれている相手にも情報を自動で送る。
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
			messageChannel.createMessage("このサーバーは別のシナリオが登録されています。「/sgsclear」コマンドを使ってシナリオをクリアしてください。").block();
			return;
		}

		// シナリオ情報取得
		Map<Object, Object> secretMap = getScenarioSheetInfo(commandArray[1]);
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

		Map<String, Role> map = new HashMap<>();
		for (Role role : guild.getRoles().collectList().block()) {
			map.put(role.getName(), role);
		}

		// PC情報の取得
		List<Map<String, Object>> pcList = (List<Map<String, Object>>)secretMap.get("pc");
		for (Map<String, Object> pcInfo : pcList) {
			String roleName = "PC" + (String)pcInfo.get("name");

			TextChannel tc = null;
			if (!map.containsKey(roleName)) {
				Role role = guild.createRole(spec -> {
					spec.setName(roleName);
				}).block();
				tc = message.getGuild().block().createTextChannel(spec ->{
					spec.setName(roleName);
					if (category != null) {
						spec.setParentId(category.getId());
					}
					spec.setPermissionOverwrites(getPrivateChannelPermission(role, guild));
				}).block();

			} else {
				List<GuildChannel> channels = guild.getChannels().collectList().block();
				for (GuildChannel channel : channels) {
					if (roleName.toLowerCase().equals(channel.getName().toLowerCase())) {
						if (channel instanceof TextChannel) {
							tc = (TextChannel)channel;
							break;
						}
					}
				}
			}

			String textMessage = "■" + roleName + "　推奨：" + text(pcInfo, "recommend") + "\r\n" +
					"・使命：【" + text(pcInfo, "mission") + "】" + "\r\n" +
					"・導入：\r\n" +
					text(pcInfo, "intro") + "\r\n" +
					"・秘密：\r\n" +
					text(pcInfo, "secret");

			tc.createMessage(textMessage).block();

		}

	}

	/**
	 * マップから文字を取り出す。なければ空文字.
	 * @param map シナリオ情報マップ
	 * @param key キー
	 * @return 値。null to blank.
	 */
	private static String text(Map<String, Object> map, String key) {
		Object result = map.get(key);
		if (result == null) {
			result = "";
		}
		return result.toString();
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

	/**
	 * シナリオシート情報を取得する.
	 * @param sheetUrl シナリオシートURL.
	 * @return シナリオ秘密Map
	 */
	private static Map<Object, Object> getScenarioSheetInfo(String sheetUrl) {
		// TODO 入力チェック
		try {
			if (sheetUrl.startsWith("<") && sheetUrl.endsWith(">")) {
				sheetUrl = sheetUrl.substring(1, sheetUrl.length() - 1);
			}
			String dispUrl = sheetUrl.replace("edit.html", "display") + "&ajax=1";
			HttpURLConnection con = getConnection(dispUrl);
			OutputStreamWriter writer = new OutputStreamWriter(con.getOutputStream());
			writer.close();
			if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
				Map<Object, Object> map = JSON.decode(con.getInputStream());
				con.disconnect();
				Map<String, Object> baseMap = (Map<String, Object>)map.get("base");
				if (baseMap != null && "1".equals(baseMap.get("publicview"))) {
					String openUrl = dispUrl.replace("display", "openSecret");
					HttpURLConnection secretCon = getConnection(openUrl);
					OutputStreamWriter secretWriter = new OutputStreamWriter(secretCon.getOutputStream());
					secretWriter.write("pass=" + text(baseMap, "publicviewpass"));
					secretWriter.flush();
					secretWriter.close();
					Map<Object, Object> secretMap = JSON.decode(secretCon.getInputStream());
					secretCon.disconnect();

					return secretMap;
				}
			}
		} catch (Exception e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		return null;

	}

	/**
	 * HTTP接続情報を取得する.
	 * @param site シナリオシートURL
	 * @return 接続情報
	 */
	private static HttpURLConnection getConnection(String site) {
		HttpURLConnection con = null;

		try {
			URL url = new URL(site);
			con = (HttpURLConnection)url.openConnection();
			con.setDoOutput(true);
			con.setConnectTimeout(100000);
			con.setReadTimeout(100000);
			con.setRequestMethod("POST");
		} catch (Exception e) {
			e.printStackTrace();
		}

		return con;
	}

}