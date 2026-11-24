package com.omegaapi.huesort

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {

    private fun solvedState(colorCount: Int = 3, capacity: Int = 4): GameState {
        val tubes = (0 until colorCount).map { color -> List(capacity) { color } } +
            listOf(emptyList<Int>(), emptyList<Int>())
        return GameState(
            level = 1,
            tubes = tubes,
            capacity = capacity,
            colorCount = colorCount
        )
    }

    @Test
    fun generatedLevelsHaveExpectedShapeAndAreNotSolved() {
        for (level in 1..12) {
            val state = GameEngine.generateLevel(level)
            assertEquals(level, state.level)
            assertEquals(state.colorCount + 2, state.tubes.size)
            assertFalse("Level $level should start scrambled", GameEngine.isSolved(state))
            assertEquals(state.colorCount, GameEngine.colorCounts(state).size)
            assertTrue(
                "Every colour must appear exactly once per tube capacity",
                GameEngine.colorCounts(state).values.all { it == state.capacity }
            )
        }
    }

    @Test
    fun generationIsDeterministicForTheSameSeed() {
        val first = GameEngine.generateLevel(4, 4242L)
        val second = GameEngine.generateLevel(4, 4242L)
        assertEquals(GameEngine.signature(first), GameEngine.signature(second))
    }

    @Test
    fun illegalPourLeavesStateUnchanged() {
        val state = GameState(
            level = 1,
            tubes = listOf(listOf(0, 0), listOf(1, 1), emptyList()),
            capacity = 3,
            colorCount = 2
        )
        val result = GameEngine.pour(state, 0, 1)
        assertFalse(result.valid)
        assertEquals(0, result.moved)
        assertEquals(GameEngine.signature(state), GameEngine.signature(result.state))
        assertEquals(0, result.state.moves)
    }

    @Test
    fun legalPourMovesTopRunAndIncrementsMoves() {
        val state = GameState(
            level = 1,
            tubes = listOf(listOf(0, 1), listOf(1), emptyList()),
            capacity = 3,
            colorCount = 2
        )
        val result = GameEngine.pour(state, 0, 1)
        assertTrue(result.valid)
        assertEquals(1, result.moved)
        assertEquals(listOf(0), result.state.tubes[0])
        assertEquals(listOf(1, 1), result.state.tubes[1])
        assertEquals(1, result.state.moves)
    }

    @Test
    fun solvedBoardIsDetected() {
        val state = solvedState()
        assertTrue(GameEngine.isSolved(state))
        assertTrue(GameEngine.legalMoves(state).isNotEmpty())
    }

    @Test
    fun hintReturnsALegalMove() {
        val state = GameEngine.generateLevel(2)
        val move = GameEngine.hint(state)
        assertNotNull(move)
        assertTrue(GameEngine.legalMoves(state).contains(move))
    }

    @Test
    fun shuffleKeepsColourCountsAndDoesNotSolveTheBoard() {
        val state = GameEngine.generateLevel(3)
        val before = GameEngine.colorCounts(state)
        val shuffled = GameEngine.shuffle(state, 987L)
        assertEquals(before, GameEngine.colorCounts(shuffled))
        assertEquals(state.moves, shuffled.moves)
        assertFalse(GameEngine.isSolved(shuffled))
    }

    @Test
    fun extraTubesAreAddedAndCapped() {
        var state = GameEngine.generateLevel(1)
        val initialSize = state.tubes.size
        repeat(GameEngine.MAX_BONUS_TUBES + 2) {
            state = GameEngine.addTube(state)
        }
        assertEquals(
            "bonus=${state.bonusTubes}, initial=$initialSize",
            initialSize + GameEngine.MAX_BONUS_TUBES,
            state.tubes.size
        )
        assertEquals(GameEngine.MAX_BONUS_TUBES, state.bonusTubes)
        assertTrue(state.tubes.last().isEmpty())
    }
}
