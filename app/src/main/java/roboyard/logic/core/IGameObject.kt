package roboyard.eclabs

import roboyard.ui.components.GameManager
import roboyard.ui.components.RenderManager

/**
 * Created by Pierre on 21/01/2015.
 */
interface IGameObject {
    fun create()
    fun load(renderManager: RenderManager?)
    fun draw(renderManager: RenderManager?)
    fun update(gameManager: GameManager?)
    fun destroy()

    /**
     * Get the z-index of this game object
     * @return The z-index value (higher values are drawn on top)
     */
    /**
     * Set the z-index of this game object
     * @param zIndex The z-index value (higher values are drawn on top)
     */
    var zIndex: Int
}
