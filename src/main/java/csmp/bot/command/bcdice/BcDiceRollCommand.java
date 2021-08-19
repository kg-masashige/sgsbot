package csmp.bot.command.bcdice;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;

import csmp.bot.command.IDiscordCommand;
import csmp.bot.command.IDiscordSlashCommand;
import csmp.bot.model.CommandHelpData;
import csmp.bot.model.DiscordMessageData;
import csmp.service.BcDiceApiService;

/**
 * ダイスボット実行コマンド.
 * @author kgmas
 *
 */
public class BcDiceRollCommand implements IDiscordCommand, IDiscordSlashCommand {

	@Override
	public boolean judgeExecute(DiscordMessageData dmd) {
		if (dmd.getGuild() == null) {
			return false;
		}
		return true;
	}

	@Override
	public boolean checkInput(DiscordMessageData dmd) {
		return true;
	}

	@Override
	public void execute(DiscordMessageData dmd) throws InterruptedException, ExecutionException {
		String result = BcDiceApiService.getInstance().rollDice(dmd.getText(), dmd.getGuild().getId());

		if (result != null) {
			dmd.getChannel().sendMessage(
					new EmbedBuilder().setAuthor(dmd.getUser())
						.addField(dmd.getText(), result)
					);
		}

	}

	@Override
	public void warning(DiscordMessageData dmd) {
	}

	@Override
	public CommandHelpData getCommandHelpData() {
		return new CommandHelpData(
				"/roll DiceBotコマンド(2D6など) (旧コマンド：コマンドなし)",
				"ダイスボットのコマンドを実行する。シークレットダイスは対応していない。");
	}

	@Override
	public SlashCommandBuilder entryCommand() {
		SlashCommandOption command = SlashCommandOption.create(
				SlashCommandOptionType.STRING, "コマンド",
				"BCDiceで判定したいコマンドを指定してください。", true);

		return new SlashCommandBuilder().setName(getCommandName())
				.setDescription("BCDiceを使用して判定します。")
				.addOption(command)
				;
	}

	@Override
	public String getCommandName() {
		// TODO 自動生成されたメソッド・スタブ
		return "roll";
	}

	@Override
	public void executeSlashCommand(DiscordMessageData dmd) throws InterruptedException, ExecutionException {

		SlashCommandInteraction interaction = dmd.getInteraction();
		List<SlashCommandInteractionOption> options = interaction.getOptions();

		String commandText = options.get(0).getStringValue().orElse("");

		interaction.createImmediateResponder().setContent("判定:" + commandText).respond();

		dmd.setText(commandText);

		execute(dmd);

	}

}
