package csmp.bot.command.sgs;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import csmp.bot.command.ISlashCommand;
import csmp.bot.model.CommandHelpData;
import csmp.service.CsmpService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

/**
 * シナリオ情報クリアコマンド.
 * @author kgmas
 *
 */
public class ScenarioClearCommand implements ISlashCommand {

	@SuppressWarnings("unchecked")
	@Override
	public void execute(SlashCommandInteractionEvent event) {
		Guild guild = event.getGuild();
		Map<Object, Object> secretMap = CsmpService.getInstance().getGuildScenarioInfo().remove(guild.getIdLong());
		if (secretMap == null) {
			event.reply("シナリオ情報が設定されていません。").queue();
			return;
		}
		event.reply("シナリオ情報をクリアします。").queue();

		Set<String> pcNames = new HashSet<>();
		for (Map<String, Object> pcInfo : (List<Map<String, Object>>)secretMap.get("pc")) {
			pcNames.add("PC" + CsmpService.text(pcInfo, "name"));
		}

		for (TextChannel channel : guild.getTextChannels()) {
			for (String pcName : pcNames) {
				if (pcName.equalsIgnoreCase(channel.getName())) {
					channel.delete().complete();
					break;
				}
			}
		}

		for (Role role : guild.getRoles()) {
			if (pcNames.contains(role.getName())) {
				role.delete().complete();
			}
		}

		event.getHook().sendMessage("役職とチャンネルを削除しました。").queue();
	}

	@Override
	public CommandHelpData getCommandHelpData() {
		return new CommandHelpData(
				"/sgsclear",
				"シナリオシートの情報、チャンネル、権限を削除する。");
	}

	@Override
	public SlashCommandData createCommandData() {
		return Commands.slash(getCommandName(), "シナリオシートの情報、チャンネル、権限を削除します。")
				.setContexts(InteractionContextType.GUILD);
	}

	@Override
	public String getCommandName() {
		return "sgsclear";
	}

}
