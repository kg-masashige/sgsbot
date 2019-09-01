package csmp.bot.command.sgs;

import java.util.List;
import java.util.Map;

import csmp.bot.command.DiscordCommandBase;
import csmp.bot.model.DiscordMessageData;
import csmp.service.CsmpService;
import csmp.utl.DiscordUtil;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.TextChannel;

/**
 * シナリオ秘密送付コマンド.
 * @author kgmas
 *
 */
public class ScenarioSendSecretCommand extends DiscordCommandBase {

	@Override
	public boolean judgeExecute(DiscordMessageData dmd) {
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
	public void execute(DiscordMessageData dmd) {
		Guild guild = dmd.getGuild();
		Map<Object, Object> secretMap = CsmpService.getGuildScenarioInfo().get(guild.getId());
		if (secretMap == null) {
			dmd.getChannel().createMessage("シナリオ情報が設定されていません。").block();
			return;
		}
		String secretName = dmd.getCommandArray()[1];
		String roleName = dmd.getCommandArray()[2];
		String textMessage = null;
		List<Map<String, Object>> pcList = (List<Map<String, Object>>)secretMap.get("pc");
		if (secretName.toUpperCase().startsWith("PC")) {
			for (Map<String, Object> map : pcList) {
				String name = "PC" +CsmpService.text(map, "name");
				if (secretName.equals(name)) {
					textMessage = "■" + name + "の秘密：\r\n" + CsmpService.text(map, "secret");
					break;
				}
			}
		}
		if (textMessage == null) {
			List<Map<String, Object>> npcList = (List<Map<String, Object>>)secretMap.get("npc");
			for (Map<String, Object> map : npcList) {
				String name =CsmpService.text(map, "name");
				if (secretName.equals(name)) {
					textMessage = "■" + name + "の秘密：\r\n" +CsmpService.text(map, "secret");
					break;
				}
			}
		}
		if (textMessage == null) {
			dmd.getChannel().createMessage(secretName + "の秘密がシナリオ情報内に見つかりません。").block();
			return;
		}
		TextChannel tc = DiscordUtil.getTextChannelByName(guild, roleName);
		if (tc == null) {
			dmd.getChannel().createMessage(roleName + "のチャンネルがサーバ内に見つかりません。").block();
			return;
		}

		tc.createMessage(textMessage).block();

	}

	@Override
	public void help(DiscordMessageData dmd) {
		dmd.getChannel().createMessage("コマンドは「/sgssend <秘密名> <送る先のチャンネル名>」と入力してください。").block();
	}

}
