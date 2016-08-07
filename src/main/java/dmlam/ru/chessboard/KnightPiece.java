package dmlam.ru.chessboard;

import android.graphics.Point;

/**
 * Created by Lam on 13.06.2015.
 */
public class KnightPiece extends Piece {

    public KnightPiece(ChessBoard chessBoard, Color color, int x, int y) {
        super(chessBoard, Kind.KNIGHT, color, x, y);
    }

    public KnightPiece(ChessBoard chessBoard, Color color, Point p) {
        super(chessBoard, Kind.KNIGHT, color, p);
    }

    @Override
    public char getPieceLetter() {
        return 'N';
    }

    @Override
    public boolean isSquareThreaten (int x, int y) {
        return ((Math.abs(this.x - x) == 1 && Math.abs(this.y - y) == 2) || Math.abs(this.x - x) == 2 && Math.abs(this.y - y) == 1);
    }

    @Override
    protected int generateAvailableMoves() {
        int result = 0;

        super.generateAvailableMoves();

        if (checkMoveAvailability(x + 1, y + 2)) result++;
        if (checkMoveAvailability(x + 1, y - 2)) result++;
        if (checkMoveAvailability(x + 2, y + 1)) result++;
        if (checkMoveAvailability(x + 2, y - 1)) result++;
        if (checkMoveAvailability(x - 1, y + 2)) result++;
        if (checkMoveAvailability(x - 1, y - 2)) result++;
        if (checkMoveAvailability(x - 2, y + 1)) result++;
        if (checkMoveAvailability(x - 2, y - 1)) result++;

        return result;
    }

}
