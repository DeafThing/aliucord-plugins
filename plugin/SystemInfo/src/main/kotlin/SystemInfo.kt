import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.BOARD
import android.os.Build.BOOTLOADER
import android.os.Build.BRAND
import android.os.Build.DEVICE
import android.os.Build.DISPLAY
import android.os.Build.FINGERPRINT
import android.os.Build.HARDWARE
import android.os.Build.MANUFACTURER
import android.os.Build.MODEL
import android.os.Build.PRODUCT
import android.os.Build.SUPPORTED_ABIS
import android.os.Build.TAGS
import android.os.Build.TYPE
import android.os.Build.VERSION.CODENAME
import android.os.Build.VERSION.INCREMENTAL
import android.os.Build.VERSION.RELEASE
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION.SECURITY_PATCH
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.text.format.DateUtils
import android.text.format.Formatter
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.api.CommandsAPI.CommandResult
import com.aliucord.entities.MessageEmbedBuilder
import com.aliucord.entities.Plugin
import com.discord.api.commands.ApplicationCommandType
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

val Long.GB: String
    get() = String.format("%.2f", this / 1024.0 / 1024.0 / 1024.0)

val Long.MB: String
    get() = String.format("%.1f", this / 1024.0 / 1024.0)

fun Double.toFixed(digits: Int = 2): String = String.format("%.${digits}f", this)

val isRooted: Boolean
    get() = try {
        System.getenv("PATH")?.split(':')?.any {
            File(it, "su").exists()
        } ?: false || File("/system/bin/su").exists() || File("/system/xbin/su").exists()
    } catch (e: Exception) {
        false
    }

fun getArch(): String {
    SUPPORTED_ABIS.forEach {
        when (it) {
            "arm64-v8a" -> return "ARM64 (64-bit)"
            "armeabi-v7a" -> return "ARM (32-bit)"
            "x86_64" -> return "x86_64 (64-bit)"
            "x86" -> return "x86 (32-bit)"
        }
    }
    return System.getProperty("os.arch") ?: "Unknown"
}

fun getCpuInfo(): String {
    return try {
        BufferedReader(FileReader("/proc/cpuinfo")).use { reader ->
            var line: String?
            var processor: String? = null
            var hardware: String? = null
            
            while (reader.readLine().also { line = it } != null) {
                when {
                    line!!.startsWith("model name") -> 
                        return line!!.split(":")[1].trim()
                    line!!.startsWith("Processor") -> 
                        processor = line!!.split(":")[1].trim()
                    line!!.startsWith("Hardware") -> 
                        hardware = line!!.split(":")[1].trim()
                }
            }
            
            processor ?: hardware ?: HARDWARE
        }
    } catch (e: Exception) {
        HARDWARE
    }
}

fun getStorageInfo(): Pair<String, String> {
    return try {
        val internalPath = Environment.getDataDirectory()
        val internalStat = StatFs(internalPath.path)
        val internalTotal = internalStat.blockCountLong * internalStat.blockSizeLong
        val internalFree = internalStat.availableBlocksLong * internalStat.blockSizeLong
        val internalUsed = internalTotal - internalFree
        val internalPercent = ((internalUsed.toDouble() / internalTotal) * 100).toFixed(1)
        
        val internal = "${internalUsed.GB}GB / ${internalTotal.GB}GB ($internalPercent% used)"
        
        // Try to get external storage info
        val external = try {
            if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                val externalPath = Environment.getExternalStorageDirectory()
                val externalStat = StatFs(externalPath.path)
                val externalTotal = externalStat.blockCountLong * externalStat.blockSizeLong
                val externalFree = externalStat.availableBlocksLong * externalStat.blockSizeLong
                val externalUsed = externalTotal - externalFree
                val externalPercent = ((externalUsed.toDouble() / externalTotal) * 100).toFixed(1)
                
                // Only show if different from internal
                if (externalTotal != internalTotal) {
                    "${externalUsed.GB}GB / ${externalTotal.GB}GB ($externalPercent% used)"
                } else {
                    "Same as internal"
                }
            } else {
                "Not available"
            }
        } catch (e: Exception) {
            "Not available"
        }
        
        Pair(internal, external)
    } catch (e: Exception) {
        Pair("Unknown", "Unknown")
    }
}

fun getKernelVersion(): String {
    return try {
        BufferedReader(FileReader("/proc/version")).use { reader ->
            val version = reader.readLine()
            when {
                version.contains("Linux version") -> {
                    val parts = version.split(" ")
                    if (parts.size >= 3) {
                        "Linux ${parts[2]}"
                    } else {
                        System.getProperty("os.version") ?: "Unknown"
                    }
                }
                else -> System.getProperty("os.version") ?: "Unknown"
            }
        }
    } catch (e: Exception) {
        System.getProperty("os.version") ?: "Unknown"
    }
}

