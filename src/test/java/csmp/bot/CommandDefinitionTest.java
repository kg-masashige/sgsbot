package csmp.bot;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import csmp.bot.command.ISlashCommand;
import csmp.bot.command.bcdice.TableSetupCommand;
import csmp.bot.command.help.HelpCommand;
import csmp.bot.controller.DiscordBotController;
import csmp.bot.controller.DiscordBotController.IntentType;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.modals.Modal;

/**
 * コマンド定義のテスト.
 * JDAはコマンド名・説明文などがDiscordの制約に合わない場合、定義の作成時に例外を投げる。
 */
class CommandDefinitionTest {

	private static List<SlashCommandData> createAll() {
		List<ISlashCommand> commands = DiscordSessionSupportBot.createCommandList(true, true, true);
		return new DiscordBotController(commands, null, "dummy").createCommandDataList();
	}

	@Test
	void すべてのスラッシュコマンドを定義できる() {
		List<SlashCommandData> list = createAll();
		Set<String> names = list.stream().map(SlashCommandData::getName).collect(Collectors.toSet());

		assertEquals(Set.of("sgshelp", "schedule", "roll", "sgstable", "plot", "openplot", "link",
				"bcdicesearch", "bcdiceset", "bcdiceget", "sgss", "sgssend", "sgsclear"), names);
		for (SlashCommandData data : list) {
			// Discordへ送るJSONに変換できること（制約違反があれば例外になる）
			assertNotNull(data.toData().toString(), data.getName());
		}
	}

	@Test
	void サービスを絞るとそのコマンドだけになる() {
		List<ISlashCommand> commands = DiscordSessionSupportBot.createCommandList(true, false, false);
		List<SlashCommandData> list = new DiscordBotController(commands, null, "dummy").createCommandDataList();
		assertEquals(List.of("sgshelp", "schedule"), list.stream().map(SlashCommandData::getName).toList());
	}

	@Test
	void オリジナル表の登録モーダルを作成できる() {
		Modal modal = TableSetupCommand.createModal();
		assertEquals(TableSetupCommand.MODAL_ID, modal.getId());
		assertTrue(modal.getId().startsWith("sgstable:"));
		assertNotNull(modal.toData().toString());
	}

	@Test
	void ヘルプに有効なコマンドが含まれる() {
		HelpCommand help = new HelpCommand(DiscordSessionSupportBot.createCommandList(true, true, true));
		String message = help.getMessage();
		for (String command : List.of("/sgshelp", "/schedule", "/roll", "/sgstable 登録", "/plot", "/openplot",
				"/link", "/bcdicesearch", "/bcdiceset", "/bcdiceget", "/sgss", "/sgssend", "/sgsclear")) {
			assertTrue(message.contains(command), command);
		}
	}

	@Test
	void INTENT_TYPEの解釈() {
		assertEquals(IntentType.COMMANDONLY, IntentType.of(null));
		assertEquals(IntentType.COMMANDONLY, IntentType.of(""));
		assertEquals(IntentType.MEMBER_CHANGE, IntentType.of("MEMBER_CHANGE"));
		assertFalse(IntentType.COMMANDONLY.isMemberEventEnabled());
		assertTrue(IntentType.ALL.isMemberEventEnabled());
		assertThrows(IllegalArgumentException.class, () -> IntentType.of("UNKNOWN"));
	}

}
