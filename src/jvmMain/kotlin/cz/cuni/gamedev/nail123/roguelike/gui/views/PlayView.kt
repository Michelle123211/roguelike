package cz.cuni.gamedev.nail123.roguelike.gui.views

import cz.cuni.gamedev.nail123.roguelike.Game
import cz.cuni.gamedev.nail123.roguelike.blocks.GameBlock
import cz.cuni.gamedev.nail123.roguelike.GameConfig
import cz.cuni.gamedev.nail123.roguelike.controls.KeyboardControls
import cz.cuni.gamedev.nail123.roguelike.events.EntityMoved
import cz.cuni.gamedev.nail123.roguelike.events.GameEvent
import cz.cuni.gamedev.nail123.roguelike.gui.CameraMover
import org.hexworks.cobalt.events.api.CallbackResult
import org.hexworks.cobalt.events.api.KeepSubscription
import org.hexworks.zircon.api.ColorThemes
import org.hexworks.zircon.api.ComponentDecorations
import org.hexworks.zircon.api.Components
import org.hexworks.zircon.api.GameComponents
import org.hexworks.zircon.api.component.ComponentAlignment
import org.hexworks.zircon.api.data.Tile
import org.hexworks.zircon.api.grid.TileGrid
import org.hexworks.zircon.api.uievent.KeyboardEventType
import org.hexworks.zircon.api.view.base.BaseView

class PlayView(val tileGrid: TileGrid, val game: Game = Game.create()): BaseView(tileGrid, ColorThemes.arc()) {
    override fun onDock() {
        val sidebar = Components.panel()
                .withSize(GameConfig.SIDEBAR_WIDTH, GameConfig.WINDOW_HEIGHT)
                .withDecorations(
                        ComponentDecorations.box()
                )
                .build()

        val logArea = Components.logArea()
                .withSize(GameConfig.WINDOW_WIDTH - GameConfig.SIDEBAR_WIDTH, GameConfig.LOG_AREA_HEIGHT)
                .withAlignmentWithin(screen, ComponentAlignment.BOTTOM_RIGHT)
                .withDecorations(
                        ComponentDecorations.box(title = "Log")
                )
                .build()

        val gameComponent = GameComponents.newGameComponentBuilder<Tile, GameBlock>()
                .withGameArea(game.world)
                .withAlignmentWithin(screen, ComponentAlignment.TOP_RIGHT)
                .build()

        screen.addComponent(sidebar)
        screen.addComponent(logArea)
        screen.addComponent(gameComponent)

        val keyboardControls = KeyboardControls(game)
        screen.handleKeyboardEvents(KeyboardEventType.KEY_PRESSED) { event, phase ->
            keyboardControls.handleInput(event)
        }

        val cameraMover = CameraMover(game.world)

        GameEvent.subscribe<EntityMoved.PlayerMoved> {
            cameraMover.update()
            KeepSubscription
        }
    }
}