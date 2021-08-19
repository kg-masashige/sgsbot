package csmp.bot.command.character;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

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

public class LinkCharacterSheetCommand implements IDiscordCommand,IDiscordSlashCommand {

	@Override
	public boolean judgeExecute(DiscordMessageData dmd) {
		if (dmd.getGuild() == null) {
			return false;
		}

		if (dmd.getText().startsWith("/link http")) {
			return true;
		}

		return false;
	}

	@Override
	public boolean checkInput(DiscordMessageData dmd) {
		return true;
	}


	@Override
	public void execute(DiscordMessageData dmd) throws InterruptedException, ExecutionException {
		String sheetUrl = dmd.getCommandArray()[1];

		String webhookUrl = DiscordUtil.getWebhookUrl(dmd);

		if (webhookUrl == null) {
			return;
		}

		Map<String, Object> result = CsmpService.getInstance().registerCharacterSheet(webhookUrl, sheetUrl);
		if (result != null && "ok".equals(result.get("result"))) {
			dmd.getChannel().sendMessage("キャラクターシートを登録しました。キャラクターシートでDRボタンを押してください。");

		} else {
			dmd.getChannel().sendMessage("キャラクターシートの登録に失敗しました。再度コマンドを実行してください。");

		}

	}

	@Override
	public void warning(DiscordMessageData dmd) {
	}

	@Override
	public CommandHelpData getCommandHelpData() {
		return new CommandHelpData(
				"/link <キャラクターシートURL>",
				"キャラクターシート上でDRボタンを押した時に結果をこのチャンネルに反映させる。");
	}

	@Override
	public SlashCommandBuilder entryCommand() {
		SlashCommandOption link = SlashCommandOption.create(
				SlashCommandOptionType.STRING, "キャラクターシートurl",
				"キャラクターシート倉庫に登録しているキャラクターシートのURLを指定してください。", true);

		return new SlashCommandBuilder().setName(getCommandName())
				.setDescription("キャラクターシート上のDRボタンを押した時に結果をこのチャンネルに反映させることができます。")
				.addOption(link)
				;
	}

	@Override
	public String getCommandName() {
		return "link";
	}

	@Override
	public void executeSlashCommand(DiscordMessageData dmd) throws InterruptedException, ExecutionException {
		SlashCommandInteraction interaction = dmd.getInteraction();
		List<SlashCommandInteractionOption> options = interaction.getOptions();

		String commandText = "/link " + options.get(0).getStringValue().orElse("");

		interaction.createFollowupMessageBuilder().setContent("キャラクターシートの紐付けを行います。").send();

		dmd.setText(commandText);

		execute(dmd);


	}

}
