package dmlam.ru.chessboard;

import android.graphics.Point;

import java.util.ArrayList;

import dmlam.ru.chessboard.Game.GameResult;

import static dmlam.ru.chessboard.Game.GameResult.UNKNOWN;

/**
 * Created by Lam on 01.07.2015.
 */

final public class Move {
    private final String NULL_MOVE_NOTATION = "--";

    private static int moveIdGenerator = 0;

    private int id = 0;  // id хода по которому его можно будет найти, например, при клике пользователя по записи партии
    private Piece piece = null; // сходившая фигура. Если null, то это null-ход
    private Piece.Kind piece1Kind = null, piece2Kind = null;
    private Point piece1From = null, piece1To = null, piece2From = null, piece2To = null;
    private ChessBoard.PromoteTo promotePawnTo = null;

    private Piece.Color moveOrder;              // кто сходил - белые или черные
    private int moveNumber = 1;
    private String FEN = null;                  // запись позиции, сформировавшейся после хода
    private String notation = null;             // записи хода в нотации SAN и простой
    private String comment = null;              // комментарий к ходу
    private String info = null;                 // дополнительная информация, не отображающаяся в нотации
    private int numericAnnotationGlyph = 0;     // код аннотации для хода (используется в PGN), т.к. !!, ?, +- ...

    private boolean opponentChecked = false;    // противник получил шах этим ходом
    private boolean opponentCheckmated = false; // противник получил мат этим ходом
    private boolean opponentStalemated = false; // противник попал в патовую ситуацию после этого хода
    private boolean opponentResign = false;     // противник сдался
    private boolean gameDrawn = false;          // игра завершилась вничью

    private GameResult gameResult = UNKNOWN;

    protected MoveList prevVariants;
    private MoveList variants = new MoveList();

    public Move(Piece piece) {
        this.piece = piece;
        this.moveNumber = piece.chessBoard.getMoveNumber(); //piece.chessBoard.getMoveOrder() == Piece.Color.WHITE ? piece.chessBoard.getMoveNumber() + 1 : piece.chessBoard.getMoveNumber();
        id = ++moveIdGenerator;
    }

    public boolean isNullMove() {
        return piece == null;
    }

    @Override
    public String toString() {
        return getFullNotation();
    }

    public ChessBoard.PromoteTo getPromotePawnTo() {
        return promotePawnTo;
    }

    public Piece.Color getMoveOrder() {
        return moveOrder;
    }

    public boolean isOpponentChecked() {
        return opponentChecked;
    }

    public boolean isOpponentCheckmated() {
        return opponentCheckmated;
    }

    public boolean isOpponentStalemated() {
        return opponentStalemated;
    }

    public boolean isGameDrawn() {
        return gameDrawn;
    }

    public void setGameDrawn(boolean gameDrawn) {
        this.gameDrawn = gameDrawn;
    }

    public boolean isOpponentResign() {
        return opponentResign;
    }

    public void setOpponentResign(boolean opponentResign) {
        this.opponentResign = opponentResign;
    }

    public void setGameResult(GameResult gameResult) {
        this.gameResult = gameResult;
    }

    public GameResult getGameResult() {
        return gameResult;
    }

    public Piece.Kind getPiece1Kind() {
        return piece1Kind;
    }

    public Piece.Kind getPiece2Kind() {
        return piece2Kind;
    }

    public Point getPiece1From() {
        return piece1From;
    }

    public Point getPiece1To() {
        return piece1To;
    }

    public Point getPiece2From() {
        return piece2From;
    }

    public Point getPiece2To() {
        return piece2To;
    }

    public int getMoveId() {
        return id;
    }

    public void shiftMoveId(int difference) {
        id += difference;
    }

    public int getMoveNumber() {
        return moveNumber;
    }

    public Move getPrevMove() {
        if (prevVariants == null)
            return null;
        else {
            for (int i = 0; i < prevVariants.size(); i++) {
                if (prevVariants.get(i).getVariants().indexOf(this) >= 0)
                    return prevVariants.get(i);
            }

            return null;
        }
    }

    public MoveList getPrevVariants () {
        return prevVariants;
    }

    public int getVariantCount() {
        return variants.size();
    }

    public Move getVariants(int num) {
        if (num >= variants.size()) {
            return null;
        }
        else
            return variants.get(num);
    }

    public Move getMainVariant() {
        return getVariants(0);
    }

