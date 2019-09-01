package csmp.bot.command.sgs;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import csmp.bot.command.DiscordCommandBase;
import csmp.bot.model.DiscordMessageData;
import csmp.service.CsmpService;
import csmp.utl.DateUtil;
import csmp.utl.DiscordUtil;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.Category;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.GuildChannel;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.util.Snowflake;

/**
 * シナリオ情報セットアップコマンド.
 * @author kgmas
 *
 */
public class ScenarioSetupCommand extends DiscordCommandBase {

	@Override
	public boolean judgeExecute(DiscordMessageData dmd) {
		if (dmd.getText().startsWith("/sgss ") || dmd.getText().startsWith("/sgread ")) {
			return true;
		}
		return false;
	}

	@Override
	public boolean checkInput(DiscordMessageData dmd) {
		if (dmd.getCommandArray().length < 2) {
			return false;
		}

		return true;
	}

	@Override
	public void execute(DiscordMessageData dmd) {
		Guild guild = dmd.getGuild();
		Category category = dmd.getChannel().getCategory().block();
		Map<Snowflake, Map<Object,Object>> guildScenarioInfo = CsmpService.getGuildScenarioInfo();

		if (guildScenarioInfo.containsKey(guild.getId())) {
			dmd.getChannel().createMessage("このサーバーは既にシナリオが登録されています。「/sgsclear」コマンドを使ってシナリオをクリアしてください。").block();
			return;
		}

		// シナリオ情報取得
		Map<Object, Object> secretMap = CsmpService.getScenarioSheetInfo(dmd.getCommandArray()[1]);
		if (secretMap == null) {
			dmd.getChannel().createMessage("シナリオシートのURLが誤っているか、公開中にチェックが入っていません。").block();
			return;
		}
		guildScenarioInfo.put(guild.getId(), secretMap);
		String scenarioName = (String)((Map<String, Object>)secretMap.get("base")).get("name");
		// シナリオ名のメッセージ出力
		dmd.getChannel().createMessage("シナリオ：" + scenarioName).block();
		System.out.println("シナリオ名：" + scenarioName);
		if ("/sgsread".equals(dmd.getCommandArray()[0])) {
			return;
		}

		Date current = Calendar.getInstance().getTime();
		SimpleDateFormat sdf = DateUtil.getDateFormat();
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
			String roleName = "PC" + CsmpService.text(pcInfo, "name");

			Role role = roleMap.get(roleName.toLowerCase());
			if (role == null) {
				role = guild.createRole(spec -> {
					spec.setName(roleName);
				}).block();
				roleMap.put(roleName.toLowerCase(), role);
			}
			Set<PermissionOverwrite> permission = DiscordUtil.getPrivateChannelPermission(role, guild);
			TextChannel tc = channelMap.get(roleName.toLowerCase());
			if (tc == null) {
				tc = guild.createTextChannel(spec ->{
					spec.setName(roleName);
					if (category != null) {
						spec.setParentId(category.getId());
					}
					spec.setPermissionOverwrites(permission);
				}).block();
				channelMap.put(roleName.toLowerCase(), tc);
			}

			String textMessage = "■" + roleName + "　推奨：" +CsmpService.text(pcInfo, "recommend") + "\r\n" +
					"・使命：【" +CsmpService.text(pcInfo, "mission") + "】" + "\r\n" +
					"・導入：\r\n" +
					CsmpService.text(pcInfo, "intro") + "\r\n" +
					"・秘密：\r\n" +
					CsmpService.text(pcInfo, "secret");

			tc.createMessage(textMessage).block();
		}
	}

	@Override
	public void help(DiscordMessageData dmd) {
		dmd.getChannel().createMessage("コマンドは「/sgss <シナリオシートURL>」と入力してください。").block();
	}

}
