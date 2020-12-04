package csmp.bot.command.sgs;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.server.Server;

import csmp.bot.command.IDiscordCommand;
import csmp.bot.model.CommandHelpData;
import csmp.bot.model.DiscordMessageData;
import csmp.service.CsmpService;

/**
 * シナリオ秘密送付コマンド.
 * @author kgmas
 *
 */
public class ScenarioSendSecretCommand implements IDiscordCommand {

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
			dmd.getChannel().sendMessage("シナリオ情報が設定されていません。");
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
			dmd.getChannel().sendMessage(secretName + "の秘密がシナリオ情報内に見つかりません。");
			return;
		}
		List<ServerTextChannel> list = guild.getTextChannelsByName(roleName.toLowerCase());
		if (list.isEmpty()) {
			dmd.getChannel().sendMessage(roleName + "のチャンネルがサーバ内に見つかりません。");
			return;
		} else {
		}

		list.get(0).sendMessage(textMessage);

	}

	@Override
	public void warning(DiscordMessageData dmd) {
		dmd.getChannel().sendMessage("コマンドは「/sgssend <秘密名> <送る先のチャンネル名>」と入力してください。");
	}

	@Override
	public CommandHelpData getCommandHelpData() {
		return new CommandHelpData(
				"/sgssend <秘密名> <PC名>",
				"PC名のチャンネルに指定された秘密を張り付ける。");
	}
}