    public MoveList getVariants() {
        return variants;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public void saveMoveState(ChessBoard board) {
        this.moveOrder = board.getMoveOrder();
//        this.moveNumber = board.getMoveNumber();
        this.opponentChecked = board.isCurrentPlayerChecked();
        this.opponentCheckmated = board.isCurrentPlayerCheckmated();
        this.opponentStalemated = board.isCurrentPlayerStalemated();
        this.prevVariants = board.getLastMoveVariants();
        this.FEN = board.saveToFEN();

        if (numericAnnotationGlyph > 0) {

        }

        if (opponentCheckmated || opponentResign) {
            notation += '#';
        }
        else
        if (opponentChecked) {
            notation += '+';
        }

        if (opponentStalemated || gameDrawn) {
            notation += " 1/2-1/2";
        }
    }

    public void saveMoveNotation(ChessBoard board) {
        this.notation = getShortNotation(board);
    }

    public void setPiece1(Piece.Kind kind, Point from, Point to) {
        piece1Kind = kind;
        piece1From = from;
        piece1To = to;
    }

    public void setPiece2(Piece.Kind kind, Point from, Point to) {
        piece2Kind = kind;
        piece2From = from;
        piece2To = to;
    }

    public void setPiece1Piece2(Piece.Kind kind1, Point from1, Point to1, Piece.Kind kind2, Point from2, Point to2) {
        piece1Kind = kind1;
        piece1From = from1;
        piece1To = to1;
        piece2Kind = kind2;
        piece2From = from2;
        piece2To = to2;
    }

    public void setPromotePawnTo(ChessBoard.PromoteTo promotion) {
        promotePawnTo = promotion;
    }

    public Point From() {
        return piece1From;
    }

    public Point To() {
        if (piece1To != null) {
            return piece1To;
        }
        else {
            // превращение пешки на последней горизонтали
            return piece2To;
        }
    }

    private boolean pointsEquals(Point p1, Point p2) {
        boolean result = false;

        result = p1 == p2 || (p1 == null && p2 == null);

        if (!result && p1 != null)
            result = p1.equals(p2);

        return result;
    }

    // возвращает true если этим ходом фигура типа kind пошла с from на to и в случае превращения пешки превратилась в promotePawnTo
    // (либо, если promotePawnTo == null, превращение не учитывается при сравнении)
    public boolean Equals(Piece.Kind kind, Piece.Color color, Point from, Point to, ChessBoard.PromoteTo promotePawnTo) {

        return this.piece1Kind == kind &&
                this.moveOrder.opposite() == color &&
                pointsEquals(piece1From, from) &&
                pointsEquals(piece1To, to) &&
                this.promotePawnTo == promotePawnTo;
    }

    // возвращает true если этим ходом фигура типа kind пошла с from на to, но без учета фигуры в которую, возможно, превратилась пешка
    public boolean Equals(Piece.Kind kind, Piece.Color color, Point from, Point to) {

        return this.piece1Kind == kind &&
                this.moveOrder == color &&
                pointsEquals(piece1From, from) &&
                pointsEquals(piece1To, to);
    }

    // возвращает true если ход совпадает с move
    public boolean Equals(Move move) {
        return !(move == null) &&
                move.getMoveOrder() == moveOrder &&
                move.getPiece1Kind() == piece1Kind &&
                pointsEquals(move.getPiece1From(), piece1From) &&
                pointsEquals(move.getPiece1To(), piece1To) &&
                move.getPiece2Kind() == piece2Kind &&
                pointsEquals(move.getPiece2From(), piece2From) &&
                pointsEquals(move.getPiece2To(), piece2To) &&
                move.promotePawnTo == promotePawnTo;
    }

    public int findVariant(Piece.Kind kind, Piece.Color color, Point from, Point to, ChessBoard.PromoteTo promotePawnTo) {

        for (int i = 0; i < variants.size(); i++) {
            if (variants.get(i).Equals(kind, color, from, to, promotePawnTo))
                return i;
        }

        return -1;
    }

    public String getPGNMove() {
        char[] a = new char[4];

        a[0] = ChessBoard.getLetter(piece1From.x);
        a[1] = (char)((int) '0' + piece1From.y + 1);
        if (piece1To == null) {
            // пешка прошла в фигуру
            a[2] = ChessBoard.getLetter(piece2To.x);
            a[3] = (char) ((int) '0' + piece2To.y + 1);
        }
        else {
            a[2] = ChessBoard.getLetter(piece1To.x);
            a[3] = (char) ((int) '0' + piece1To.y + 1);
        }

        if (piece1To != null) {
            return ChessBoard.squareName(piece1From.x, piece1From.y) + ChessBoard.squareName(piece1To.x, piece1To.y);
        }
        else {
            // превращение пешки на последней горизонтали
            return ChessBoard.squareName(piece1From.x, piece1From.y) + ChessBoard.squareName(piece2To.x, piece2To.y);
        }
    }

    // Выдает нотацию хода в формате SAN: Bxd4, R2d4 (фигура, откуда (если есть другая фигура, могущая пойти туда же), куда)
    private String getShortNotation(ChessBoard board) {
        String result;

        if (isNullMove()) {
            result = NULL_MOVE_NOTATION;
        }
        else {
            if (piece1Kind == Piece.Kind.KING && Math.abs(piece1To.x - piece1From.x) > 1) {  // была рокировка?
                if (piece1To.x == 2) {
                    result = "0-0-0";
                } else {
                    result = "0-0";
                }
            } else {
                StringBuilder sb = new StringBuilder();

                if (piece1Kind == Piece.Kind.PAWN) {
                    sb.append(ChessBoard.getLetter(piece1From.x));

                    if (piece2Kind != null && piece2To == null) {
                        // взятие фигуры или пешки
                        sb.append("x").append(ChessBoard.squareName(piece1To));
                        if (piece1To != null && piece1To.y != piece2From.y) {
                            sb.append(" e.p.");
                        }
                    } else {
                        if (piece1To != null) {
                            // не было превращения в фигуру
                            sb.append(Integer.toString(piece1To.y + 1));
                        } else {
                            sb.append(Integer.toString(piece2To.y + 1));
                        }
                    }
                    // если пешка превращается на последней для нее горизонтали - добавим в кого
                    if (promotePawnTo != null) {
                        sb.append('=').append(promotePawnTo.getNotationLetter());
                    }
                } else {
                    // все остальные фигуры (кроме пешки)
                    sb.append(piece1Kind.getNotationLetter());

                    // Проверим, не существует ли неопределенности с фигурой, совершающей ход (т.е., две одинаковые фигуры, могущие сходить на одну клетку)
                    ArrayList<Piece> ambiguity = new ArrayList<Piece>();

                    for (int i = 0; i < board.getMoveablePiecesCount(); i++) {
                        Piece piece = board.getMoveablePiece(i);
                        if ((piece.getKind() == piece1Kind) && (piece != this.piece)) {

                            for (int j = 0; j < piece.getAvailableMoveCount(); j++) {
                                if (piece.getAvailableMove(j).equals(piece1To)) {
                                    // есть неопределенность - добавим фигуру в список вносящих неопределенность
                                    ambiguity.add(piece);
                                }
                            }
                        }
                    }

                    if (ambiguity.size() > 0) {
                        if (ambiguity.size() == 1) {
                            // только еще одна фигура может сходить на ту же клетку
                            if (ambiguity.get(0).getX() == piece1From.x) {
                                // совпадают горизонтали фигур - добавляем в ход номер вертикали
                                sb.append(piece1From.y + 1);
                            } else {
                                // совпадают вертикали - добавляем в ход наименование горизонтали
                                sb.append(ChessBoard.getLetter(piece1From.x));
                            }
                        } else {
                            // еще две или более фигур могут сходить на ту же клетку, то обозначим фигуру, которая ходила полным названием клетки
                            sb.append(ChessBoard.squareName(piece1From));
                        }
                    }

                    if (piece2Kind != null && piece2To == null) {
                        sb.append('x');
                    }
                    sb.append(ChessBoard.squareName(piece1To));
                }

                result = sb.toString();
            }
        }

        return result;
    }

    // Выдает нотацию хода в простом формате Rd2xd4 (фигура, откуда, куда)
    private String getFullNotation() {
        String result;

        if (isNullMove()) {
            result = NULL_MOVE_NOTATION;
        }
        else {
            if (piece1Kind == Piece.Kind.KING && Math.abs(piece1To.x - piece1From.x) > 1) {  // была рокировка?
                if (piece1To.x == 2) {
                    result = "0-0-0";
                } else {
                    result = "0-0";
                }
            } else {
                StringBuilder sb = new StringBuilder();

                sb.append(piece1Kind.getNotationLetter()).append(ChessBoard.squareName(piece1From));
                if (piece2Kind != null && piece2To == null) {
                    sb.append('x');
                } else {
                    sb.append('-');
                }

                if (piece1To == null) {
                    // превращение пешки в фигуру
                    sb.append(ChessBoard.squareName(piece2To));
                    sb.append(piece2Kind.getNotationLetter());
                } else {
                    sb.append(ChessBoard.squareName(piece1To));
                }

                result = sb.toString();
            }
        }

        return result;
    }

    public String getNotation()
    {
        return notation;
    }

    public String getFEN() {
        return FEN;
    }

    public int getNumericAnnotationGlyph() {
        return numericAnnotationGlyph;
    }

    public void setNumericAnnotationGlyph(int numericAnnotationGlyph) {
        this.numericAnnotationGlyph = numericAnnotationGlyph;
    }


}
