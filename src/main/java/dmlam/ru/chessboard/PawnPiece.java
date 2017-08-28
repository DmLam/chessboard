package dmlam.ru.chessboard;

import android.graphics.Point;

/**
 * Created by Lam on 13.06.2015.
 */
public class PawnPiece extends Piece {

    public PawnPiece(ChessBoard chessBoard, Color color, int x, int y) {
        super(chessBoard, Kind.PAWN, color, x, y);
    }

    public PawnPiece(ChessBoard chessBoard, Color color, Point p) {
        super(chessBoard, Kind.PAWN, color, p);
    }

    @Override
    public char getPieceLetter() {
        return 'P';
    }

    @Override
    public boolean isMovePossible (int x, int y) {
        boolean result = true;
        Piece targetPiece = chessBoard.getPiece(x, y);

        if (x == this.x) {
            // проверяем обычный ход
            if (targetPiece != null) {
                // на клетке уже находится какая-то фигура
                result = false;
            } else {
                // если идем на две клетки, то можно идти только со второй (шестой) горизонтали на четвертую (пятую) и если на третьей (шестой) нет других фигур
                if (color == Color.WHITE) {
                    if (y != this.y + 1 && (this.y != 1 || y != 3 || chessBoard.getPiece(x, 2) != null)) {
                        result = false;
                    }
                } else { // BLACK
                    if (y != this.y - 1 && (this.y != 6 || y != 4 || chessBoard.getPiece(x, 5) != null)) {
                        result = false;
                    }
                }
            }
        } else { // проверяем взятие
            if (isSquareThreaten(x, y)) {
                if (targetPiece != null)
                    result = targetPiece.getColor() != color && targetPiece.getKind() != Kind.KING;
                else {
                    // взятие на проходе
                    Point ep = chessBoard.getEnPassantSquare();

                    // предыдущий ход был ходом пешки на две клетки и именно через ту клетку, которую проверяем на возможность хода туда
                    if (ep != null)
                        result = ep.x == x && ep.y == y;
                    else
                        result = false;
                }
            } else
                result = false;
        }

        return result;
    }

    @Override
    public void moveTo(int x, int y) {
        int oldY = this.y;
        boolean enPassant =(x != this.x && chessBoard.getPiece(x, y) == null);


        if (y - oldY == 2) {
            chessBoard.setEnPassantSquare(new Point(x, y - 1));
        }
        else
        if (y - oldY == -2) {
            chessBoard.setEnPassantSquare(new Point(x, y + 1));
        }

        super.moveTo(x, y);

        if (enPassant) {  // является ли этот ход взятием на проходе?
            switch (color) {
                case WHITE:
                    chessBoard.setPieceAt(x, y - 1, null);
                    lastMove.setPiece2(Kind.PAWN, new Point(x, y - 1), null);
                    break;
                case BLACK:
                    chessBoard.setPieceAt(x, y + 1, null);
                    lastMove.setPiece2(Kind.PAWN, new Point(x, y + 1), null);
                    break;
            }
        }
    }

    @Override
    // пешка может взять другую пешку на проходе, тогда стандартный (для Piece) метод тестового хода не сохранит взятую пешку
    void testMoveTo(int x, int y) {
        super.testMoveTo(x, y);

        // Был ли последний ход ходом пешки на две клетки?
        Point ep = chessBoard.getEnPassantSquare();

        if (ep != null) {
            if (ep.x == x && ep.y == y) {
                testMoveTakenPiece = chessBoard.getPiece(x, color == Color.WHITE ? 4 : 3);
                chessBoard.setPieceAt(testMoveTakenPiece.getX(), testMoveTakenPiece.getY(), null);
            }
        }
    }

    @Override
    protected int generateAvailableMoves (){
        int result = 0;
        int delta;

        result = super.generateAvailableMoves();

        if (color == Color.WHITE) {
            delta = 1;
        }
        else {
            delta = -1;
        }

        if (checkMoveAvailability(x, y + delta)) result++;
        if (checkMoveAvailability(x, y + delta * 2)) result++;

        // взятие вправо
        if (checkMoveAvailability(x + 1, y + delta)) result++;
        // взятие влево
        if (checkMoveAvailability(x - 1, y + delta)) result++;

        return result;
    }

    void promoteTo(Point sourceSquare, ChessBoard.PromoteTo promotion) {

        switch (promotion) {
            case QUEEN:
                chessBoard.setPieceAt(x, y, new QueenPiece(chessBoard, chessBoard.getMoveOrder(), x, y));
                break;
            case ROOK:
                chessBoard.setPieceAt(x, y, new RookPiece(chessBoard, chessBoard.getMoveOrder(), x, y));
                break;
            case BISHOP:
                chessBoard.setPieceAt(x, y, new BishopPiece(chessBoard, chessBoard.getMoveOrder(), x, y));
                break;
            case KNIGHT:
                chessBoard.setPieceAt(x, y, new KnightPiece(chessBoard, chessBoard.getMoveOrder(), x, y));
                break;
        }
        lastMove.setPromotePawnTo(promotion);
    }

    @Override
    public boolean isSquareThreaten(int x, int y) {
        boolean result = false;
        int delta = color == Color.WHITE ? 1 : -1;

        result = (y == this.y + delta) && (x == this.x + 1 || x == this.x - 1);

        return result;
    }

}
