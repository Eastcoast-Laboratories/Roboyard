/*  DriftingDroids - yet another Ricochet Robots solver program.
    Copyright (C) 2011-2025 Michael Henke

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package driftingdroids.model

import kotlin.math.max

/**
 * Factory that creates instances of KeyDepthMap.
 */
object KeyDepthMapFactory {
    private var defaultClazz: Class<out KeyDepthMap?>? = KeyDepthMapTrieSpecial::class.java


    /**
     * Set this factory's default implementation class of KeyDepthMap.
     * 
     * @param clazz the implementation class of KeyDepthMap
     */
    fun setDefaultClass(clazz: Class<out KeyDepthMap?>?) {
        defaultClazz = clazz
    }


    /**
     * Creates a new instance of KeyDepthMap.
     * 
     * @param board the board that is to be solved
     * @param clazz the implementation class of KeyDepthMap
     * @return a new instance of KeyDepthMap
     */
    /**
     * Creates a new instance of KeyDepthMap.
     * Uses this factory's default implementation class of KeyDepthMap.
     * 
     * @param board the board that is to be solved
     * @return
     */
    @JvmOverloads
    fun newInstance(board: Board, clazz: Class<out KeyDepthMap?>? = defaultClazz): KeyDepthMap {
        if (KeyDepthMapTrieGeneric::class.java == clazz) {
            return KeyDepthMapTrieGeneric(max(12, board.numRobots * board.sizeNumBits))
        } else if (KeyDepthMapTrieSpecial::class.java == clazz) {
            return KeyDepthMapTrieSpecial.Companion.createInstance(board, true)
        } else {
            throw IllegalArgumentException("unknown KeyDepthMap class: " + clazz)
        }
    }
}
