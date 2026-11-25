package com.example.chessmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessmate.ui.theme.ChessMateTheme

private val Background = Color(0xFF101713)
private val Panel = Color(0xFF1B2620)
private val Accent = Color(0xFF79C990)
private val LightSquare = Color(0xFFE7EBDD)
private val DarkSquare = Color(0xFF76966D)

private data class Piece(val kind: Char, val isWhite: Boolean) {
    val symbol: String
        get() = when (kind) {
            'K' -> if (isWhite) "♔" else "♚"
            'Q' -> if (isWhite) "♕" else "♛"
            'R' -> if (isWhite) "♖" else "♜"
            'B' -> if (isWhite) "♗" else "♝"
            'N' -> if (isWhite) "♘" else "♞"
            else -> if (isWhite) "♙" else "♟"
        }
}

private data class ChessMove(val from: Int, val to: Int, val piece: Piece, val captured: Piece?)

private fun startingBoard(): List<Piece?> {
    val board = MutableList<Piece?>(64) { null }
    val backRank = listOf('R', 'N', 'B', 'Q', 'K', 'B', 'N', 'R')
    for (column in 0..7) {
        board[column] = Piece(backRank[column], false)
        board[8 + column] = Piece('P', false)
        board[48 + column] = Piece('P', true)
        board[56 + column] = Piece(backRank[column], true)
    }
    return board
}

private fun isSquareAttacked(board: List<Piece?>, square: Int, byWhite: Boolean): Boolean {
    val targetRow = square / 8
    val targetColumn = square % 8
    for (from in board.indices) {
        val piece = board[from] ?: continue
        if (piece.isWhite != byWhite) continue
        val row = from / 8
        val column = from % 8
        val rowDelta = targetRow - row
        val columnDelta = targetColumn - column
        val attacks = when (piece.kind) {
            'P' -> rowDelta == (if (piece.isWhite) -1 else 1) && kotlin.math.abs(columnDelta) == 1
            'N' -> (kotlin.math.abs(rowDelta) == 2 && kotlin.math.abs(columnDelta) == 1) ||
                    (kotlin.math.abs(rowDelta) == 1 && kotlin.math.abs(columnDelta) == 2)
            'K' -> kotlin.math.abs(rowDelta) <= 1 && kotlin.math.abs(columnDelta) <= 1
            else -> {
                val diagonal = kotlin.math.abs(rowDelta) == kotlin.math.abs(columnDelta)
                val straight = rowDelta == 0 || columnDelta == 0
                val movesInLine = when (piece.kind) {
                    'B' -> diagonal
                    'R' -> straight
                    else -> diagonal || straight
                }
                if (!movesInLine || (rowDelta == 0 && columnDelta == 0)) false
                else {
                    val rowStep = rowDelta.compareTo(0)
                    val columnStep = columnDelta.compareTo(0)
                    var r = row + rowStep
                    var c = column + columnStep
                    var clear = true
                    while (r != targetRow || c != targetColumn) {
                        if (board[r * 8 + c] != null) clear = false
                        r += rowStep
                        c += columnStep
                    }
                    clear
                }
            }
        }
        if (attacks) return true
    }
    return false
}

private fun isLegalMove(board: List<Piece?>, from: Int, to: Int, whiteToMove: Boolean): Boolean {
    val piece = board[from] ?: return false
    val target = board[to]
    if (piece.isWhite != whiteToMove || target?.isWhite == piece.isWhite || from == to) return false

    val row = from / 8
    val column = from % 8
    val targetRow = to / 8
    val targetColumn = to % 8
    val dr = targetRow - row
    val dc = targetColumn - column

    val shapeIsValid = when (piece.kind) {
        'P' -> {
            val direction = if (piece.isWhite) -1 else 1
            val startRow = if (piece.isWhite) 6 else 1
            when {
                dc == 0 && dr == direction && target == null -> true
                dc == 0 && dr == direction * 2 && row == startRow && target == null &&
                        board[(row + direction) * 8 + column] == null -> true
                kotlin.math.abs(dc) == 1 && dr == direction && target != null -> true
                else -> false
            }
        }
        'N' -> (kotlin.math.abs(dr) == 2 && kotlin.math.abs(dc) == 1) ||
                (kotlin.math.abs(dr) == 1 && kotlin.math.abs(dc) == 2)
        'K' -> kotlin.math.abs(dr) <= 1 && kotlin.math.abs(dc) <= 1
        else -> {
            val diagonal = kotlin.math.abs(dr) == kotlin.math.abs(dc)
            val straight = dr == 0 || dc == 0
            val followsShape = when (piece.kind) {
                'B' -> diagonal
                'R' -> straight
                else -> diagonal || straight
            }
            if (!followsShape || (dr == 0 && dc == 0)) false
            else {
                val rowStep = dr.compareTo(0)
                val columnStep = dc.compareTo(0)
                var r = row + rowStep
                var c = column + columnStep
                var clear = true
                while (r != targetRow || c != targetColumn) {
                    if (board[r * 8 + c] != null) clear = false
                    r += rowStep
                    c += columnStep
                }
                clear
            }
        }
    }
    if (!shapeIsValid) return false

    val afterMove = board.toMutableList()
    afterMove[to] = if (piece.kind == 'P' && (targetRow == 0 || targetRow == 7)) Piece('Q', piece.isWhite) else piece
    afterMove[from] = null
    val king = afterMove.indexOfFirst { it?.kind == 'K' && it.isWhite == whiteToMove }
    return king >= 0 && !isSquareAttacked(afterMove, king, !whiteToMove)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ChessMateTheme { ChessGameScreen() } }
    }
}

