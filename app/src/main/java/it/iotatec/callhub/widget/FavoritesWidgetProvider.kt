package it.iotatec.callhub.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import it.iotatec.callhub.R
import it.iotatec.callhub.data.repo.FavoritesRepository
import it.iotatec.callhub.ui.MainActivity
import it.iotatec.callhub.util.CallPlacer

/** Home-screen widget: quick-dial buttons for the first favorite numbers. */
class FavoritesWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val favorites = FavoritesRepository.favorites(context).toList()
        val buttons = listOf(R.id.widget_btn_0, R.id.widget_btn_1, R.id.widget_btn_2, R.id.widget_btn_3)

        ids.forEach { widgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_favorites)
            views.setTextViewText(R.id.widget_title, context.getString(R.string.favorites_title))
            views.setOnClickPendingIntent(
                R.id.widget_title,
                PendingIntent.getActivity(
                    context, 0, Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )

            buttons.forEachIndexed { i, btnId ->
                if (i < favorites.size) {
                    val number = favorites[i]
                    views.setTextViewText(btnId, number)
                    views.setViewVisibility(btnId, View.VISIBLE)
                    val callIntent = Intent(context, FavoritesWidgetProvider::class.java).apply {
                        action = ACTION_CALL
                        putExtra(EXTRA_NUMBER, number)
                    }
                    views.setOnClickPendingIntent(
                        btnId,
                        PendingIntent.getBroadcast(
                            context, number.hashCode(), callIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                } else {
                    views.setViewVisibility(btnId, View.GONE)
                }
            }
            manager.updateAppWidget(widgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_CALL) {
            intent.getStringExtra(EXTRA_NUMBER)?.takeIf { it.isNotBlank() }?.let {
                CallPlacer.place(context, it)
            }
        }
    }

    companion object {
        const val ACTION_CALL = "it.iotatec.callhub.WIDGET_CALL"
        const val EXTRA_NUMBER = "number"

        /** Refresh all placed widgets (call after favorites change). */
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, FavoritesWidgetProvider::class.java))
            if (ids.isNotEmpty()) FavoritesWidgetProvider().onUpdate(context, manager, ids)
        }
    }
}
