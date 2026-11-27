package com.example.superclicker.util

import android.content.Context
import com.example.superclicker.model.Rule
import com.google.gson.Gson
import java.io.File

object RuleStore {
    private const val RULE_FILE = "current_rule.json"
    private const val BACKUP_FILE = "rule_backup.json"
    private val gson = Gson()

    fun save(context: Context, rule: Rule): Boolean = try {
        val file = File(context.filesDir, RULE_FILE)
        file.writeText(gson.toJson(rule), Charsets.UTF_8)
        true
    } catch (e: Exception) {
        false
    }

    fun load(context: Context): Rule? = try {
        val file = File(context.filesDir, RULE_FILE)
        if (file.exists()) gson.fromJson(file.readText(Charsets.UTF_8), Rule::class.java) else null
    } catch (e: Exception) {
        null
    }

    fun saveBackup(context: Context, rule: Rule): Boolean = try {
        File(context.filesDir, BACKUP_FILE).writeText(gson.toJson(rule), Charsets.UTF_8)
        true
    } catch (e: Exception) {
        false
    }

    fun toJson(rule: Rule): String = gson.toJson(rule)

    fun fromJson(json: String): Rule? = try {
        gson.fromJson(json, Rule::class.java)
    } catch (e: Exception) {
        null
    }
}