@Composable
fun ChessGameScreen() {
    val board = remember { mutableStateListOf<Piece?>().apply { addAll(startingBoard()) } }
    val history = remember { mutableStateListOf<ChessMove>() }
    val selectedSquare = remember { mutableStateOf<Int?>(null) }
    val whiteToMove = remember { mutableStateOf(true) }
    val message = remember { mutableStateOf("White to move") }

    fun resetGame() {
        board.clear()
        board.addAll(startingBoard())
        history.clear()
        selectedSquare.value = null
        whiteToMove.value = true
        message.value = "White to move"
    }

    fun tapSquare(index: Int) {
        val selected = selectedSquare.value
        if (selected == null) {
            val piece = board[index]
            if (piece?.isWhite == whiteToMove.value) selectedSquare.value = index
            return
        }

        if (board[index]?.isWhite == whiteToMove.value) {
            selectedSquare.value = index
            return
        }
        if (isLegalMove(board, selected, index, whiteToMove.value)) {
            val piece = board[selected]!!
            val captured = board[index]
            history.add(ChessMove(selected, index, piece, captured))
            board[index] = if (piece.kind == 'P' && (index / 8 == 0 || index / 8 == 7)) Piece('Q', piece.isWhite) else piece
            board[selected] = null
            whiteToMove.value = !whiteToMove.value
            message.value = if (captured != null) "${if (piece.isWhite) "White" else "Black"} captured a piece" else if (whiteToMove.value) "White to move" else "Black to move"
            selectedSquare.value = null
        } else {
            message.value = "That move is not legal"
            selectedSquare.value = null
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("CHESSMATE", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("Casual game", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                }
                Text("♟", color = Accent, fontSize = 25.sp)
            }

            Spacer(Modifier.height(16.dp))
            PlayerCard(name = "Olivia Chen", label = "BLACK", time = "10:00", isWhite = false)
            Spacer(Modifier.height(10.dp))
            ChessBoard(board = board, selectedSquare = selectedSquare.value, whiteToMove = whiteToMove.value, onSquareTap = ::tapSquare)
            Spacer(Modifier.height(10.dp))
            PlayerCard(name = "You", label = "WHITE", time = "10:00", isWhite = true)
            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        if (history.isNotEmpty()) {
                            val move = history.removeAt(history.lastIndex)
                            board[move.from] = move.piece
                            board[move.to] = move.captured
                            whiteToMove.value = move.piece.isWhite
                            message.value = if (whiteToMove.value) "White to move" else "Black to move"
                            selectedSquare.value = null
                        }
                    },
                    enabled = history.isNotEmpty(),
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Panel, contentColor = Color.White, disabledContainerColor = Panel, disabledContentColor = Color.Gray)
                ) { Text("↶   Undo", fontWeight = FontWeight.SemiBold) }
                Button(
                    onClick = ::resetGame,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Background)
                ) { Text("＋   New Game", fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(10.dp))
            Text(message.value, color = Color(0xFFB5C1B8), fontSize = 13.sp)
        }
    }
}

@Composable
private fun PlayerCard(name: String, label: String, time: String, isWhite: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(38.dp).background(if (isWhite) Color(0xFFF1F1E9) else Color(0xFF35453B), CircleShape),
            contentAlignment = Alignment.Center
        ) { Text(if (isWhite) "♙" else "♟", fontSize = 22.sp, color = if (isWhite) Color(0xFF26352B) else Color.White) }
        Column(modifier = Modifier.weight(1f).padding(start = 11.dp)) {
            Text(name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(label, color = Color(0xFF9BA9A0), fontSize = 10.sp, letterSpacing = 1.5.sp)
        }
        Text(time, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ChessBoard(
    board: List<Piece?>,
    selectedSquare: Int?,
    whiteToMove: Boolean,
    onSquareTap: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF243229), RoundedCornerShape(12.dp)).padding(5.dp)) {
        for (row in 0..7) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (column in 0..7) {
                    val index = row * 8 + column
                    val piece = board[index]
                    val isLight = (row + column) % 2 == 0
                    val squareColor = when {
                        selectedSquare == index -> Color(0xFFE5D36F)
                        isLight -> LightSquare
                        else -> DarkSquare
                    }
                    val canMoveHere = selectedSquare?.let { isLegalMove(board, it, index, whiteToMove) } == true
                    Box(
                        modifier = Modifier.weight(1f).aspectRatio(1f).background(squareColor).clickable { onSquareTap(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (canMoveHere && piece == null) {
                            Box(Modifier.size(9.dp).background(Color(0x8879C990), CircleShape))
                        } else if (canMoveHere) {
                            Box(Modifier.fillMaxSize().padding(3.dp).background(Color.Transparent, RoundedCornerShape(4.dp)))
                        }
                        if (piece != null) {
                            Text(
                                text = piece.symbol,
                                color = if (piece.isWhite) Color(0xFFFDFDF8) else Color(0xFF1D2821),
                                fontSize = 34.sp,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChessGamePreview() {
    ChessMateTheme { ChessGameScreen() }
}
