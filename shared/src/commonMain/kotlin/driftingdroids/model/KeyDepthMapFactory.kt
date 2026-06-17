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
    private var defaultImpl: String = "KeyDepthMapTrieSpecial"


    /**
     * Set this factory's default implementation class of KeyDepthMap.
     * 
     * @param impl the implementation name of KeyDepthMap
     */
    fun setDefaultImpl(impl: String) {
        defaultImpl = impl
    }


    /**
     * Creates a new instance of KeyDepthMap.
     * 
     * @param board the board that is to be solved
     * @param impl the implementation name of KeyDepthMap
     * @return a new instance of KeyDepthMap
     */
    /**
     * Creates a new instance of KeyDepthMap.
     * Uses this factory's default implementation class of KeyDepthMap.
     * 
     * @param board the board that is to be solved
     * @return
     */
    fun newInstance(board: Board, impl: String = defaultImpl): KeyDepthMap {
        if ("KeyDepthMapTrieGeneric" == impl) {
            return KeyDepthMapTrieGeneric(max(12, board.numRobots * board.sizeNumBits))
        } else if ("KeyDepthMapTrieSpecial" == impl) {
            return KeyDepthMapTrieSpecial.Companion.createInstance(board, true)
        } else {
            throw IllegalArgumentException("unknown KeyDepthMap implementation: " + impl)
        }
    }
}
