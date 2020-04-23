package cz.cuni.gamedev.nail123.roguelike.events

import org.hexworks.cobalt.events.api.CallbackResult
import org.hexworks.cobalt.events.api.Event
import org.hexworks.zircon.internal.Zircon

/**
 * This class simplifies Zircon's event handling to always use default scope and eventBus.
 * It provides a simple emit / subscribe API.
 */
abstract class GameEvent : Event {
    open fun emit() {
        Zircon.eventBus.publish(this)
    }

    companion object {
        fun subscribe(key: String, fn: (GameEvent) -> CallbackResult) {
            Zircon.eventBus.subscribeTo(key=key, fn=fn)
        }
    }
}