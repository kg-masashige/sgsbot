package csmp.bot.controller;

import java.util.ArrayList;
import java.util.List;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;

import csmp.bot.command.IDiscordCommand;
import csmp.bot.command.help.HelpCommand;
import csmp.bot.model.DiscordMessageData;

public class DiscordBotController {

	/**
	 * コマンドリスト.
	 */
	private List<IDiscordCommand> commandList;

	/**
	 * トークン.
	 */
	private String token;

	/**
	 * コンストラクタ.
	 * @param classList コマンド配列
	 * @param token Discord botトークン
	 */
	public DiscordBotController(List<Class<? extends IDiscordCommand>> classList, String token) {
		this.token = token;
		commandList = new ArrayList<>();
		List<IDiscordCommand> commandListForHelp = new ArrayList<>();
		for (Class<? extends IDiscordCommand> clazz : classList) {
			IDiscordCommand command;
			try {
				command = clazz.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
				continue;
			}
			commandListForHelp.add(command);
		}
		HelpCommand hc = new HelpCommand();
		hc.setCommandList(commandListForHelp);
		commandList.add(hc);
		commandList.addAll(commandListForHelp);
	}

	/**
	 * 実行メソッド.
	 */
	public void execute(String joinMessage) {
		System.out.println("Botを起動中...");
		DiscordApi api = new DiscordApiBuilder().setToken(token).login().join();

		api.addMessageCreateListener(event -> {
			DiscordMessageData dmd = new DiscordMessageData(event);

			try {
				for (IDiscordCommand command : commandList) {
					if (command.judgeExecute(dmd)) {

						if (command.checkInput(dmd)) {
							try {
								command.execute(dmd);
							} catch (Throwable e) {
								handleError(dmd, e);
							}
						} else {
							command.warning(dmd);
						}
						break;
					}
				}

			} catch (Throwable e) {
				handleError(dmd, e);
			}

		});

		api.addServerJoinListener(event -> event.getServer().getSystemChannel()
				.ifPresent(channel -> channel.sendMessage(joinMessage)));

		System.out.println("Botの起動完了.");

	}

	private void handleError(DiscordMessageData dmd, Throwable e) {
		System.out.println("error command:" + dmd.getText());
		e.printStackTrace();
		dmd.getChannel().sendMessage("エラーが発生しました。Twitter ID:@kg_masashigeまで連絡してください。");
	}

}
