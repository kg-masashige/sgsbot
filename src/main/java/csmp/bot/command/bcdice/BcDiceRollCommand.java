package csmp.bot.command.bcdice;

import java.util.concurrent.ExecutionException;

import org.javacord.api.entity.message.embed.EmbedBuilder;

import csmp.bot.command.IDiscordCommand;
import csmp.bot.model.CommandHelpData;
import csmp.bot.model.DiscordMessageData;
import csmp.service.BcDiceApiService;

/**
 * ダイスボット実行コマンド.
 * @author kgmas
 *
 */
public class BcDiceRollCommand implements IDiscordCommand {

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
					new EmbedBuilder().setAuthor(dmd.getMessage().getMessageAuthor())
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
				"DiceBotコマンド(2D6など)",
				"ダイスボットのコマンドを実行する。シークレットダイスは対応していない。");
	}

}
