package csmp.bot.controller;

import java.util.ArrayList;
import java.util.List;

import csmp.bot.command.IDiscordCommand;
import csmp.bot.command.help.HelpCommand;
import csmp.bot.model.DiscordMessageData;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;

public class DiscordBotController extends Thread {

	/**
	 * コマンドリスト.
	 */
	private List<IDiscordCommand> commandList;

	/**
	 * discordクライアントインスタンス.
	 */
	private DiscordClient client;

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
	@Override
	public void run() {
		System.out.println("Botを起動中...");

		client = new DiscordClientBuilder(token).build();

		client.getEventDispatcher().on(ReadyEvent.class)
		.subscribe(ready -> {
			System.out.println("Logged in as " + ready.getSelf().getUsername());
		});

		client.getEventDispatcher().on(MessageCreateEvent.class)
		.subscribe(event -> {
			Message message = event.getMessage();

			DiscordMessageData dmd = new DiscordMessageData(message);
			try {
				for(IDiscordCommand command : commandList) {
					if (command.judgeExecute(dmd)) {
						if (command.checkInput(dmd)) {
							command.execute(dmd);
						} else {
							command.warning(dmd);
						}
						break;
					}
				}

			} catch (Throwable e) {
				System.out.println("error command:" + dmd.getText());
				e.printStackTrace();
				message.getChannel().block().createMessage("エラーが発生しました。Twitter ID:@kg_masashigeまで連絡してください。");
			}

		 });

		client.login().block();


	}

}
