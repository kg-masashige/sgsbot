package csmp.bot.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
	public void execute() {
		System.out.println("Botを起動中...");
		Integer threadPoolCount = Integer.valueOf(System.getenv("THREAD_POOL_COUNT"));
		System.out.println("スレッドプール：" + threadPoolCount);
		if (threadPoolCount == null) {
			threadPoolCount = 1;
		}

		DiscordApi api = new DiscordApiBuilder().setToken(token).login().join();

		ExecutorService exec = Executors.newFixedThreadPool(threadPoolCount);

		api.addMessageCreateListener(event -> {
			DiscordMessageData dmd = new DiscordMessageData(event);

			try {
				for (IDiscordCommand command : commandList) {
					if (command.judgeExecute(dmd)) {

						// コマンドのチェック、実行処理をスレッド化する
						exec.execute(new Runnable() {
							@Override
							public void run() {
								if (command.checkInput(dmd)) {
									try {
										command.execute(dmd);
									} catch (Throwable e) {
										handleError(dmd, e);
									}
								} else {
									command.warning(dmd);
								}
							}
			        	});

						break;
					}
				}

			} catch (Throwable e) {
				handleError(dmd, e);
			}

		});

		api.addServerJoinListener(event -> event.getServer().getSystemChannel()
				.ifPresent(channel -> channel.sendMessage("TRPGセッション（主にシノビガミ）を行うためのbotです。\r\n"
						+ "「/sgshelp」で実行可能なコマンドを確認できます。\r\n"
						+ "詳細は https://github.com/kg-masashige/sgsbot をご確認ください。")));

		System.out.println("Botの起動完了.");

	}

	private void handleError(DiscordMessageData dmd, Throwable e) {
		System.out.println("error command:" + dmd.getText());
		e.printStackTrace();
		dmd.getChannel().sendMessage("エラーが発生しました。Twitter ID:@kg_masashigeまで連絡してください。");
	}

}
