package csmp.bot.command.help;

import csmp.bot.command.DiscordCommandBase;
import csmp.bot.model.DiscordMessageData;

/**
 * ヘルプコマンド.
 * @author kgmas
 *
 */
public class HelpCommand extends DiscordCommandBase {

	@Override
	public boolean judgeExecute(DiscordMessageData dmd) {
		if ((dmd.getText().startsWith("/sche") || dmd.getText().startsWith("/sgs"))
				&& dmd.getText().contains("help")) {
			return true;
		}
		return false;
	}

	@Override
	public boolean checkInput(DiscordMessageData dmd) {
		return false;
	}

	@Override
	public void execute(DiscordMessageData dmd) {
	}

}
