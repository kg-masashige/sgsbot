package csmp.bot.command.bcdice;

import csmp.bot.command.ISlashCommand;
import csmp.bot.model.CommandHelpData;
import csmp.service.BcDiceApiService;
import csmp.utl.DiscordUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

/**
 * ダイスボット実行コマンド.
 * オリジナル表（/sgstable）に登録したコマンドと一致した場合は、表を振る。
 * @author kgmas
 *
 */
public class BcDiceRollCommand implements ISlashCommand {

	private static final String OPTION_COMMAND = "コマンド";

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		String commandText = event.getOption(OPTION_COMMAND).getAsString();
		event.reply("判定:" + commandText).queue();

		BcDiceApiService service = BcDiceApiService.getInstance();
		long guildId = event.getGuild().getIdLong();
		String result = service.rollTable(guildId, commandText);
		if (result == null) {
			result = service.rollDice(commandText, guildId);
		}
		if (result == null) {
			// ダイスのコマンドとして解釈できない場合は何もしない。
			return;
		}

		EmbedBuilder embed = new EmbedBuilder()
				.setAuthor(DiscordUtil.getDisplayName(event), null, getAvatarUrl(event))
				.addField(DiscordUtil.truncate(commandText, MessageEmbed.TITLE_MAX_LENGTH),
						DiscordUtil.truncate(result, MessageEmbed.VALUE_MAX_LENGTH), false);
		event.getChannel().sendMessageEmbeds(embed.build()).queue();
	}

	private String getAvatarUrl(SlashCommandInteractionEvent event) {
		if (event.getMember() != null) {
			return event.getMember().getEffectiveAvatarUrl();
		}
		return event.getUser().getEffectiveAvatarUrl();
	}

	@Override
	public CommandHelpData getCommandHelpData() {
		return new CommandHelpData(
				"/roll <DiceBotコマンド(2D6など)>",
				"ダイスボットのコマンドを実行する。/sgstableで登録したコマンドを指定すると表を振る。シークレットダイスは対応していない。");
	}

	@Override
	public SlashCommandData createCommandData() {
		return Commands.slash(getCommandName(), "BCDiceを使用して判定します。")
				.setContexts(InteractionContextType.GUILD)
				.addOption(OptionType.STRING, OPTION_COMMAND, "BCDiceで判定したいコマンドを指定してください。", true);
	}

	@Override
	public String getCommandName() {
		return "roll";
	}

}
