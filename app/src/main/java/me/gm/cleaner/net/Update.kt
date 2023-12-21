package me.gm.cleaner.net

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.gm.cleaner.BuildConfig
import me.gm.cleaner.R
import me.gm.cleaner.dao.RootPreferences
import me.gm.cleaner.util.PermissionUtils.notifySafe
import org.json.JSONObject
import java.net.URL
import java.util.Locale
import kotlin.math.pow

data class UpdateInfo(
    val versionName: String,
    val changeLog: String,
)

/*
int strverscmp(const char *l0, const char *r0)
{
	const unsigned char *l = (const void *)l0;
	const unsigned char *r = (const void *)r0;
	size_t i, dp, j;
	int z = 1;

	/* Find maximal matching prefix and track its maximal digit
	 * suffix and whether those digits are all zeros. */
	for (dp=i=0; l[i]==r[i]; i++) {
		int c = l[i];
		if (!c) return 0;
		if (!isdigit(c)) dp=i+1, z=1;
		else if (c!='0') z=0;
	}

	if (l[dp]!='0' && r[dp]!='0') {
		/* If we're not looking at a digit sequence that began
		 * with a zero, longest digit string is greater. */
		for (j=i; isdigit(l[j]); j++)
			if (!isdigit(r[j])) return 1;
		if (isdigit(r[j])) return -1;
	} else if (z && dp<i && (isdigit(l[i]) || isdigit(r[i]))) {
		/* Otherwise, if common prefix of digit sequence is
		 * all zeros, digits order less than non-digits. */
		return (unsigned char)(l[i]-'0') - (unsigned char)(r[i]-'0');
	}

	return l[i] - r[i];
}
*/

private fun String.toFloatVersionName(): Float {
    var floatVersionName = 0F
    val split = filter { it.isDigit() || it == '.' }.split('.')
    for (i in split.indices) {
        floatVersionName += 100F.pow(-i) * split[i].toFloat()
    }
    return floatVersionName
}

suspend fun getUpdateInfo(): UpdateInfo? {
    val result = withContext(Dispatchers.IO) {
        runCatching {
            JSONObject(URL(Website.latestReleasesApi).readText())
        }
    }
    val json = result.getOrDefault(JSONObject())
    if (json.optBoolean("draft") || json.optBoolean("prerelease")) {
        return null
    }
    val name = json.optString("name")
    if (name.isEmpty()) {
        return null
    }
    val hasNewVersion = name.toFloatVersionName() > BuildConfig.VERSION_NAME.toFloatVersionName()
    return if (hasNewVersion) {
        val body = JSONObject(json.optString("body"))
        val changeLog = LocaleAdapter(
            Locale.US to {
                body.optString(Locale.US.toLanguageTag())
            },
            Locale.SIMPLIFIED_CHINESE to {
                body.optString(Locale.SIMPLIFIED_CHINESE.toLanguageTag())
            }
        ).getContentForLocale(RootPreferences.locale)
        UpdateInfo(name, changeLog)
    } else {
        null
    }
}

const val NOTIFICATION_CHANNEL: String = "update"

suspend fun maybeBuildUpdateNotification(context: Context) {
    val info = getUpdateInfo() ?: return
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Website.latestReleases.toUri()
    }
    val contentIntent = PendingIntent.getActivity(
        context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val style = NotificationCompat.BigTextStyle().bigText(info.changeLog)
    val notification = NotificationCompat
        .Builder(context, NOTIFICATION_CHANNEL)
        .setContentTitle(context.getString(R.string.new_version, info.versionName))
        .setContentText(info.changeLog)
        .setStyle(style)
        .setContentIntent(contentIntent)
        .setAutoCancel(true)
        .setSmallIcon(R.drawable.ic_outline_update_24)
        .setColor(context.getColor(R.color.color_primary))
        .build()
    NotificationManagerCompat.from(context).run {
        val channel = NotificationChannelCompat
            .Builder(NOTIFICATION_CHANNEL, NotificationManager.IMPORTANCE_MAX)
            .setName(context.getString(R.string.update))
            .setSound(null, null)
            .build()
        createNotificationChannel(channel)
        notifySafe(context, NOTIFICATION_CHANNEL.hashCode(), notification)
    }
}
