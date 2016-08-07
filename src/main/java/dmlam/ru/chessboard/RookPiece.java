package dmlam.ru.chessboard;

import android.graphics.Point;

/**
 * Created by Lam on 13.06.2015.
 */
public class RookPiece extends Piece {

    public RookPiece(ChessBoard chessBoard, Color color, int x, int y) {
        super(chessBoard, Kind.ROOK, color, x, y);
    }

    public RookPiece(ChessBoard chessBoard, Color color, Point p) {
        super(chessBoard, Kind.ROOK, color, p);
    }

    @Override
    public char getPieceLetter() {
        return 'R';
    }

    @Override
    public boolean isSquareThreaten (int x, int y) {
        int deltax = x - this.x, deltay = y - this.y;
        boolean result = deltax == 0 || deltay == 0;

        if (result) {
            // проверяем отсутствие фигур на пути
            deltax = deltax == 0 ? 0 : (deltax > 0 ? 1 : -1);
            deltay = deltay == 0 ? 0 : (deltay > 0 ? 1 : -1);

            for (int cx = this.x + deltax, cy = this.y + deltay; result && (Math.abs(cx - x) > 0 || Math.abs(cy - y) > 0) ; cx += deltax, cy += deltay) {
                result = chessBoard.getPiece(cx, cy) == null;
            }
        }

        return result;
    }

    @Override
    protected int generateAvailableMoves () {
        super.generateAvailableMoves();

        return generateLineMoves(1, 0) + generateLineMoves(0, 1);
    }

}
