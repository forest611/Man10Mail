package net.testusuke.open.man10mail.DataBase

import net.testusuke.open.man10mail.MailUtil
import net.testusuke.open.man10mail.Main.Companion.plugin
import net.testusuke.open.man10mail.Main.Companion.prefix
import org.bukkit.Bukkit
import java.sql.Statement
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Created by testusuke on 2020/07/04
 * @author testusuke
 */
object MailConsole {

    /**
     * function of send mail.
     * @param from[String] 発信元(uuid or name)
     * @param to[String] 送信先(uuid)
     * @param title[String] タイトル
     * @param tag[String] タグ
     * @param message[String] メッセージ [;]で改行
     * @param senderType[MailSenderType] uuid/mcid=PLAYER server=SERVER custom sender=CUSTOM(先頭に[#]が代入される。)
     * @return mailResult[MailResult<V>]
     */
    fun sendMail(from: String, to: String, title: String, tag: String, message: String, senderType: MailSenderType): MailResult {
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val formatted = current.format(formatter)
        val formattedFrom = formatFromUser(from,senderType)
        var sql = "INSERT INTO mail_list (to_player,from_player,title,message,tag,date) VALUES('${to}','$formattedFrom','${title}','${message}','${MailUtil.convertTag(tag)}','${formatted}');"
        plugin.dataBase.open()
        val connection = plugin.dataBase.connection
        if (connection == null) {
            plugin.dataBase.sendErrorMessage()
            return MailResult.Error(MailErrorReason.CAN_NOT_ACCESS_DB)
        }
        val statement = connection.createStatement()

        statement.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS)
        val resultSet = statement.generatedKeys
        resultSet.next()
        val id = resultSet.getInt(1)
        resultSet.close()
        statement.close()

        //  サーバー内にユーザーがいる場合は通知
        Bukkit.getPlayer(UUID.fromString(to))?.sendMessage("${prefix}§6新しいメールが届いています。")

        return MailResult.Success(id)
    }

    /**
     * function of send mail to everyone.
     * @param from[String] 発信元
     * @param title[String] タイトル
     * @param tag[String] タグ
     * @param message[String] メッセージ [;]で改行
     */
    fun issueEveryoneMail(from: String, title: String, tag: String, message: String, senderType: MailSenderType): MailResult {
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val formatted = current.format(formatter)
        val formattedFrom = formatFromUser(from,senderType)
        var sql = "INSERT INTO mail_all (from_name,title,message,tag,`date`) VALUES('$formattedFrom','${title}','${message}','${MailUtil.convertTag(tag)}','${formatted}');"
        plugin.dataBase.open()
        val connection = plugin.dataBase.connection
        if (connection == null) {
            plugin.dataBase.sendErrorMessage()
            return MailResult.Error(MailErrorReason.CAN_NOT_ACCESS_DB)
        }
        val statement = connection.createStatement()

        statement.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS)
        val resultSet = statement.generatedKeys
        resultSet.next()
        val id = resultSet.getInt(1)

        //  close
        resultSet.close()
        statement.close()

        //  サバ内のプレイヤーに通知
        for (player in Bukkit.getOnlinePlayers()) {
            val uuid = player.uniqueId.toString()
            sendEveryoneMail(uuid)
            player.sendMessage("${prefix}§6新しいメールが届いています。")
        }

        return MailResult.Success(id)
    }

    /**
     * function of send mail in 'mail_all' table to 'mail_list'
     * @param uuid[String] ターゲット uuid
     * @return amount[Int] 読み込んだカラム数
     */
    fun sendEveryoneMail(uuid: String): MailResult {
        plugin.dataBase.open()
        val connection = plugin.dataBase.connection
        if (connection == null) {
            plugin.dataBase.sendErrorMessage()
            return MailResult.Error(MailErrorReason.CAN_NOT_ACCESS_DB)
        }
        val selectReadSQL = "SELECT from_mail_id FROM mail_read WHERE to_player='${uuid}';"
        val selectReadStatement = connection.createStatement()
        val selectReadResult = selectReadStatement.executeQuery(selectReadSQL)
        //  送信済みのMail ID
        val readMailList = mutableListOf<Int>()
        while (selectReadResult.next()) {
            readMailList.add(selectReadResult.getInt("from_mail_id"))
        }
        //  close
        selectReadResult.close()
        selectReadStatement.close()

        //  Mail All Table
        val selectAllSQL = "SELECT * FROM mail_all;"
        val selectAllStatement = connection.createStatement()
        val selectAllResult = selectAllStatement.executeQuery(selectAllSQL)
        //  create statement
        val insertMailStatement = connection.createStatement()
        var amount = 0
        while (selectAllResult.next()) {
            if (!readMailList.contains(selectAllResult.getInt("id"))) {
                val from = selectAllResult.getString("from_name")
                val title = selectAllResult.getString("title")
                val tag = selectAllResult.getString("tag")
                val message = selectAllResult.getString("message")
                val date = selectAllResult.getString("date")
                val insertMailSQL = "INSERT INTO mail_list (to_player,from_player,title,message,tag,date) VALUES('${uuid}','${from}','${title}','${message}','${MailUtil.convertTag(tag)}','${date}');"
                insertMailStatement.executeUpdate(insertMailSQL)

                val insertMailReadSQL = "INSERT INTO mail_read (to_player,from_mail_id) VALUES ('${uuid}','${selectAllResult.getInt("id")}');"
                insertMailStatement.executeUpdate(insertMailReadSQL)
                amount++
            }
        }
        selectAllResult.close()
        selectAllStatement.close()
        insertMailStatement.close()

        return MailResult.Success(amount)
    }

    /**
     * function of remove mail with id
     * @param id[Int] id
     * @return success[Boolean]
     */
    fun removeMail(id: Int): Boolean {
        plugin.dataBase.open()
        val connection = plugin.dataBase.connection
        if (connection == null) {
            plugin.dataBase.sendErrorMessage()
            return false
        }
        val sql = "DELETE FROM mail_all WHERE id='$id';"
        val statement = connection.createStatement()
        statement.executeUpdate(sql)
        statement.close()
        return true
    }

    data class MailInformation(val id: Int, val from: String, val to: String, val title: String, val message: String, val tag: String)

    /**
     * function of get mail information
     * @param id[Int]
     * @return info[MailInformation]
     */
    fun getInformation(id: Int): MailInformation? {
        plugin.dataBase.open()
        val connection = plugin.dataBase.connection
        if (connection == null) {
            plugin.dataBase.sendErrorMessage()
            return null
        }
        val sql = "SELECT * FROM mail_list WHERE id='$id' LIMIT 1;"
        val statement = connection.createStatement()
        val result = statement.executeQuery(sql)
        result.next()
        val from = result.getString("from_player")
        val to = result.getString("to_player")
        val title = result.getString("title")
        val message = result.getString("message")
        val tag = result.getString("tag")

        result.close()
        statement.close()
        return MailInformation(id, from, to, title, message, tag)
    }

    private fun formatFromUser(from: String, senderType: MailSenderType): String {
        return when (senderType) {
            MailSenderType.PLAYER -> {
                from
            }
            MailSenderType.SERVER -> {
                "&SERVER"
            }
            MailSenderType.CUSTOM -> {
                "#${from}"
            }
        }
    }

}