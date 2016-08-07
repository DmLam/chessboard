package dmlam.ru.chessboard;

import android.graphics.Point;

/**
 * Created by Lam on 13.06.2015.
 */
public class BishopPiece extends Piece {

    public BishopPiece(ChessBoard chessBoard, Color color, int x, int y) {
        super(chessBoard, Kind.BISHOP, color, x, y);
    }

    public BishopPiece(ChessBoard chessBoard, Color color, Point p) {
        super(chessBoard, Kind.BISHOP, color, p);
    }

    @Override
    public char getPieceLetter() {
        return 'B';
    }

    @Override
    public boolean isSquareThreaten (int x, int y) {
        int deltax = x - this.x, deltay = y - this.y;
        boolean result = Math.abs(deltax) == Math.abs(deltay);

        if (result) {
            // проверяем отсутствие фигур на пути
            deltax = deltax > 0 ? 1 : -1;
            deltay = deltay > 0 ? 1 : -1;

            for (int cx = this.x + deltax, cy = this.y + deltay; result && (Math.abs(cx - x) > 0 || Math.abs(cy - y) > 0) ; cx += deltax, cy += deltay) {
                result = chessBoard.getPiece(cx, cy) == null;
            }
        }

        return result;
    }

    @Override
    protected int generateAvailableMoves () {
        super.generateAvailableMoves();

        return generateLineMoves(1, 1) + generateLineMoves(1, -1);
    }

}
