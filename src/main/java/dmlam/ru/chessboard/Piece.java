package dmlam.ru.chessboard;

import android.graphics.Point;

import java.util.ArrayList;

/**
 * Created by Lam on 12.06.2015.
 */
public abstract class Piece {
    public enum Color {
        WHITE, BLACK;

        public Color opposite() { return this == WHITE ? BLACK : WHITE; }
        public char getFENColorLetter() { return this == WHITE ? 'w' : 'b'; }
    };
    public enum Kind {KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN;

    public String getNotationLetter() {
        switch(this) {
            case KING:
                return "K";
            case QUEEN:
                return "Q";
            case ROOK:
                return "R";
            case BISHOP:
                return "B";
            case KNIGHT:
                return "N";
            default:
                return "";
            }
        }
    };

    protected ChessBoard chessBoard;
    protected Color color;
    protected Kind kind;
    protected int x, y;
    protected Move lastMove = null;  // последний ход фигуры. Должен быть установлен в методе moveTo. Используется для корректного заполнения данных о ходе в партии
    protected Point testMoveSource = new Point(), testMoveTarget = new Point();
    protected Piece testMoveTakenPiece;
    protected ArrayList<Point> availableMoves = new ArrayList<Point>();

    public Piece(ChessBoard chessBoard, Kind kind, Color color, int x, int y) {
        this.chessBoard = chessBoard;
        this.color = color;
        this.kind = kind;
        this.x = x;
        this.y = y;

        chessBoard.setPieceAt(x, y, this);
    }

    public Piece(ChessBoard chessBoard, Kind kind, Color color, Point p) {
        this.chessBoard = chessBoard;
        this.color = color;
        this.kind = kind;
        this.x = p.x;
        this.y = p.y;

        chessBoard.setPieceAt(x, y, this);
    }

    static public Piece createPiece(ChessBoard chessBoard, Kind kind, Color color, int x, int y) {
        switch (kind) {
            case KING:
                return new KingPiece(chessBoard, color, x, y);
            case QUEEN:
                return new QueenPiece(chessBoard, color, x, y);
            case ROOK:
                return new RookPiece(chessBoard, color, x, y);
            case BISHOP:
                return new BishopPiece(chessBoard, color, x, y);
            case KNIGHT:
                return new KnightPiece(chessBoard, color, x, y);
            case PAWN:
                return new PawnPiece(chessBoard, color, x, y);
            default:
                throw new RuntimeException("Unknown piece type");
        }
    }

    static public Piece createPiece(ChessBoard chessBoard, Kind kind, Color color, Point p) {
        return createPiece(chessBoard, kind, color, p.x, p.y);
    }
    public int getID () {
        return kind.ordinal() + color.ordinal() * 6;
    }

    public Color getColor() {
        return color;
    }

    public int getX () {
        return x;
    }

    public int getY () {
        return y;
    }

    public Point getXY() {
        return new Point(x, y);
    }

    public void setXY(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setXY(Point p) {
        x = p.x;
        y = p.y;
    }

    void moveTo(int x, int y) {
        Point oldSquare = new Point(getX(), getY());
        Piece takenPiece = chessBoard.getPiece(x, y);

        if (x != this.x || y != this.y) {
            lastMove = new Move(this);
            if (takenPiece == null) {
                lastMove.setPiece1(kind, oldSquare, new Point(x, y));
            }
            else {
                lastMove.setPiece1Piece2(kind, oldSquare, new Point(x, y), takenPiece.kind, new Point(x, y), null);
            }

            setXY(x, y);
        }
        else
        {
            if (chessBoard.getPiece(x, y) == null) {
                chessBoard.setPieceAt(x, y, this);
            }
        }
    }

    void moveTo(Point target) {
        moveTo(target.x, target.y);
    }

    // тестовый ход фигуры на поле (x, y). Должны быть запомнены текущие координаты и взятая фигура если была
    // используется не будет ли находится король под шахом после данного хода
    void testMoveTo(int x, int y) {
        testMoveSource.x = this.x;
        testMoveSource.y = this.y;
        testMoveTarget.x = x;
        testMoveTarget.y = y;
        testMoveTakenPiece = chessBoard.getPiece(x, y);

        chessBoard.setPieceAt(testMoveSource, null);
        chessBoard.setPieceAt(x, y, this);
    }

    // возврат тестового хода. Фигура должна быть возвращена на (x, y), взятая фигура - на свое место
    void testMoveRollback() {
        chessBoard.setPieceAt(testMoveTarget, null);
        chessBoard.setPieceAt(testMoveSource, this);

        if (testMoveTakenPiece != null) {
            chessBoard.setPieceAt(testMoveTakenPiece.x, testMoveTakenPiece.y, testMoveTakenPiece);
        }
    }

    // возвращает true если указанное поле находится под боем данной фигуры
    public abstract boolean isSquareThreaten(int x, int y);

    // возвращает true, если данная фигура может переместиться на указанное поле
    public boolean isMovePossible (int x, int y) {

        return isSquareThreaten(x, y);
    };

    public Kind getKind() {
        return kind;
    }

    // буква, используемая для записи партии
    public char getPieceLetter() {
        return ' ';
    };

    // выдает букву фигуры для FEN-нотации с учетом цвета фигуры
    char getFENPieceLetter() {
        char result = getPieceLetter();

        if (getColor() == Color.WHITE) {
            result = Character.toUpperCase(result);
        } else {
            result = Character.toLowerCase(result);
        }

        return result;
    }

    int getAvailableMoveCount() {
        return availableMoves.size();
    }

    Point getAvailableMove(int index) {
        return availableMoves.get(index);
    }

    void clearAvailableMoves() {
        availableMoves.clear();
    }

    protected boolean checkMoveAvailability(int x, int y) {
        boolean result = false;

        if (x >= 0 && x < 8 && y >= 0 && y < 8) {
            if (isMovePossible(x, y)) {
                Piece piece = chessBoard.getPiece(x, y);

                if (piece == null || (piece.getColor() != color && piece.getKind() != Kind.KING)) {
                    // физически ход возможен, проверим не будет ли после этого наш король под шахом
                    chessBoard.beginUpdate();
                    try {
                        testMoveTo(x, y);
                        result = !chessBoard.isInCheck(color);
                        testMoveRollback();
                    } finally {
                        chessBoard.endUpdate(false);
                    }
                    if (result) {
                        availableMoves.add(new Point(x, y));
                        result = true;
                    }
                }
            }
        }

        return result;
    }

    int generateLineMoves(int dx, int dy) {
        int result = 0;

        for (int curx = x + dx, cury = y + dy; curx >= 0 && cury >= 0 && curx < 8 && cury < 8; curx += dx, cury += dy) {
            if (checkMoveAvailability(curx, cury)) {
                result++;
                if (chessBoard.getPiece(curx, cury) != null) break;
            }
        }
        for (int curx = x - dx, cury = y - dy; curx >= 0 && cury >= 0 && curx < 8 && cury < 8; curx -= dx, cury -= dy) {
            if (checkMoveAvailability(curx, cury)) {
                result++;
                if (chessBoard.getPiece(curx, cury) != null) break;
            }
        }

        return result;
    }

    protected int generateAvailableMoves () {
        availableMoves.clear();

        return 0;
    };
}
