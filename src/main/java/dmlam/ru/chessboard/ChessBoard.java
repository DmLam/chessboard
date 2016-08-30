package dmlam.ru.chessboard;

import android.graphics.Point;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.min;

/**
 * Created by Lam on 13.06.2015.
 */

class ErrorIllegalSquareName extends Error {
    public ErrorIllegalSquareName(String square) { super("Illegal square name " + square); }
}

class ErrorIllegalFEN extends Error {
    public ErrorIllegalFEN(String msg) { super(msg); }
}

class ErrorWaitingForPawnPromotion extends  Error {
    public ErrorWaitingForPawnPromotion () {
        super ("Waiting for pawn promotion");
    }
}

public class ChessBoard {
    private static final String LOGTAG = ChessBoard.class.getName();

    public enum Castling {QUEEN, KING};
    public enum PromoteTo {QUEEN, ROOK, BISHOP, KNIGHT;

        public String getNotationLetter() {
            switch(this) {
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
    }

    private OnNeedPieceForTransformation mOnNeedPieceForTransformation;
    private List<IOnMoveListener> mOnMoveListeners = new ArrayList<>();

    private String initialPositionFEN = null;

    private int updateCount = 0;
    private ArrayList<Piece> piecesToMove = new ArrayList<>();
    private Piece[][] squares = new Piece[8][8];
    private boolean castling[][] = new boolean[2][2];  // [color][castling]
    private Point enPassantSquare = null;  // поле на котором возможно взятие на проходе на текущем ходе (пешка только что прошла через него)
    private Piece.Color moveOrder = Piece.Color.WHITE;
    private Point promotedPawn = null;  // проведенная пешка если есть
    private int moveNumber = 1;
    private int halfmoveQnt = 0; // счетчик количества последних незначащих полуходов (не ходов пешкой и ходов, не являющихся взятием фигуры)
                                 // необходим для применения правила 50 ходов
    private int lastMoveIndex = -1;       // индекс последнего хода в вариантах последнего хода (lastMoveVariants)
    private Piece lastMovedPiece = null; // последняя сходившая фигура. Используется для сохранения хода в записи партии после окончания хода
    private MoveList lastMoveVariants = null;  // список вариантов последнего хода (если был rollback, если нет, то в в lastMoveVariants[0] последний ход)
    private MoveList firstMoveVariants = null; // варианты первого хода. Если lastMoveVariants == null, то смотрим firstMoveVariants
    private int moveIdGenerator = 0; // id последнего хода. используется для генерации уникальных id-ов

    private boolean currentPlayerChecked;     // игрок, чья очередь хода находится под шахом
    private boolean currentPlayerMated;       // игрок, чья очередь хода получил мат
    private boolean currentPlayerStalemated;  // игрок, чья очередь хода находится в патовой ситуации
    private boolean gameDrawn;                // игра завершилась вничью
    private char transformation = ' ';        // какая фигура появится при превращении пешки если ход делается методом pgnmove

    public interface OnNeedPieceForTransformation {
        void onNeedPieceForTransformation(Point sourceSquare);
    }

    public static char getLetter(int i) {
        return (char) ((int) 'a' + i);
    }

    public static String squareName(int x, int y) { return getLetter(x) + Integer.toString(y + 1); }

    public static String squareName(Point p) {
        return getLetter(p.x) + Integer.toString(p.y + 1);
    }

    public static Point getPointFromSquareName(String square) {
        char xc = square.charAt(0), yc = square.charAt(1);
        int x = getLetterRow(xc), y = getLetterColumn(yc);

        if (x < 0 || x > 7 || y < 0 || y > 7) {
            throw new ErrorIllegalSquareName(square);
        }

        return new Point(x, y);
    }

    public static int getLetterRow(char c) {
        return (int) c - (int) 'a';
    }

    public static int getLetterColumn(char c) {
        return (int) c - (int) '1';
    }

    public void setOnNeedPieceForTransformation(OnNeedPieceForTransformation l) {
        mOnNeedPieceForTransformation = l;
    }

    public void addOnMoveListener(IOnMoveListener l) {
        mOnMoveListeners.add(l);
    }

    public void deleteOnMoveListener(IOnMoveListener l) {
        mOnMoveListeners.remove(l);
    }

    public Piece getPiece(int x, int y) {
        return squares[x][y];
    }

    public Piece getPiece(Point point) { return getPiece(point.x, point.y); };

    public void setPieceAt(int x, int y, Piece piece) {
        squares[x][y] = piece;
        if (piece != null) {
            piece.setXY(x, y);
        }
    }

    void setPieceAt(Point point, Piece piece) {
        setPieceAt(point.x, point.y, piece);
    }

    boolean movePieceTo(Piece piece, int x, int y) {
        Point sourceSquare = piece.getXY();

        if (isInPawnPromotion())
        {
            // возбудим здесь исключение, т.к. есть пешка, проведенная до последней горизонтали и нам нужна реакция на это событие -
            // либо указать фигуру, в которую она должна превратиться (promotePawnTo), либо отменить ход (cancelPawnPromotion)
            throw new ErrorWaitingForPawnPromotion();
        }

        boolean result = isMovePossible(piece, x, y);

        if (result) {
            lastMovedPiece = piece;

            // очищаем признак возможности взятия на проходе
            enPassantSquare = null;

            // убираем фигуру с клетки где она находилась
            setPieceAt(piece.getX(), piece.getY(), null);

            // если на новой клетке уже что-то есть, то это взятие и нужно сбросить счетчик нерезультативных ходов (для правила 50-ти ходов)
            // аналогично, если ход пешкой
            if ( getPiece(x, y) != null || piece.getKind() == Piece.Kind.PAWN)
            {
                halfmoveQnt = -1;  // -1 т.к. в passMoveToOpponent счетчик будет увеличен на 1
            }
            // ставим на новую клетку
            piece.moveTo(x, y);
            setPieceAt(x, y, piece);

            if (piece.getKind() == Piece.Kind.ROOK) {
                // если был ход ладьей, то установим невозможность соответствующих рокировок
                int horz = piece.getColor() == Piece.Color.WHITE ? 0 : 7;

                if (piece.getX() == 0 && piece.getY() == horz) {
                    castling[piece.getColor().ordinal()][Castling.QUEEN.ordinal()] = false;
                } else if (piece.getX() == 7 && piece.getY() == horz) {
                    castling[piece.getColor().ordinal()][Castling.KING.ordinal()] = false;
                }
                passMoveToOpponent();
            } else if (piece.getKind() == Piece.Kind.KING) {
                // если ход королем, то установим невозможность рокировок и если это собственно рокировка - двинем и соответствующую ладье тоже
                castling[piece.getColor().ordinal()][Castling.KING.ordinal()] = false;
                castling[piece.getColor().ordinal()][Castling.QUEEN.ordinal()] = false;
                passMoveToOpponent();


            } else if (piece.getKind() == Piece.Kind.PAWN && (transformation != ' ' || mOnNeedPieceForTransformation != null)) {
                // проверим, не дошла ли пешка до последней горизонтали и если да - превратим в фигуру
                int lastLine = -1;
                switch (piece.getColor()) {
                    case WHITE:
                        lastLine = 7;
                        break;
                    case BLACK:
                        lastLine = 0;
                        break;
                }
                if (y == lastLine) {
                    // ура, мы прошли!
                    promotedPawn = new Point(x, y);
                    // если ход делался из PGN методом pgnmove, то фигура превращения может быть указана в transformation
                    if (transformation != ' ') {
                        switch (Character.toUpperCase(transformation)) {
                            case 'N':
                                promotePawnTo(sourceSquare, PromoteTo.KNIGHT);
                                break;
                            case 'B':
                                promotePawnTo(sourceSquare, PromoteTo.BISHOP);
                                break;
                            case 'R':
                                promotePawnTo(sourceSquare, PromoteTo.ROOK);
                                break;
                            case 'Q':
                                promotePawnTo(sourceSquare, PromoteTo.QUEEN);
                                break;
                            default:
                                result = false;
                        }
                    }
                    else {
                        // иначе ждем реакции на проведение пешки - должна быть вызвана либо promotePawnTo, либо cancelPawnPromotion
                        // иначе при любой попытке хода будет возбуждено исключение
                        mOnNeedPieceForTransformation.onNeedPieceForTransformation(sourceSquare);
                    }
                }
                else {
                    // если пешка не превращается в фигуру - меняем очередь хода.
                    // иначе поменяем когда будет выбрана фигура для превращения (т.к., ход может быть отменен)
                    passMoveToOpponent();
                }
            }
            else {
                passMoveToOpponent();
            }
        }

        return result;
    }

    boolean movePieceTo(Piece piece, Point target) {
        return movePieceTo(piece, target.x, target.y);
    }

    boolean movePieceTo(Piece piece, Point target, PromoteTo pawnPromotion) {
        if (pawnPromotion != null) {
            switch (pawnPromotion) {
                case KNIGHT:
                    transformation = 'N';
                    break;
                case BISHOP:
                    transformation = 'B';
                    break;
                case ROOK:
                    transformation = 'R';
                    break;
                case QUEEN:
                    transformation = 'Q';
                    break;
            }
        }

        return movePieceTo(piece, target);
    }

    public int getMoveablePiecesCount() {
        return piecesToMove.size();
    }

    public Piece getMoveablePiece(int index) {
        return piecesToMove.get(index);
    }

    // ход в записи вроде "d2d4" или "с7с8q"
    public boolean pgnmove(String pgnMove) {
        boolean result = false;

        if (pgnMove.length() == 5) {
            transformation = pgnMove.charAt(4);
        }
        if (pgnMove.length() == 4 || pgnMove.length() == 5) {
            Piece piece = getPiece(getPointFromSquareName(pgnMove.substring(0, 2)));

            if (piece != null) {
                result = movePieceTo(piece, getPointFromSquareName(pgnMove.substring(2, 4)));
            }
        }

        return result;
    }

    private void checkForCheckCheckmateStalemate() {
        currentPlayerChecked = isInCheck(moveOrder);

        generateAvailableMoves(moveOrder);
        if (piecesToMove.size() == 0) {
            currentPlayerMated = currentPlayerChecked;
            currentPlayerStalemated = !currentPlayerChecked;
            if (currentPlayerStalemated) {
                gameDrawn = true;
            }
        }
    }

    public void restoreBoardState(Move move) {
        loadFromFEN(move.getFEN(), true);
        gameDrawn = move.isGameDrawn();
    }

    private void gotoInitialPosition() {
        loadFromFEN(initialPositionFEN, true);
        gameDrawn = false;
    }

    // возврат хода
    public void rollback() {
        if (lastMoveVariants != null && lastMoveVariants.size() > 0) {
            Move lastMove;

            lastMove = getLastMove();
            lastMoveVariants = lastMove.getPrevVariants();
            lastMoveIndex = -1;
            if (lastMoveVariants != null) {
                for (int i = 0; i < lastMoveVariants.size(); i++) {
                    if (lastMoveVariants.get(i).getMainVariant() == lastMove) {
                        lastMoveIndex = i;
                    }
                }
                if (lastMoveIndex == -1) {
                    lastMoveIndex = 0;
                }
                lastMove = lastMoveVariants.get(lastMoveIndex);

                restoreBoardState(lastMove);
            }
            else {
                beginUpdate();
                try {
                    gotoInitialPosition();
                    lastMoveVariants = null;
                }
                finally {
                    endUpdate();
                }

            }

            doOnRollback(lastMove);
        }
    }

    // возврат хода
    public void rollup(int variantIndex) {
        Move nextMove = null;

        if (lastMoveVariants == null) {
            if (firstMoveVariants != null) {
                lastMoveVariants = firstMoveVariants;
                nextMove = lastMoveVariants.get(variantIndex);
            }
        }
        else {
            nextMove = getLastMove().getVariants(variantIndex);
            if (nextMove != null)
                lastMoveVariants = getLastMove().getVariants();  // установим текущий ход в качестве основного варианта после предыдущего, к которому можно будет вернуться с помощью rollup()
        }
        lastMoveIndex = variantIndex;

        if (nextMove != null) {

            restoreBoardState(nextMove);

            doOnRollup(nextMove);
        }
    }

    // возврат хода из основной линии
    public void rollup() {
        rollup(0);
    }

    private void gotoMove(Move move) {
        if (move != null) {
            beginUpdate();
            try {
                MoveList saveFirstMoveVariants = firstMoveVariants;
                Move prevMove = move.getPrevMove();

                restoreBoardState(move);

                firstMoveVariants = saveFirstMoveVariants;
                if (prevMove != null) {
                    lastMoveVariants = prevMove.getVariants();
                    lastMoveIndex = prevMove.getVariants().indexOf(move);
                } else {
                    lastMoveVariants = firstMoveVariants;
                    lastMoveIndex = firstMoveVariants.indexOf(move);
                }

                doOnGoto(move);
            }
            finally {
                endUpdate();
            }
        }
    }

    // переход в позицию после хода с идентификатором Id
    public void gotoMove(int Id) {
        Move move = findMove(Id);

        if (move == null) {
            throw new RuntimeException("Invalid move id");
        }

        gotoMove(move);
    }

    // переход на полуход number от начала партии
    public void gotoMoveNumber(int number) {
        if (number < 0) {
            throw new RuntimeException("Invalid move number");
        }

        // проверим, что уже был сделан хотя бы один ход
        if (firstMoveVariants != null) {
            gotoInitialPosition();

            Move move = firstMoveVariants.get(0);
            while (number > 1) {
                number--;
                move = move.getMainVariant();
            }
            gotoMove(move);
        }
    }

    public MoveList getLastMoveVariants() {
        return lastMoveVariants;
    }

    public Move getLastMoveVariants(int variantNumber) {
        return lastMoveVariants.get(variantNumber);
    }

    public MoveList getFirstMoveVariants() {
        return firstMoveVariants;
    }

    public Move getFirstMoveVariants(int variantNumber) {
        return firstMoveVariants.get(variantNumber);
    }

    public int getLastMoveIndex() {
        return lastMoveIndex;
    }

    public Move getLastMove() {
        if (lastMoveVariants == null || lastMoveVariants.size() == 0 || lastMoveIndex == -1) {
            return null;
        }
        else {
            return lastMoveVariants.get(lastMoveIndex);
        }
    }

    public void removeMove(Move move) {
        if (move != null) {
            Move lastMove = getLastMove();
            /* получение variants разбито на два этапа и две переменные для отладки - чтобы понять, где возникало nullPointer */
            Move prevMove = move.getPrevMove();
            MoveList variants = prevMove.getVariants();

            if (move != lastMove) {
                if (variants.size() > 1 || variants.get(0).getVariantCount() == 0) {
                    variants.removeLine(move);
                }
            } else {
                lastMoveVariants = move.getPrevVariants();
                lastMoveIndex = 0;
                variants.removeLine(move);
            }
        }
    }

    public Piece.Color getMoveOrder() {
        return moveOrder;
    }

    public void setMoveOrder(Piece.Color moveOrder) {
        this.moveOrder = moveOrder;
    }

    public int getMoveNumber() {
        return moveNumber;
    }

    public void setMoveNumber(int moveNumber) {
        this.moveNumber = moveNumber;
    }

    public int getHalfmoveQnt() {
        return halfmoveQnt;
    }

    public boolean isInPawnPromotion() {
        return promotedPawn != null;
    }

    public void promotePawnTo(Point sourceSquare, PromoteTo piece) {
        PawnPiece pawn = (PawnPiece) getPiece(promotedPawn);

        pawn.promoteTo(sourceSquare, piece);

        halfmoveQnt = -1; // -1 т.к. в passMoveToOpponent счетчик будет увеличен на 1
        passMoveToOpponent();
        promotedPawn = null;
    }

    public void cancelPawnPromotion() {
        if (promotedPawn != null) {
            int prevLine =  promotedPawn.y == 0 ? 1 : 6;
            setPieceAt(promotedPawn.x, prevLine, new PawnPiece(this, moveOrder, promotedPawn.x, prevLine));
        }
    }

    public void SetupInitialPosition(){
        loadFromFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", false);
    }

    public boolean isCastlingPossible(Piece.Color color, Castling castling) {
        boolean result = this.castling[color.ordinal()][castling.ordinal()];

        return result;
    }

    public boolean isMovePossible(Piece piece, int x, int y) {
        boolean result = !currentPlayerMated && !currentPlayerStalemated;

        if (result) {
            Piece targetPiece = getPiece(x, y);

            // проверим очередь хода
            result = piece.getColor() == moveOrder;

            if (result) {
                if (x < 0 || x > 7 || y < 0 || y > 7) {
                    // проверим выход за переделы доски
                    result = false;
                } else if (targetPiece != null) {
                    // если на поле куда идем находится фигура, то она не должна быть своего цвета и это не должен быть король
                    if (targetPiece.getColor() == piece.getColor()) {
                        // нельзя ходить на клетку, где находится фигура того же цвета
                        result = false;
                    } else {
                        // нельзя взять короля
                        if (targetPiece.getKind() == Piece.Kind.KING) {
                            result = false;
                        }
                    }
                }

                if (result) {
                    result = piece.isMovePossible(x, y);

                    if (result) {
                        // если свой король окажется под шахом после хода - ход невозможен
                        beginUpdate();
                        try {
                            piece.testMoveTo(x, y);
                            result = !isInCheck(piece.getColor());
                            piece.testMoveRollback();
                        } finally {
                            endUpdate(false);
                        }
                    }
                }
            }
        }

        return result;
    }

    public int generateAvailableMoves(Piece.Color color) {
        int result = 0;

        beginUpdate();
        try {
            clearPiecesToMove();

            for (int x = 0; x < 8; x++)
                for (int y = 0; y < 8; y++) {
                    Piece piece = getPiece(x, y);

                    if (piece != null && piece.getColor() == color) {
                        int count = piece.generateAvailableMoves();

                        if (count > 0 ) {
                            result += count;
                            piecesToMove.add(piece);
                        }
                    }
                }
        }
        finally {
            endUpdate(false);
        }

        return result;
    }

    public boolean getPlayerChecked(Piece.Color color) {
        return getMoveOrder() == color && currentPlayerChecked;
    }

    public boolean getPlayerCheckmated(Piece.Color color) {
        return getMoveOrder() == color && currentPlayerMated;
    }

    // Возвращает true, если король игрока color находится под шахом
    boolean isInCheck(Piece.Color color) {
        boolean result = false;
        Piece King = null;

        // найдем короля противника
        for (int x = 0; King == null && x <= 7; x++)
            for (int y = 0; King == null && y <= 7; y++)
            {
                Piece piece = getPiece(x, y);
                if (piece != null && piece.getKind() == Piece.Kind.KING && piece.getColor() == color) {
                    King = piece;
                }
            }

        // если нашли (может быть это просто какая-то расстановка без короля), то проверим не угрожает ли ему какая-то фигура
        if (King != null) {
            for (int x = 0; !result && x <= 7; x++)
                for (int y = 0; !result && y <= 7; y++) {
                    Piece piece = getPiece(x, y);

                    if (piece != null && piece.getColor() != color) {
                        result = piece.isSquareThreaten(King.getX(), King.getY());
                    }
                }
        }

        return result;
    }

    public boolean isSquareThreaten(int x, int y, Piece.Color color) {
        boolean result = false;

        for (int cx = 0; !result && cx <= 7; cx++)
            for (int cy = 0; !result && cy <= 7; cy++) {
                Piece piece = getPiece(cx, cy);

                if (piece != null && piece.getColor() == color ) {
                    result = piece.isSquareThreaten(x, y);
                }
            }

        return result;
    }

    public void beginUpdate() {
        updateCount++;
    }

    public void endUpdate() {
        endUpdate(true);
    }

    public void endUpdate(boolean doOnChange) {
        updateCount--;

        if (doOnChange) {
            doOnBoardChange();
        }
    }

    public String saveToFEN() {
        StringBuilder builder = new StringBuilder();

        // расположение фигур
        for (int y = 7; y >= 0; y--) {
            int emptySquares = 0;
            for (int x = 0; x <= 7; x++) {
                if (getPiece(x, y) == null) {
                    emptySquares++;
                } else {
                    if (emptySquares != 0) {
                        builder.append(emptySquares);
                        emptySquares = 0;
                    }
                    builder.append(getPiece(x, y).getFENPieceLetter());
                }
            }
            if (emptySquares != 0) {
                builder.append(emptySquares);
            }

            if (y != 0) {
                builder.append('/');
            }
        }

        // активная сторона
        builder.append(' ');
        builder.append(getMoveOrder().getFENColorLetter());

        // возможность рокировки
        boolean haveCastling = false;
        builder.append(' ');
        if (castling[Piece.Color.WHITE.ordinal()][Castling.KING.ordinal()]) { builder.append('K'); haveCastling = true;}
        if (castling[Piece.Color.WHITE.ordinal()][Castling.QUEEN.ordinal()]) { builder.append('Q'); haveCastling = true; }
        if (castling[Piece.Color.BLACK.ordinal()][Castling.KING.ordinal()]) { builder.append('k'); haveCastling = true; }
        if (castling[Piece.Color.BLACK.ordinal()][Castling.QUEEN.ordinal()]) { builder.append('q'); haveCastling = true; }
        if (!haveCastling) { builder.append('-'); }

        // возможность взятия пешки на проходе
        builder.append(' ');
        if (getEnPassantSquare() != null)
        {
            builder.append(squareName(getEnPassantSquare()));
        } else {
            builder.append('-');
        }

        // счетчик полуходов
        builder.append(' ');
        builder.append(halfmoveQnt);

        // номер хода
        builder.append(' ');
        builder.append(moveOrder == Piece.Color.WHITE ? moveNumber + 1: moveNumber);

        return builder.toString();
    }

    private void lineFromFEN(int line, String position) {
        int x = 0;

        for(int charIndex = 0; charIndex < position.length(); charIndex++) {
            char c = position.charAt(charIndex);

            if (x > 7) {
                throw new ErrorIllegalFEN(String.format("Line too long (%d)", line));
            }

            if (Character.isDigit(c)) {
                int empty = Character.getNumericValue(c);

                x += empty;
                if (x > 8 || (x == 8 && charIndex >= position.length() && position.charAt(charIndex + 1) != '0')) {
                    throw new ErrorIllegalFEN(String.format("Line too long (%d)", line));
                }
            }
            else {
                switch(c) {
                    case 'P':
                        setPieceAt(x, line, new PawnPiece(this, Piece.Color.WHITE, x, line));
                        break;
                    case 'R':
                        setPieceAt(x, line, new RookPiece(this, Piece.Color.WHITE, x, line));
                        break;
                    case 'N':
                        setPieceAt(x, line, new KnightPiece(this, Piece.Color.WHITE, x, line));
                        break;
                    case 'B':
                        setPieceAt(x, line, new BishopPiece(this, Piece.Color.WHITE, x, line));
                        break;
                    case 'K':
                        setPieceAt(x, line, new KingPiece(this, Piece.Color.WHITE, x, line));
                        break;
                    case 'Q':
                        setPieceAt(x, line, new QueenPiece(this, Piece.Color.WHITE, x, line));
                        break;
                    case 'p':
                        setPieceAt(x, line, new PawnPiece(this, Piece.Color.BLACK, x, line));
                        break;
                    case 'r':
                        setPieceAt(x, line, new RookPiece(this, Piece.Color.BLACK, x, line));
                        break;
                    case 'n':
                        setPieceAt(x, line, new KnightPiece(this, Piece.Color.BLACK, x, line));
                        break;
                    case 'b':
                        setPieceAt(x, line, new BishopPiece(this, Piece.Color.BLACK, x, line));
                        break;
                    case 'k':
                        setPieceAt(x, line, new KingPiece(this, Piece.Color.BLACK, x, line));
                        break;
                    case 'q':
                        setPieceAt(x, line, new QueenPiece(this, Piece.Color.BLACK, x, line));
                        break;
                    default:
                        throw new ErrorIllegalFEN(String.format("Unknown piece letter (%c)", c));
                }
                x++;
            }
        }

        if (x < 8) {
            throw new ErrorIllegalFEN(String.format("Line too short (%d)", line));
        }
    }

    private boolean castlingPossible(Piece.Color color, Castling side) {
        boolean result = false;
        Piece pKing = null, pRook = null;

        // Проверим, что король стоит на своем месте
        switch (color) {
            case WHITE:
                pKing = getPiece(getPointFromSquareName("e1"));
                switch (side) {
                    case KING:
                        pRook = getPiece(getPointFromSquareName("h1"));
                        break;
                    case QUEEN:
                        pRook = getPiece(getPointFromSquareName("a1"));
                        break;
                }
                break;
            case BLACK:
                pKing = getPiece(getPointFromSquareName("e8"));
                switch (side) {
                    case KING:
                        pRook = getPiece(getPointFromSquareName("h8"));
                        break;
                    case QUEEN:
                        pRook = getPiece(getPointFromSquareName("a8"));
                        break;
                }
                break;
        }

        if (// проверим, что король на своем месте
            pKing != null && pKing.getKind() == Piece.Kind.KING && pKing.getColor() == color &&
            // проверим, что соответствующая ладья на своем месте
            pRook != null && pRook.getKind() == Piece.Kind.ROOK && pRook.getColor() == color) {

            result = true;
        }

        return result;
    }

    public void loadFromFEN(String FEN) {
        loadFromFEN(FEN, false);
    }

    // загрузка позиции из FEN.
    // Параметр restoring - признак загрузки начальной исследуемой позиции непосредственно из FEN (restoring = false)
    // или восстановления сохраненной позиции после одного из ходов (по rollback(), rollup(), gotoMove()) (restoring = true)
    public void loadFromFEN(String FEN, boolean restoring) {
        int line = 0, charIndex = 0;

        FEN = FEN.trim();

        beginUpdate();
        try {
            // очищаем доску от фигур
            for (int x = 0; x < 8; x++)
                for (int y = 0; y < 8; y++)
                    squares[x][y] = null;

            currentPlayerChecked = false;
            currentPlayerMated = false;
            currentPlayerStalemated = false;

            castling[Piece.Color.WHITE.ordinal()][Castling.KING.ordinal()] = false;
            castling[Piece.Color.WHITE.ordinal()][Castling.QUEEN.ordinal()] = false;
            castling[Piece.Color.BLACK.ordinal()][Castling.KING.ordinal()] = false;
            castling[Piece.Color.BLACK.ordinal()][Castling.QUEEN.ordinal()] = false;

            if (!restoring) {
                initialPositionFEN = FEN;

                lastMoveVariants = null;
                firstMoveVariants = null;
            }

            // строки доски
            while (line < 8) {
                int n = -1;

                if (line == 7) {
                    n = FEN.indexOf(' ', charIndex);
                    if (n == -1) {
                        throw new ErrorIllegalFEN("No active color part is represented");
                    }
                } else {
                    n = FEN.indexOf('/', charIndex);
                    if (n == -1) {
                        throw new ErrorIllegalFEN(String.format("Too few lines (%d)", line + 1));
                    }
                }
                lineFromFEN(7 - line, FEN.substring(charIndex, n));

                charIndex = n + 1;
                line++;
            }

            // активная сторона
            if (FEN.length() > charIndex  && FEN.charAt(charIndex - 1) == ' ') {
                char c = FEN.charAt(charIndex++);
                switch (c) {
                    case 'w':
                        setMoveOrder(Piece.Color.WHITE);
                        break;
                    case 'b':
                        setMoveOrder(Piece.Color.BLACK);
                        break;
                    default:
                        throw new ErrorIllegalFEN(String.format("Unknown active color letter (%c)", c));
                }

                charIndex++;
            } else {
                throw new ErrorIllegalFEN("No active color part is represented");
            }

            // возможность рокировки
            if (FEN.length() > charIndex && FEN.charAt(charIndex - 1) == ' ') {
                char c = FEN.charAt(charIndex++);

                if (c != '-') {
                    do {
                        switch (c) {
                            case 'K':
                                if (castling[Piece.Color.WHITE.ordinal()][Castling.KING.ordinal()]) {
                                    throw new ErrorIllegalFEN("Double white king-side castling is represented");
                                } else {
                                    // в FEN указана возможность рокировки белых в короткую сторону
                                    // проверим, находятся ли фигуры в соответствующих положениях
                                    if (castlingPossible(Piece.Color.WHITE, Castling.KING)) {
                                        castling[Piece.Color.WHITE.ordinal()][Castling.KING.ordinal()] = true;
                                    }
                                }
                                break;
                            case 'Q':
                                if (castling[Piece.Color.WHITE.ordinal()][Castling.QUEEN.ordinal()]) {
                                    throw new ErrorIllegalFEN("Double white queen-side castling is represented");
                                } else {
                                    // в FEN указана возможность рокировки белых в длинную сторону
                                    // проверим, находятся ли фигуры в соответствующих положениях
                                    if (castlingPossible(Piece.Color.WHITE, Castling.QUEEN)) {
                                        castling[Piece.Color.WHITE.ordinal()][Castling.QUEEN.ordinal()] = true;
                                    }
                                }
                                break;
                            case 'k':
                                if (castling[Piece.Color.BLACK.ordinal()][Castling.KING.ordinal()]) {
                                    throw new ErrorIllegalFEN("Double black king-side castling is represented");
                                } else {
                                    // в FEN указана возможность рокировки черных в короткую сторону
                                    // проверим, находятся ли фигуры в соответствующих положениях
                                    if (castlingPossible(Piece.Color.BLACK, Castling.KING)) {
                                        castling[Piece.Color.BLACK.ordinal()][Castling.KING.ordinal()] = true;
                                    }
                                }
                                break;
                            case 'q':
                                if (castling[Piece.Color.BLACK.ordinal()][Castling.QUEEN.ordinal()]) {
                                    throw new ErrorIllegalFEN("Double black queen-side castling is represented");
                                } else {
                                    // в FEN указана возможность рокировки черных в длинную сторону
                                    // проверим, находятся ли фигуры в соответствующих положениях
                                    if (castlingPossible(Piece.Color.BLACK, Castling.QUEEN)) {
                                        castling[Piece.Color.BLACK.ordinal()][Castling.QUEEN.ordinal()] = true;
                                    }
                                }
                                break;
                            default:
                                throw new ErrorIllegalFEN(String.format("Unknown castling letter (%c)", c));
                        }

                        if (FEN.length() > charIndex) {
                            c = FEN.charAt(charIndex++);
                        } else {
                            throw new ErrorIllegalFEN("No en passant part is represented");
                        }

                    } while (c != ' ');
                }
                else {
                    charIndex++;
                }
            } else {
                throw new ErrorIllegalFEN("No castling availability part is represented");
            }

            // взятие на проходе
            if (FEN.length() > charIndex && FEN.charAt(charIndex - 1) == ' ') {
                if (FEN.charAt(charIndex) != '-') {
                        setEnPassantSquare(getPointFromSquareName(FEN.substring(charIndex, charIndex + 2)));
                        charIndex += 3;
                }
                else {
                    setEnPassantSquare(null);
                    charIndex += 2;
                }
            }
            else {
                throw new ErrorIllegalFEN("No en passant part is represented");
            }

            // количество незначащих полуходов
            if (FEN.length() > charIndex && FEN.charAt(charIndex - 1) == ' ') {
                int n = FEN.indexOf(' ', charIndex);
                if (n == -1) {
                    throw new ErrorIllegalFEN("No move number part is represented");
                }

                halfmoveQnt = Integer.valueOf(FEN.substring(charIndex, n));
                charIndex = n + 1;
            }
            else {
                throw new ErrorIllegalFEN("No halfmove clock part is represented");
            }

            // номер хода
            if (FEN.length() > charIndex&& FEN.charAt(charIndex - 1) == ' ') {

                moveNumber = Integer.valueOf(FEN.substring(charIndex, FEN.length()));
            }
            else {
                throw new ErrorIllegalFEN("No move number part is represented");
            }

            checkForCheckCheckmateStalemate();
        } finally {
            endUpdate();
        }

//        doOnBoardChange();  // будет сделано в endUpdate()
    }

    private void doOnBoardChange() {
        if (updateCount == 0) {
            for (int i = 0; i < mOnMoveListeners.size(); i++) {
                mOnMoveListeners.get(i).onBoardChange();
            }
        }
    }

    private boolean doOnMove(Move move) {
        boolean result = true;

        if (updateCount == 0) {
            for (int i = 0; i < mOnMoveListeners.size(); i++) {
                result &= mOnMoveListeners.get(i).onMove(move);
            }
        }

        return result;
    }

    private void doOnRollback(Move move) {
        if (updateCount == 0) {
            for (int i = 0; i < mOnMoveListeners.size(); i++) {
                mOnMoveListeners.get(i).onRollback(move);
            }
        }
    }

    private void doOnRollup(Move move) {
        if (updateCount == 0) {
            for (int i = 0; i < mOnMoveListeners.size(); i++) {
                mOnMoveListeners.get(i).onRollup(move);
            }
        }
    }

    private void doOnGoto(Move move) {
        for (int i = 0; i < mOnMoveListeners.size(); i++) {
            mOnMoveListeners.get(i).onGoto(move);
        }
    }

    public Point getEnPassantSquare() {return enPassantSquare;}

    public void setEnPassantSquare(Point p) {enPassantSquare = p;}

    private void clearPiecesToMove() {
        for (int i = 0; i < piecesToMove.size(); i++) {
            piecesToMove.get(i).clearAvailableMoves();
        }

        piecesToMove.clear();
    }

    public int findMoveVariant(Piece.Kind kind, Piece.Color color,Point from, Point to, ChessBoard.PromoteTo promotePawnTo) {
        int result = -1;
        Move lastMove = getLastMove();

        if (lastMove != null) {
            result = lastMove.findVariant(kind, color, from, to, promotePawnTo);
        }

        return result;
    }

    public boolean isCurrentPlayerChecked() {
        return currentPlayerChecked;
    }

    public boolean isCurrentPlayerCheckmated() {
        return currentPlayerMated;
    }

    public boolean isCurrentPlayerStalemated() {
        return currentPlayerStalemated;
    }

    public boolean isGameDrawn() {
        return gameDrawn;
    }

    public void setMoveList(MoveList moves) {
        if (firstMoveVariants != moves) {
            firstMoveVariants = moves;
            lastMoveVariants = null;
            lastMoveIndex = -1;

            doOnBoardChange();
        }
    }

    // id-ы ходов в новой ветке не должны совпадать с существующими
    public void addLineToLastMove(Move newLine) {
        int newLineMinId = min(newLine.getMoveId(), newLine.getVariants().minId());
        int curMaxId = getFirstMoveVariants().maxId();

        if (newLineMinId <= curMaxId) {
            int difference = curMaxId - newLineMinId + 1;
            newLine.shiftMoveId(difference);
            newLine.getVariants().shiftIds(difference);
        }

        getLastMove().getVariants().addLine(newLine);
    }

    public void doAfterMove(Move move) {
        if (updateCount == 0) {
            for (int i = 0; i < mOnMoveListeners.size(); i++) {
                mOnMoveListeners.get(i).afterMove(move);
            }
        }
    }

    // передаем ход противнику
    void passMoveToOpponent() {
        Move lastMove = lastMovedPiece.lastMove;

        lastMove.saveMoveNotation(this);
        transformation = ' ';
        moveOrder = moveOrder.opposite();
        halfmoveQnt++; // счетчик незначащих полуходов
/*
        if (halfmoveQnt >= 50) {
            gameDrawn = true;
        }
*/

        checkForCheckCheckmateStalemate();
        lastMove.saveMoveState(this);

        if (lastMoveVariants == null) {
            if (firstMoveVariants != null) {
                lastMoveVariants = firstMoveVariants;
            }
            else {
                lastMoveVariants = new MoveList();
                firstMoveVariants = lastMoveVariants;
            }
        }
        else {
            lastMoveVariants = lastMoveVariants.get(0).getVariants();
        }

        lastMoveIndex = -1;
        if (lastMoveVariants.size() > 0) {
            // Проверим, что ранее в этой ветке не было уже такого хода. Если был, то не будем добавлять еще раз
            Move prevMove = lastMoveVariants.get(0).getPrevMove();
            if (prevMove != null) {
                // prevMove == null => только что сделан первый ход
                lastMoveIndex = prevMove.findVariant(lastMove.getPiece1Kind(), moveOrder.opposite(), lastMove.getPiece1From(), lastMove.getPiece1To(), lastMove.getPromotePawnTo());
            }
        }

        // Если только что был сделан первый ход или не было такого варианта - добавим
        if (lastMoveIndex == -1) {
            int moveIndex = lastMoveVariants.locateMove(lastMove);
            if (moveIndex == -1) {
                lastMoveVariants.add(0, lastMove);
                lastMoveIndex = 0;
            } else {
                // переместим вариант в начало
                lastMoveVariants.add(0, lastMoveVariants.remove(moveIndex));
            }
            lastMoveIndex = 0;
        }

        if (doOnMove(lastMove)) {
            if (moveOrder == Piece.Color.WHITE) {
                Move last = getLastMove();

                if (last == null) {
                    moveNumber = 1;
                } else {
                    moveNumber = last.getMoveNumber() + 1;
                }
            }

            doAfterMove(lastMove);
            doOnBoardChange();
        }
        else {
            rollback();
            removeMove(lastMove);
        }
    }

    public int generateId() {
        return ++moveIdGenerator;
    }


    public Move findMove(int id) {
        Move result = null;

        if (firstMoveVariants != null) {
            result = firstMoveVariants.findMove(id);
        }

        return result;
    }
}
