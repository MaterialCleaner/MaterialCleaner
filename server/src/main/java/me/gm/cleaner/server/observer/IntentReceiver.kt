package me.gm.cleaner.server.observer

import android.content.IIntentReceiver
import android.content.IntentFilter
import me.gm.cleaner.client.CleanerHooksClient

class IntentReceiver : BaseIntentObserver(), ZygiskObserver {

    override fun registerReceiverInternal(
        receiver: IIntentReceiver, filter: IntentFilter, userId: Int, flags: Int
    ) {
        CleanerHooksClient.whileAlive { it.registerReceiver(receiver, filter, userId, flags) }
    }

    override fun unregisterReceiverInternal(receiver: IIntentReceiver) {
        CleanerHooksClient.whileAlive { it.unregisterReceiver(receiver) }
    }
}
