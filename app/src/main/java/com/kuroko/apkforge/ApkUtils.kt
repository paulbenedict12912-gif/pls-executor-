package com.kuroko.apkforge

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Environment
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

object ApkUtils {

    fun outputDir(): File {
        val dir = File(Environment.getExternalStorageDirectory(), "ApkForge")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun listInstalledApps(ctx: Context, includeSystem: Boolean = false): List<AppInfo> {
        val pm = ctx.packageManager
        val flags = PackageManager.GET_META_DATA
        val apps = pm.getInstalledApplications(flags)
        val out = ArrayList<AppInfo>()
        for (ai in apps) {
            val isSys = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (!includeSystem && isSys) continue
            val src = ai.sourceDir ?: ai.publicSourceDir ?: continue
            val file = File(src)
            out.add(
                AppInfo(
                    appName = pm.getApplicationLabel(ai).toString(),
                    packageName = ai.packageName,
                    apkPath = src,
                    icon = try { pm.getApplicationIcon(ai) } catch (_: Exception) { null },
                    isSystem = isSys,
                    sizeBytes = file.length()
                )
            )
        }
        out.sortBy { it.appName.lowercase() }
        return out
    }

    fun copyApk(app: AppInfo, customName: String? = null): File? {
        val src = File(app.apkPath)
        if (!src.exists()) return null
        val safeName = customName?.takeIf { it.isNotBlank() } ?: app.appName
        val outFile = File(outputDir(), "${sanitize(safeName)}_${System.currentTimeMillis()}.apk")
        src.inputStream().use { input ->
            outFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return outFile
    }

    fun listEntries(apk: File): List<ZipEntry> {
        val list = ArrayList<ZipEntry>()
        ZipFile(apk).use { zip ->
            val e = zip.entries()
            while (e.hasMoreElements()) list.add(e.nextElement())
        }
        list.sortBy { it.name }
        return list
    }

    fun extractEntry(apk: File, entryName: String, outDir: File): File? {
        ZipFile(apk).use { zip ->
            val entry = zip.getEntry(entryName) ?: return null
            if (entry.isDirectory) return null
            val out = File(outDir, entryName.replace("/", "_"))
            zip.getInputStream(entry).use { input ->
                out.outputStream().use { output -> input.copyTo(output) }
            }
            return out
        }
    }

    fun readEntryBytes(apk: File, entryName: String, maxBytes: Int = 2_000_000): ByteArray? {
        ZipFile(apk).use { zip ->
            val entry = zip.getEntry(entryName) ?: return null
            if (entry.size > maxBytes) return null
            return zip.getInputStream(entry).use { it.readBytes() }
        }
    }

    fun isTextLike(name: String): Boolean {
        val lower = name.lowercase()
        val ext = lower.substringAfterLast('.', "")
        return ext in setOf(
            "txt", "xml", "json", "properties", "md", "html", "htm",
            "js", "css", "yml", "yaml", "csv", "smali", "kotlin", "java",
            "kt", "gradle", "pro", "cfg", "ini"
        )
    }

    private fun sanitize(s: String): String =
        s.replace(Regex("[^A-Za-z0-9._-]"), "_").take(60)
}
