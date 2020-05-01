# シノビガミセッションサポートbot

[忍術バトルRPGシノビガミ -忍神-](http://www.bouken.jp/pd/sg/)をDiscordを使って遊びやすくするためのbotです。
シノビガミ以外のセッションでもご利用いただけます。


## 使用方法
１．[こちらのリンク](https://discordapp.com/oauth2/authorize?client_id=611880245707931648&permissions=8&scope=bot)から自分が管理しているサーバにbotを招待する。

２．/sgshelp と発言して使用方法を確認する。



使用できる機能は以下の通りです。

### 秘密配付機能
[シノビガミシナリオ登録サイト](https://character-sheets.appspot.com/sgScenario/)と連動し、プレイヤーに秘密を配ることができます。

[サンプルシナリオ](https://character-sheets.appspot.com/sgScenario/detail?key=ahVzfmNoYXJhY3Rlci1zaGVldHMtbXByFwsSDUNoYXJhY3RlckRhdGEY1uz8sgIM)を取り込む場合、
botを参加させたサーバで以下の通り発言してください。

~~~
/sgss https://character-sheets.appspot.com/sgScenario/detail?key=ahVzfmNoYXJhY3Rlci1zaGVldHMtbXByFwsSDUNoYXJhY3RlckRhdGEY1uz8sgIM
~~~

すると、PC1～PC4までのチャンネルが作られ、それぞれの導入、使命、秘密が貼られます。<br>
チャンネルはそれぞれのPC番号に沿ったDiscordの役割が割り振られるため、役割を持った人とサーバ管理者しか観ることができません。<br>
サーバ管理者は各プレイヤーにPC番号の役割を割り振ってください。<br>

### セッション予定管理機能
セッションの次回予定日を登録し、リマインドすることができます。

botを参加させたサーバで以下の通り発言してください。

~~~
/scheadd 9/16,9/17
~~~

すると、
ShinobigamiScenarioSetupBOT
2019/09/16,2019/09/17を登録しました。

と返事があります。

登録した日付がみたい場合は

~~~
/scheshow
~~~

と発言してください。

すると<br>
予定日は：<br>
2019/09/16(月)<br>
2019/09/17(火)<br>

と表示されます。

登録した日付の日本時間0時になると<br>
2019/09/16はセッション予定日です。<br>
<リマインドメッセージ><br>

と表示されます。



### ダイスボット機能
どどんとふなどで採用されているBCDice機能を利用してダイスを振ることができます。

セッション開始時や再開時に

~~~
/set system シノビガミ
~~~

と発言してください。
すると指定したシステムのダイスボットを振ることができます。

システムを設定したあとは、

~~~
2D6
~~~

などとコマンドを半角で入力することでダイスを振ることができます。



#### キャラクターシート倉庫との連携
[シノビガミ用キャラクターシート](https://character-sheets.appspot.com/shinobigami/)と連携してダイスを振ることができます。

１．キャラクターシートのDiscord連携チェックに✓を入れてキャラクターシートを登録してください。

２．登録したキャラクターシートのURL（[例](https://character-sheets.appspot.com/shinobigami/edit.html?key=ahVzfmNoYXJhY3Rlci1zaGVldHMtbXByFwsSDUNoYXJhY3RlckRhdGEY-eKkrwEM)）をコピーし、Discord上で「/link <コピーしたURL>」と発言してください。

~~~
/link https://character-sheets.appspot.com/shinobigami/edit.html?key=ahVzfmNoYXJhY3Rlci1zaGVldHMtbXByFwsSDUNoYXJhY3RlckRhdGEY-eKkrwEM
~~~

３．キャラクターシートのDRボタンで判定を行うことができます。


### プロット機能
シノビガミのルールで使用するプロット機能を使用することができます。

１．導入したbotに対してDiscord上でダイレクトメッセージを送ってください。<br>
（例　「プロット：6」、「影分身で2と5」など）

２．プロットを公開したいチャンネルで、「/plot who」と発言してください。

すると、参加者のうち誰がプロットしているかが表示されます。

３．プロットを公開する時は、「/plot open」と発言してください。


### 利用しているサービス
[BCDice](https://github.com/bcdice/BCDice)

日本のデファクトスタンダードなオンセツール用ダイスエンジン。[修正BSDライセンス](https://github.com/bcdice/BCDice/blob/master/LICENSE)でライセンスされています。



[BCDice-API](https://github.com/ysakasin/bcdice-api)

BCDiceが振れるWebAPI。[MITライセンス](https://github.com/ysakasin/bcdice-api/blob/master/LICENSE)でライセンスされています。



[どどんとふ公式鯖](https://dodontof.onlinesession.app/)

どどんとふ日本国内最大の公開サーバー。設置されているBCDice-APIを使用。


[キャラクターシート倉庫](https://character-sheets.appspot.com/)

キャラクターシートを管理するためのサイト。シノビガミのキャラシ、シナリオシートの管理、スケジュール管理の連携先として使用。


### 使用しているライブラリ
[javacord](https://github.com/Javacord/Javacord)

Discordとの通信で使用。[Apache License 2.0](https://github.com/Javacord/Javacord/blob/master/LICENSE)でライセンスされています。


[RESTeasy](https://github.com/resteasy/Resteasy)

Dicebot、キャラクターシートへの連携で使用。[Apache License 2.0](https://github.com/resteasy/Resteasy/blob/master/License.html)でライセンスされています。


[JSONIC](https://github.com/hidekatsu-izuno/jsonic)

JSONの解析、作成に使用。[Apache License 2.0](https://github.com/hidekatsu-izuno/jsonic/blob/master/LICENSE)でライセンスされています。


### 参考にしたサービス
[BCDice bot for Discord](https://github.com/Shunshun94/discord-bcdicebot)

DiscordでBCDiceを振ることに特化したbot。BCDiceとの連携処理、Javacordの使用方法などで参考にさせていただきました。



### 連絡先
bot作成者への連絡は[Twitter](https://twitter.com/kg_masashige/)でお願いします。<br>
不具合などご報告いただけるとたすかります。

### 支援
支援は[pixivFANBOX](https://kg-masashige.fanbox.cc/)へいただけると助かります。
[キャラクターシート倉庫](https://character-sheets.appspot.com/)トップからのAmazonギフト券でも受け付けております。
