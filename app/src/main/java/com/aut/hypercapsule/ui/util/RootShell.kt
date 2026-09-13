package com.aut.hypercapsule.ui.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataOutputStream

/** Root shell helpers used by manager-app actions that must touch SystemUI. */
object RootShell {
    suspend fun run(commands: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val process = Runtime.getRuntime().exec("su")
            DataOutputStream(process.outputStream).use { writer ->
                commands.forEach { command -> writer.writeBytes("$command\n") }
                writer.writeBytes("exit\n")
                writer.flush()
            }
            val exitCode = process.waitFor()
            check(exitCode == 0) { "Root permission denied (exit $exitCode)" }
        }
    }

    suspend fun run(command: String): Result<Unit> = run(listOf(command))
}