fun getAndroidSecurityPatchLevel(): String {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            SECURITY_PATCH
        } else {
            "Not available"
        }
    } catch (e: Exception) {
        "Unknown"
    }
}

fun formatUptime(uptimeMillis: Long): String {
    val seconds = uptimeMillis / 1000
    val days = seconds / 86400
    val hours = (seconds % 86400) / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    
    return when {
        days > 0 -> "${days}d ${hours}h ${minutes}m"
        hours > 0 -> "${hours}h ${minutes}m ${secs}s"
        minutes > 0 -> "${minutes}m ${secs}s"
        else -> "${secs}s"
    }
}

@AliucordPlugin
class SystemInfo : Plugin() {
    override fun start(context: Context) {
        commands.registerCommand(
            "system-info",
            "Get detailed system information",
            listOf(
                Utils.createCommandOption(
                    ApplicationCommandType.BOOLEAN,
                    "send",
                    "Send result visible to everyone"
                ),
                Utils.createCommandOption(
                    ApplicationCommandType.BOOLEAN,
                    "detailed",
                    "Show additional technical details"
                )
            )
        ) { ctx ->
            val memInfo = ActivityManager.MemoryInfo()
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.getMemoryInfo(memInfo)

            val totalMem = memInfo.totalMem
            val availMem = memInfo.availMem
            val usedMem = totalMem - availMem
            val memoryPercent = ((usedMem.toDouble() / totalMem) * 100).toFixed(1)
            
            val (internalStorage, externalStorage) = getStorageInfo()
            val isDetailed = ctx.getBoolOrDefault("detailed", false)
            
            // Basic system info
            val basicInfo = linkedMapOf(
                "Device" to "$MANUFACTURER $MODEL",
                "Android Version" to "$RELEASE (API $SDK_INT)",
                "Architecture" to getArch(),
                "Root Status" to if (isRooted) "Rooted" else "Not Rooted",
                "Memory Usage" to "${usedMem.GB}GB / ${totalMem.GB}GB (${memoryPercent}% used)",
                "Internal Storage" to internalStorage,
                "Security Patch" to getAndroidSecurityPatchLevel(),
                "Uptime" to formatUptime(SystemClock.elapsedRealtime())
            )
            
            // Detailed technical info
            val detailedInfo = if (isDetailed) linkedMapOf(
                "Kernel Version" to getKernelVersion(),
                "CPU/Hardware" to getCpuInfo(),
                "Bootloader" to BOOTLOADER,
                "Build Type" to "$TYPE ($TAGS)",
                "External Storage" to externalStorage,
                "Hardware Platform" to HARDWARE,
                "Supported ABIs" to SUPPORTED_ABIS.take(3).joinToString(", ") + if (SUPPORTED_ABIS.size > 3) ", ..." else "",
                "Build Fingerprint" to FINGERPRINT.split("/").let { parts ->
                    if (parts.size >= 3) "${parts[0]}/${parts[1]}/${parts[2]}/..." else FINGERPRINT.take(40) + "..."
                }
            ) else emptyMap()
            
            val allInfo = basicInfo + detailedInfo
            
            if (ctx.getBoolOrDefault("send", false)) {
                // Public message format - clean and concise
                StringBuilder().run {
                    append("**System Information**\n")
                    append("```\n")
                    
                    basicInfo.forEach { (key, value) ->
                        append("${key.padEnd(18)}: $value\n")
                    }
                    
                    if (isDetailed) {
                        append("\n--- Additional Details ---\n")
                        detailedInfo.forEach { (key, value) ->
                            append("${key.padEnd(18)}: $value\n")
                        }
                    }
                    
                    append("```")
                    CommandResult(toString(), null, true)
                }
            } else {
                // Embed format - rich and colorful
                MessageEmbedBuilder().run {
                    setColor(0x2ECC71) // Green color
                    setTitle("System Information")
                    setDescription("Detailed system specifications and status")
                    
                    // Add basic info fields
                    basicInfo.forEach { (key, value) ->
                        addField(key, "`$value`", true)
                    }
                    
                    if (isDetailed) {
                        // Add separator
                        addField("", "━━━━━━━━━━━━━━━━━━━━", false)
                        
                        // Add detailed info fields
                        detailedInfo.forEach { (key, value) ->
                            addField(key, "`$value`", true)
                        }
                    }
                    
                    // Add footer with timestamp
                    setFooter("Generated on ${SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault()).format(Date())}")
                    
                    CommandResult(null, listOf(build()), false)
                }
            }
        }
    }

    override fun stop(context: Context) = commands.unregisterAll()
}