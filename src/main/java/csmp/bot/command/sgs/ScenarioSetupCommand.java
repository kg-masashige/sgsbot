package csmp.bot.command.sgs;

import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.channel.ServerTextChannelBuilder;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.permission.PermissionsBuilder;
import org.javacord.api.entity.permission.Role;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.server.ServerUpdater;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;

import csmp.bot.command.IDiscordCommand;
import csmp.bot.command.IDiscordSlashCommand;
import csmp.bot.model.CommandHelpData;
import csmp.bot.model.DiscordMessageData;
import csmp.service.CsmpService;
import csmp.utl.DateUtil;

/**
 * シナリオ情報セットアップコマンド.
 * @author kgmas
 *
 */
public class ScenarioSetupCommand implements IDiscordCommand, IDiscordSlashCommand {

	/**
	 * ロガー
	 */
	private static Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	public boolean judgeExecute(DiscordMessageData dmd) {
		if (dmd.getGuild() == null) {
			return false;
		}
		if (dmd.getText().startsWith("/sgss ") || dmd.getText().startsWith("/sgsread ")) {
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
	public void execute(DiscordMessageData dmd) throws InterruptedException, ExecutionException {
		Server guild = dmd.getGuild();
		CsmpService csmpService = CsmpService.getInstance();
		Map<Long, Map<Object,Object>> guildScenarioInfo = csmpService.getGuildScenarioInfo();

		if (guildScenarioInfo.containsKey(guild.getId())) {
			dmd.getChannel().sendMessage("このサーバーは既にシナリオが登録されています。「/sgsclear」コマンドを使ってシナリオをクリアしてください。");
			return;
		}

		// シナリオ情報取得
		Map<Object, Object> secretMap = csmpService.getScenarioSheetInfo(dmd.getCommandArray()[1]);
		if (secretMap == null) {
			dmd.getChannel().sendMessage("シナリオシートのURLが誤っているか、公開中にチェックが入っていません。");
			return;
		}
		guildScenarioInfo.put(guild.getId(), secretMap);
		String scenarioName = (String)((Map<String, Object>)secretMap.get("base")).get("name");
		// シナリオ名のメッセージ出力
		dmd.getChannel().sendMessage("シナリオ：" + scenarioName);
		logger.info("シナリオ名：" + scenarioName);
		if ("/sgsread".equals(dmd.getCommandArray()[0])) {
			return;
		}

		Date current = Calendar.getInstance().getTime();
		SimpleDateFormat sdf = DateUtil.getDateFormat();
		new ServerUpdater(guild).setName(scenarioName + "_" + sdf.format(current)).update();

		Map<String, Role> roleMap = new HashMap<>();
		for (Role role : guild.getRoles()) {
			roleMap.put(role.getName().toLowerCase(), role);
		}

		Map<String, ServerTextChannel> channelMap = new HashMap<>();
		List<ServerTextChannel> channels = guild.getTextChannels();
		for (ServerTextChannel channel : channels) {
			channelMap.put(channel.getName().toLowerCase(), channel);
		}

		// PC情報の取得
		List<Map<String, Object>> pcList = (List<Map<String, Object>>)secretMap.get("pc");
		for (Map<String, Object> pcInfo : pcList) {
			String roleName = "PC" + CsmpService.text(pcInfo, "name");

			Role role = roleMap.get(roleName.toLowerCase());
			if (role == null) {
				role = guild.createRoleBuilder()
						.setName(roleName)
						.create().get();
				roleMap.put(roleName.toLowerCase(), role);
			}


			ServerTextChannel tc = channelMap.get(roleName.toLowerCase());

			if (tc == null) {
				ServerTextChannelBuilder stcBuilder = new ServerTextChannelBuilder(guild)
						.setName(roleName)
						.addPermissionOverwrite(
								guild.getEveryoneRole(), new PermissionsBuilder().setDenied(PermissionType.READ_MESSAGES).build())
						.addPermissionOverwrite(
								role, new PermissionsBuilder().setAllowed(PermissionType.READ_MESSAGES).build());

				dmd.getCategory().ifPresent(category -> stcBuilder.setCategory(category));

				tc = stcBuilder.create().get();

				channelMap.put(roleName.toLowerCase(), tc);
			}

			String textMessage = "■" + roleName + "　推奨：" +CsmpService.text(pcInfo, "recommend") + "\r\n" +
					"・使命：【" +CsmpService.text(pcInfo, "mission") + "】" + "\r\n" +
					"・導入：\r\n" +
					CsmpService.text(pcInfo, "intro") + "\r\n" +
					"・秘密：\r\n" +
					CsmpService.text(pcInfo, "secret");

			tc.sendMessage(textMessage);
		}
	}

	@Override
	public void warning(DiscordMessageData dmd) {
		dmd.getChannel().sendMessage("コマンドは「/sgss <シナリオシートURL>」と入力してください。");
	}

	@Override
	public CommandHelpData getCommandHelpData() {
		List<CommandHelpData> list = new ArrayList<>();
		list.add(new CommandHelpData(
				"/sgsread <シナリオシートURL>",
				"シナリオシートの情報の読み込みだけ行う。"));
		list.add(new CommandHelpData(
				"/sgss <シナリオシートURL>",
				"シナリオシートの情報を読み込んでチャンネル、権限を設定する。"));

		return new CommandHelpData(list);
	}

	@Override
	public SlashCommandBuilder entryCommand() {
		SlashCommandOption link = SlashCommandOption.create(
				SlashCommandOptionType.STRING, "シナリオシートurl",
				"キャラクターシート倉庫に登録しているシナリオシートのURLを指定してください。", true);

		SlashCommandOption readonly = SlashCommandOption.create(
				SlashCommandOptionType.STRING, "読み込み専用",
				"シナリオシートの読み込みだけ行い、チャンネルやロールの設定を行わない場合はtrueを指定してください。", false);

		return new SlashCommandBuilder().setName(getCommandName())
				.setDescription("シノビガミのシナリオシートを読み込み、PCごとのチャンネルとロールを作成し、秘密を配付します。")
				.addOption(link)
				.addOption(readonly)
				;
	}

	@Override
	public String getCommandName() {
		return "sgss";
	}

	@Override
	public void executeSlashCommand(DiscordMessageData dmd) throws InterruptedException, ExecutionException {
		SlashCommandInteraction interaction = dmd.getInteraction();
		List<SlashCommandInteractionOption> options = interaction.getOptions();

		String commandText = "";

		Map<String, SlashCommandInteractionOption> optionMap = new HashMap<>();
		for (SlashCommandInteractionOption option : options) {
			optionMap.put(option.getName(), option);
		}

		// シナリオシートURL
		SlashCommandInteractionOption urlOption = optionMap.get("シナリオシートurl");
		String url = urlOption.getStringValue().orElse(null);

		// 読み込み専用オプション
		SlashCommandInteractionOption readOnlyOption = optionMap.get("読み込み専用");
		if (readOnlyOption != null) {
			boolean readonly = readOnlyOption.getBooleanValue().orElse(false);

			if (readonly) {
				commandText = "/sgsread ";
			} else {
				commandText = "/sgss ";
			}
		}

		interaction.createFollowupMessageBuilder().setContent("シナリオのセットアップを開始します。").send();

		dmd.setText(commandText + url);

		execute(dmd);

	}
}
