package csmp.bot.command.sgs;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.server.Server;
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
import csmp.utl.DiscordUtil;

/**
 * シナリオ秘密送付コマンド.
 * @author kgmas
 *
 */
public class ScenarioSendSecretCommand implements IDiscordCommand,IDiscordSlashCommand {

	@Override
	public boolean judgeExecute(DiscordMessageData dmd) {
		if (dmd.getGuild() == null) {
			return false;
		}
		if (dmd.getText().startsWith("/sgssend ")) {
			return true;
		}
		return false;
	}

	@Override
	public boolean checkInput(DiscordMessageData dmd) {
		if (dmd.getCommandArray().length != 3) {
			return false;
		}
		return true;
	}

	@Override
	public void execute(DiscordMessageData dmd) throws InterruptedException, ExecutionException {
		Server guild = dmd.getGuild();
		Map<Object, Object> secretMap = CsmpService.getInstance().getGuildScenarioInfo().get(guild.getId());
		if (secretMap == null) {
			DiscordUtil.sendMessage("シナリオ情報が設定されていません。", dmd);
			return;
		}
		String secretName = dmd.getCommandArray()[1];
		String roleName = dmd.getCommandArray()[2];
		String textMessage = null;
		List<Map<String, Object>> pcList = (List<Map<String, Object>>)secretMap.get("pc");
		if (secretName.toUpperCase().startsWith("PC")) {
			for (Map<String, Object> map : pcList) {
				String name = "PC" +CsmpService.text(map, "name");
				if (secretName.equalsIgnoreCase(name)) {
					textMessage = "■" + name + "の秘密：\r\n" + CsmpService.text(map, "secret");
					break;
				}
			}
		}
		if (textMessage == null) {
			String secretNameEdit = "";
			if (secretName.toUpperCase().startsWith("NPC")) {
				secretNameEdit = secretName.substring(3);
			}
			List<Map<String, Object>> npcList = (List<Map<String, Object>>)secretMap.get("npc");

			for (Map<String, Object> map : npcList) {
				String name =CsmpService.text(map, "name");

				if (secretName.equalsIgnoreCase(name)
						|| secretNameEdit.equalsIgnoreCase(name)) {
					textMessage = "■" + name + "の秘密：\r\n" +CsmpService.text(map, "secret");
					break;
				}
			}
		}
		if (textMessage == null) {
			DiscordUtil.sendMessage(secretName + "の秘密がシナリオ情報内に見つかりません。", dmd);
			return;
		}
		List<ServerTextChannel> list = guild.getTextChannelsByName(roleName.toLowerCase());
		if (list.isEmpty()) {
			DiscordUtil.sendMessage(roleName + "のチャンネルがサーバ内に見つかりません。", dmd);
			return;
		} else {
		}

		list.get(0).sendMessage(textMessage);

		DiscordUtil.sendMessage(roleName + "のチャンネルに" + secretName + "の秘密を送りました。", dmd, false);

	}

	@Override
	public void warning(DiscordMessageData dmd) {
		DiscordUtil.sendMessage("コマンドは「/sgssend <秘密名> <送る先のチャンネル名>」と入力してください。", dmd);
	}

	@Override
	public CommandHelpData getCommandHelpData() {
		return new CommandHelpData(
				"/sgssend <秘密名> <PC名>",
				"PC名のチャンネルに指定された秘密を張り付ける。");
	}

	@Override
	public SlashCommandBuilder entryCommand() {
		SlashCommandOption secret = SlashCommandOption.create(
				SlashCommandOptionType.STRING, "秘密名",
				"シナリオシート上の秘密名を指定してください。PCの秘密の場合は先頭にPCとつけてください。", true);

		SlashCommandOption channel = SlashCommandOption.create(
				SlashCommandOptionType.CHANNEL, "送付先チャンネル",
				"秘密を送付するチャンネルを選択してください。", true);


		return new SlashCommandBuilder().setName(getCommandName())
				.setDescription("シナリオシートの秘密を特定のチャンネルに対して送付します。")
				.addOption(secret)
				.addOption(channel)
				;
	}

	@Override
	public String getCommandName() {
		return "sgssend";
	}

	@Override
	public void executeSlashCommand(DiscordMessageData dmd) throws InterruptedException, ExecutionException {
		SlashCommandInteraction interaction = dmd.getInteraction();
		List<SlashCommandInteractionOption> options = interaction.getOptions();

		String commandText = "/sgssend " + options.get(0).getStringValue().orElse("") + " ";

		ServerChannel channel = options.get(1).getChannelValue().orElse(null);
		if (channel == null) {
			DiscordUtil.sendMessage("チャンネルが正しく指定できていません。", dmd);
			return;
		}

		commandText += channel.getName();

		dmd.setText(commandText);

		execute(dmd);


	}
}
