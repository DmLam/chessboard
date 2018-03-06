package dmlam.ru.chessboard;

import android.graphics.Point;

/**
 * Created by Lam on 13.06.2015.
 */
public class KingPiece extends Piece {

    public KingPiece(ChessBoard chessBoard, Color color, int x, int y) {
        super(chessBoard, Kind.KING, color, x, y);
    }

    public KingPiece(ChessBoard chessBoard, Color color, Point p) {
        super(chessBoard, Kind.KING, color, p);
    }

    @Override
    public char getPieceLetter() {
        return 'K';
    }

    @Override
    public boolean isSquareThreaten (int x, int y) {
        int deltax = x - this.x, deltay = y - this.y;
        boolean result = Math.abs(deltax) <= 1 && Math.abs(deltay) <= 1;

        return result;
    }

    @Override
    public boolean isMovePossible(int x, int y) {
        boolean result;

        // проверяем рокировку
        if (y == this.y && Math.abs(x - this.x) == 2) {
            ChessBoard.Castling castling = x > this.x ? ChessBoard.Castling.KING : ChessBoard.Castling.QUEEN;

            result = chessBoard.isCastlingPossible(this.getColor(), castling) &&
                    // король не должен быть под шахом
                    !chessBoard.isPlayerChecked(this.getColor());
            if (result) {
                int delta = x > this.x ? 1 : -1;
                int rookx = delta == 1 ? 7 : 0;

                // поля между ладьей и королем должны быть пустые
                for (int i = getX() + delta; result && i != rookx; i += delta) {
                    result = chessBoard.getPiece(i, this.y) == null;
                }

                // король не должен находиться под шахом, проходить через битое поле или вставать на битое поле
                for (int i = getX(); result && i != x + delta; i += delta) {
                    result = !chessBoard.isSquareThreaten(i, this.y, getColor().opposite());
                }
            }
        }
        else {
            result = isSquareThreaten(x, y);
        }

        return result;
    }

    @Override
    public void moveTo(int x, int y) {
        int oldX = getX();

        super.moveTo(x, y);

        // если это рокировка - переместим ладью тоже
        if (Math.abs(x - oldX) == 2) {
            int rookX, newRookX;

            if (x > oldX) {
                rookX = 7;
                newRookX = 5;
            }
            else {
                rookX = 0;
                newRookX = 3;
            }
            Piece rook = chessBoard.getPiece(rookX, getY());

            chessBoard.setPieceAt(rookX, getY(), null);
            chessBoard.setPieceAt(newRookX, getY(), rook);

            lastMove.setPiece2(Kind.ROOK, new Point(rookX, getY()), new Point(newRookX, getY()));
        }
    }

    @Override
    protected int generateAvailableMoves() {
        int result = 0;

        super.generateAvailableMoves();

        if (checkMoveAvailability(x, y - 1)) result++;
        if (checkMoveAvailability(x + 1, y + 1)) result++;
        if (checkMoveAvailability(x + 1, y)) result++;
        if (checkMoveAvailability(x + 1, y - 1)) result++;
        if (checkMoveAvailability(x, y + 1)) result++;
        if (checkMoveAvailability(x - 1, y + 1)) result++;
        if (checkMoveAvailability(x - 1, y)) result++;
        if (checkMoveAvailability(x - 1, y - 1)) result++;

        return result;
    }
}
