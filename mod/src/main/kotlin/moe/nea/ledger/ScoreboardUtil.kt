package moe.nea.ledger

import net.minecraft.client.Minecraft
import net.minecraft.scoreboard.ScorePlayerTeam

object ScoreboardUtil {

    val sidebarSlot = 1
    fun getScoreboardStrings(): List<String> {
        val scoreboard = Minecraft.getMinecraft().theWorld.scoreboard
        val objective = scoreboard.getObjectiveInDisplaySlot(sidebarSlot)
        val scoreList = scoreboard.getSortedScores(objective).take(15)
            .map {
                ScorePlayerTeam.formatPlayerName(scoreboard.getPlayersTeam(it.playerName), it.playerName)
            }
            .map { stripAlien(it) }
            .reversed()
        return scoreList
    }

    fun stripAlien(string: String): String {
        val sb = StringBuilder()
        for (c in string) {
            if (Minecraft.getMinecraft().fontRendererObj.getCharWidth(c) > 0 || c == '§')
                sb.append(c)
        }
        return sb.toString()
    }
}