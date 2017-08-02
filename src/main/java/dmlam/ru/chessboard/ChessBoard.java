package dmlam.ru.chessboard;

import android.graphics.Point;

import java.util.ArrayList;
import java.util.List;

import dmlam.ru.androidcommonlib.ACRAUtils;
import dmlam.ru.chessboard.Piece.Color;
import dmlam.ru.chessboard.Piece.Kind;

import static dmlam.ru.chessboard.Piece.Color.BLACK;
import static dmlam.ru.chessboard.Piece.Color.WHITE;
import static dmlam.ru.chessboard.Piece.Kind.PAWN;
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

    public final static String STARTING_POSITION_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

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
    private Color moveOrder = WHITE;
    private Point promotedPawn = null;  // проведенная пешка если есть
    private int moveNumber = 1;
    private int halfmoveQnt = 0; // счетчик количества последних незначащих полуходов (не ходов пешкой и ходов, не являющихся взятием фигуры)
                                 // необходим для применения правила 50 ходов

    Game game = new Game();
    private int lastMoveIndex = -1;       // индекс последнего хода в вариантах последнего хода (lastMoveVariants)
    private Move lastMove = null; // последняя сходившая фигура. Используется для сохранения хода в записи партии после окончания хода
    private MoveList lastMoveVariants = null;  // список вариантов последнего хода (если был rollback, если нет, то в в lastMoveVariants[0] последний ход)

    private boolean currentPlayerChecked;     // игрок, чья очередь хода находится под шахом
    private boolean currentPlayerMated;       // игрок, чья очередь хода получил мат
    private boolean currentPlayerStalemated;  // игрок, чья очередь хода находится в патовой ситуации
    private char transformation = ' ';        // какая фигура появится при превращении пешки если ход делается методом fromtomove

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

    public void setNextMoveNumber(int nextMoveNumber) {

        this.moveNumber = nextMoveNumber;
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
        return (int) Character.toLowerCase(c) - (int) 'a';
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

    public Piece getPiece(Point point) { return squares[point.x][point.y]; };

    public void setPieceAt(int x, int y, Piece piece) {
        squares[x][y] = piece;
        if (piece != null) {
            piece.setXY(x, y);
        }
    }

    void setPieceAt(Point point, Piece piece) {
        setPieceAt(point.x, point.y, piece);
    }

    // forceNewVariant = true - если необходимо создать новый вариант, даже если в вариантах продолжения текущего хода уже есть такой ход
    boolean movePieceTo(Piece piece, int x, int y, boolean forceNewVariant) {
        Point sourceSquare = piece.getXY();

        if (isInPawnPromotion())
        {
            // возбудим здесь исключение, т.к. есть пешка, проведенная до последней горизонтали и нам нужна реакция на это событие -
            // либо указать фигуру, в которую она должна превратиться (promotePawnTo), либо отменить ход (cancelPawnPromotion)
            throw new ErrorWaitingForPawnPromotion();
        }

        boolean result = isMovePossible(piece, x, y);

        if (result) {
            // очищаем признак возможности взятия на проходе
            enPassantSquare = null;

            // убираем фигуру с клетки где она находилась
            setPieceAt(piece.getX(), piece.getY(), null);

            // если на новой клетке уже что-то есть, то это взятие и нужно сбросить счетчик нерезультативных ходов (для правила 50-ти ходов)
            // аналогично, если ход пешкой
            if ( squares[x][y] != null || piece.getKind() == PAWN)
            {
                halfmoveQnt = -1;  // -1 т.к. в passMoveToOpponent счетчик будет увеличен на 1
            }
            // ставим на новую клетку
            piece.moveTo(x, y);
            lastMove = piece.lastMove;
            setPieceAt(x, y, piece);

            if (ACRAUtils.getCustomData("start loading problem") == null) {
                // если задача уже загружена, то делаем лог ходов.
                // В процессе загрузки и разбора задачи ходы не логгируем
                String m = piece.lastMove.toString();
                if (ACRAUtils.getCustomData("moves") != null) {
                    m = ACRAUtils.getCustomData("moves") + " " + m;
                }
                ACRAUtils.putCustomData("moves", m);
            }

            if (piece.getKind() == Kind.ROOK) {
                // если был ход ладьей, то установим невозможность соответствующих рокировок
                int horz = piece.getColor() == WHITE ? 0 : 7;

                if (piece.getX() == 0 && piece.getY() == horz) {
                    castling[piece.getColor().ordinal()][Castling.QUEEN.ordinal()] = false;
                } else if (piece.getX() == 7 && piece.getY() == horz) {
                    castling[piece.getColor().ordinal()][Castling.KING.ordinal()] = false;
                }
                passMoveToOpponent(forceNewVariant);
            } else if (piece.getKind() == Kind.KING) {
                // если ход королем, то установим невозможность рокировок и если это собственно рокировка - двинем и соответствующую ладье тоже
                castling[piece.getColor().ordinal()][Castling.KING.ordinal()] = false;
                castling[piece.getColor().ordinal()][Castling.QUEEN.ordinal()] = false;
                passMoveToOpponent(forceNewVariant);


            } else if (piece.getKind() == PAWN && (transformation != ' ' || mOnNeedPieceForTransformation != null)) {
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
                    // если ход делался методом fromtomove, то фигура превращения может быть указана в transformation
                    if (transformation != ' ') {
                        switch (Character.toUpperCase(transformation)) {
                            case 'N':
                                promotePawnTo(sourceSquare, PromoteTo.KNIGHT, forceNewVariant);
                                break;
                            case 'B':
                                promotePawnTo(sourceSquare, PromoteTo.BISHOP, forceNewVariant);
                                break;
                            case 'R':
                                promotePawnTo(sourceSquare, PromoteTo.ROOK, forceNewVariant);
                                break;
                            case 'Q':
                                promotePawnTo(sourceSquare, PromoteTo.QUEEN, forceNewVariant);
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
                    passMoveToOpponent(forceNewVariant);
                }
            }
            else {
                passMoveToOpponent(forceNewVariant);
            }
        }

        return result;
    }

    boolean movePieceTo(Piece piece, Point target) {
        return movePieceTo(piece, target, false);
    }

    boolean movePieceTo(Piece piece, Point target, boolean forceNewVariant) {
        return movePieceTo(piece, target.x, target.y, forceNewVariant);
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
    public boolean fromtomove(String fromtoMove) {
        return fromtomove(fromtoMove, false);
    }

    public boolean fromtomove(String fromtoMove, boolean forceNewVariant) {
        boolean result = false;

        if (fromtoMove.length() == 5) {
            transformation = fromtoMove.charAt(4);
        }
        if (fromtoMove.length() == 4 || fromtoMove.length() == 5) {
            Point p = getPointFromSquareName(fromtoMove.substring(0, 2));
            Piece piece = squares[p.x][p.y];

            if (piece != null) {
                result = movePieceTo(piece, getPointFromSquareName(fromtoMove.substring(2, 4)), forceNewVariant);
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
        }
    }

    public void restoreBoardState(Move move) {
        loadFromFEN(move.getFEN(), true);
    }

    private void gotoInitialPosition() {
        loadFromFEN(initialPositionFEN, true);
    }

    // откат к предыдущему ходу
    public void rollback(int variantIndex) {
        if (lastMoveVariants != null && lastMoveVariants.size() > 0) {
            Move lastMove;

            lastMove = getLastMove();
            lastMoveVariants = lastMove.getPrevVariants();
            lastMoveIndex = -1;

            if (lastMoveVariants != null || variantIndex > 0) {
                // if current move is the first and variant index is defferent from 0 then we'll got the 'index out of range' exception
                lastMoveIndex = variantIndex;
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

    // откат к предыдущему ходу (из основной ветки)
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
                doOnBoardChange();
            }
            else {
                beginUpdate();
                try {
                    gotoInitialPosition();
                    lastMoveVariants = null;
                }
                finally {
                    endUpdate(true);
                }

            }

            doOnRollback(lastMove);
        }
    }

    // возврат хода
    public void rollup(int variantIndex) {
        Move nextMove = null;

        if (lastMoveVariants == null) {
            if (!game.getMoves().isEmpty()) {
                lastMoveVariants = game.getMoves();
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
            doOnBoardChange();

            doOnRollup(nextMove);
        }
    }

    // возврат хода из основной линии
    public void rollup() {
        rollup(0);
    }

    public void moveVariantUp(Move move) {
        if (move.getPrevMove() != null) {
            MoveList variants = move.getPrevMove().getVariants();
            if (variants != null) {
                int index = variants.indexOf(move);

                if (variants.size() > 1 && index > 0) {
                    variants.remove(index);
                    variants.add(index - 1, move);
                }
            }
        }
    }

    public void moveVariantDown(Move move) {
        if (move.getPrevMove() != null) {
            MoveList variants = move.getPrevMove().getVariants();
            if (variants != null) {
                int index = variants.indexOf(move);

                if (variants.size() > 1 && index < variants.size() - 1) {
                    variants.remove(index);
                    variants.add(index + 1, move);
                }
            }
        }
    }

    private void gotoMove(Move move) {
        if (move != null) {
            beginUpdate();
            try {
                MoveList saveFirstMoveVariants = game.getMoves();
                Move prevMove = move.getPrevMove();

                restoreBoardState(move);

                game.setMoves(saveFirstMoveVariants);
                if (prevMove != null) {
                    lastMoveVariants = prevMove.getVariants();
                    lastMoveIndex = prevMove.getVariants().indexOf(move);
                } else {
                    lastMoveVariants = game.getMoves();
                    lastMoveIndex = game.getMoves().indexOf(move);
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
            throw new RuntimeException(String.format("Invalid move id (%d)", Id));
        }

        gotoMove(move);
    }

    // переход на полуход number от начала партии
    public void gotoMoveNumber(int number) {
        if (number < 0) {
            throw new RuntimeException("Invalid move number");
        }

        // проверим, что уже был сделан хотя бы один ход
        if (!game.getMoves().isEmpty()) {
            gotoInitialPosition();

            Move move = game.getMoves().get(0);
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
        return game.getMoves();
    }

    public Move getFirstMoveVariants(int variantNumber) {
        return game.getMoves().get(variantNumber);
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

            if (lastMove != null) {
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
    }

    public Color getMoveOrder() {
        return moveOrder;
    }

    public void setMoveOrder(Color moveOrder) {
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

    public void promotePawnTo(Point sourceSquare, PromoteTo piece, boolean forceNewVariant) {
        PawnPiece pawn = (PawnPiece) getPiece(promotedPawn);

        pawn.promoteTo(sourceSquare, piece);

        halfmoveQnt = -1; // -1 т.к. в passMoveToOpponent счетчик будет увеличен на 1
        passMoveToOpponent(forceNewVariant);
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

    public boolean isCastlingPossible(Color color, Castling castling) {
        boolean result = this.castling[color.ordinal()][castling.ordinal()];

        return result;
    }

    public boolean isMovePossible(Piece piece, int x, int y) {
        boolean result = !currentPlayerMated && !currentPlayerStalemated;

        if (result) {
            Piece targetPiece = squares[x][y];

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
                        if (targetPiece.getKind() == Kind.KING) {
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

    public int generateAvailableMoves(Color color) {
        int result = 0;

        beginUpdate();
        try {
            clearPiecesToMove();

            for (int x = 0; x < 8; x++)
                for (int y = 0; y < 8; y++) {
                    Piece piece = squares[x][y];

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

    public boolean getPlayerChecked(Color color) {
        return getMoveOrder() == color && currentPlayerChecked;
    }

    public boolean getPlayerCheckmated(Color color) {
        return getMoveOrder() == color && currentPlayerMated;
    }

    // Возвращает true, если король игрока color находится под шахом
    boolean isInCheck(Color color) {
        boolean result = false;
        Piece King = null;

        // найдем короля противника
        for (int x = 0; King == null && x <= 7; x++)
            for (int y = 0; King == null && y <= 7; y++)
            {
                Piece piece = squares[x][y];
                if (piece != null && piece.getKind() == Kind.KING && piece.getColor() == color) {
                    King = piece;
                }
            }

        // если нашли (может быть это просто какая-то расстановка без короля), то проверим не угрожает ли ему какая-то фигура
        if (King != null) {
            int KingX = King.getX(), KingY = King.getY();

            for (int x = 0; !result && x <= 7; x++)
                for (int y = 0; !result && y <= 7; y++) {
                    Piece piece = squares[x][y];

                    if (piece != null && piece.getColor() != color) {
                        result = piece.isSquareThreaten(KingX, KingY);
                    }
                }
        }

        return result;
    }

    public boolean isSquareThreaten(int x, int y, Color color) {
        boolean result = false;

        for (int cx = 0; !result && cx <= 7; cx++)
            for (int cy = 0; !result && cy <= 7; cy++) {
                Piece piece = squares[cx][cy];

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

        if (doOnChange && updateCount == 0) {
            doOnBoardChange();
        }
    }

    public String saveToFEN() {
        StringBuilder builder = new StringBuilder();

        // расположение фигур
        for (int y = 7; y >= 0; y--) {
            int emptySquares = 0;
            for (int x = 0; x <= 7; x++) {
                if (squares[x][y] == null) {
                    emptySquares++;
                } else {
                    if (emptySquares != 0) {
                        builder.append(emptySquares);
                        emptySquares = 0;
                    }
                    builder.append(squares[x][y].getFENPieceLetter());
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
        if (castling[WHITE.ordinal()][Castling.KING.ordinal()]) { builder.append('K'); haveCastling = true;}
        if (castling[WHITE.ordinal()][Castling.QUEEN.ordinal()]) { builder.append('Q'); haveCastling = true; }
        if (castling[BLACK.ordinal()][Castling.KING.ordinal()]) { builder.append('k'); haveCastling = true; }
        if (castling[BLACK.ordinal()][Castling.QUEEN.ordinal()]) { builder.append('q'); haveCastling = true; }
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
        builder.append(moveOrder == WHITE ? moveNumber + 1: moveNumber);

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
                        setPieceAt(x, line, new PawnPiece(this, WHITE, x, line));
                        break;
                    case 'R':
                        setPieceAt(x, line, new RookPiece(this, WHITE, x, line));
                        break;
                    case 'N':
                        setPieceAt(x, line, new KnightPiece(this, WHITE, x, line));
                        break;
                    case 'B':
                        setPieceAt(x, line, new BishopPiece(this, WHITE, x, line));
                        break;
                    case 'K':
                        setPieceAt(x, line, new KingPiece(this, WHITE, x, line));
                        break;
                    case 'Q':
                        setPieceAt(x, line, new QueenPiece(this, WHITE, x, line));
                        break;
                    case 'p':
                        setPieceAt(x, line, new PawnPiece(this, BLACK, x, line));
                        break;
                    case 'r':
                        setPieceAt(x, line, new RookPiece(this, BLACK, x, line));
                        break;
                    case 'n':
                        setPieceAt(x, line, new KnightPiece(this, BLACK, x, line));
                        break;
                    case 'b':
                        setPieceAt(x, line, new BishopPiece(this, BLACK, x, line));
                        break;
                    case 'k':
                        setPieceAt(x, line, new KingPiece(this, BLACK, x, line));
                        break;
                    case 'q':
                        setPieceAt(x, line, new QueenPiece(this, BLACK, x, line));
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

    private boolean castlingPossible(Color color, Castling side) {
        boolean result = false;
        Piece pKing = null, pRook = null;

        // Проверим, что король стоит на своем месте
        switch (color) {
            case WHITE:
                pKing = squares[4][0]; // getPiece(getPointFromSquareName("e1"));
                switch (side) {
                    case KING:
                        pRook = squares[7][0]; //getPiece(getPointFromSquareName("h1"));
                        break;
                    case QUEEN:
                        pRook = squares[0][0]; // getPiece(getPointFromSquareName("a1"));
                        break;
                }
                break;
            case BLACK:
                pKing = squares[4][7]; // getPiece(getPointFromSquareName("e8"));
                switch (side) {
                    case KING:
                        pRook = squares[7][7]; // getPiece(getPointFromSquareName("h8"));
                        break;
                    case QUEEN:
                        pRook = squares[0][7]; // getPiece(getPointFromSquareName("a8"));
                        break;
                }
                break;
        }

        if (// проверим, что король на своем месте
            pKing != null && pKing.getKind() == Kind.KING && pKing.getColor() == color &&
            // проверим, что соответствующая ладья на своем месте
            pRook != null && pRook.getKind() == Kind.ROOK && pRook.getColor() == color) {

            result = true;
        }

        return result;
    }

    public void setupStartingPosition() {
        loadFromFEN(STARTING_POSITION_FEN);
    }

    public void loadFromFEN(String FEN) {
        loadFromFEN(FEN, false);
    }

    // загрузка позиции из FEN.
    // Параметр restoring - признак загрузки начальной исследуемой позиции непосредственно из FEN (restoring = false)
    // или восстановления сохраненной позиции после одного из ходов (по rollback(), rollup(), gotoMove()) (restoring = true)
    private void loadFromFEN(String FEN, boolean restoring) {
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

            castling[WHITE.ordinal()][Castling.KING.ordinal()] = false;
            castling[WHITE.ordinal()][Castling.QUEEN.ordinal()] = false;
            castling[BLACK.ordinal()][Castling.KING.ordinal()] = false;
            castling[BLACK.ordinal()][Castling.QUEEN.ordinal()] = false;

            if (!restoring) {
                initialPositionFEN = FEN;

                lastMoveVariants = null;
                game.clearMoves();
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
                        setMoveOrder(WHITE);
                        break;
                    case 'b':
                        setMoveOrder(BLACK);
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
                                if (castling[WHITE.ordinal()][Castling.KING.ordinal()]) {
                                    throw new ErrorIllegalFEN("Double white king-side castling is represented");
                                } else {
                                    // в FEN указана возможность рокировки белых в короткую сторону
                                    // проверим, находятся ли фигуры в соответствующих положениях
                                    if (castlingPossible(WHITE, Castling.KING)) {
                                        castling[WHITE.ordinal()][Castling.KING.ordinal()] = true;
                                    }
                                }
                                break;
                            case 'Q':
                                if (castling[WHITE.ordinal()][Castling.QUEEN.ordinal()]) {
                                    throw new ErrorIllegalFEN("Double white queen-side castling is represented");
                                } else {
                                    // в FEN указана возможность рокировки белых в длинную сторону
                                    // проверим, находятся ли фигуры в соответствующих положениях
                                    if (castlingPossible(WHITE, Castling.QUEEN)) {
                                        castling[WHITE.ordinal()][Castling.QUEEN.ordinal()] = true;
                                    }
                                }
                                break;
                            case 'k':
                                if (castling[BLACK.ordinal()][Castling.KING.ordinal()]) {
                                    throw new ErrorIllegalFEN("Double black king-side castling is represented");
                                } else {
                                    // в FEN указана возможность рокировки черных в короткую сторону
                                    // проверим, находятся ли фигуры в соответствующих положениях
                                    if (castlingPossible(BLACK, Castling.KING)) {
                                        castling[BLACK.ordinal()][Castling.KING.ordinal()] = true;
                                    }
                                }
                                break;
                            case 'q':
                                if (castling[BLACK.ordinal()][Castling.QUEEN.ordinal()]) {
                                    throw new ErrorIllegalFEN("Double black queen-side castling is represented");
                                } else {
                                    // в FEN указана возможность рокировки черных в длинную сторону
                                    // проверим, находятся ли фигуры в соответствующих положениях
                                    if (castlingPossible(BLACK, Castling.QUEEN)) {
                                        castling[BLACK.ordinal()][Castling.QUEEN.ordinal()] = true;
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
            endUpdate(!restoring);
        }
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

    public int findMoveVariant(Kind kind, Color color,Point from, Point to, ChessBoard.PromoteTo promotePawnTo) {
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

    public void setMoveList(MoveList moves) {
        if (game.getMoves() != moves) {
            game.setMoves(moves);
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
    void passMoveToOpponent(boolean forceNewVariant) {
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
            if (game.getMoves() != null) {
                lastMoveVariants = game.getMoves();
            }
            else {
                lastMoveVariants = new MoveList();
                game.setMoves(lastMoveVariants);
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
        if (lastMoveIndex == -1 || forceNewVariant) {
            int moveIndex = lastMoveVariants.locateMove(lastMove);

            if (moveIndex == -1 || forceNewVariant) {
                lastMoveVariants.add(0, lastMove);
                lastMoveIndex = 0;
            } else {
                // переместим вариант в начало
                lastMoveVariants.add(0, lastMoveVariants.remove(moveIndex));
            }
            lastMoveIndex = 0;
        }

        if (doOnMove(lastMove)) {
            if (moveOrder == WHITE) {
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
            beginUpdate();
            try {
                rollback();
            }
            finally {
                endUpdate(false);
            }
            removeMove(lastMove);
        }
    }

    public Move findMove(int id) {
        Move result = null;

        if (!game.getMoves().isEmpty()) {
            result = game.getMoves().findMove(id);
        }

        return result;
    }

    private void listPieces(Kind kind, Color color, ArrayList<Piece> pieces) {
        for (int x = 0; x <= 7; x++)
            for (int y = 0; y <= 7; y++)
            {
                Piece piece = squares[x][y];
                if (piece != null && piece.getKind() == kind && piece.getColor() == color) {
                    pieces.add(piece);
                }
            }
    }
    public boolean shortMove(Color player, String move) {
        return shortMove(player, move, false);
    }

    // make move by short notation (like e4, d1Q, Rae2, ...)
    public boolean shortMove(Color player, String move, boolean forceNewVariant) {
        boolean result = false;

        if (move != null && !move.isEmpty()) {
            move = move.toLowerCase();

            if (move.charAt(move.length() - 1) == '+' || move.charAt(move.length() - 1) == '#') {
                move = move.substring(0, move.length() - 1);
            }

            if (move.equals("--") || move.equals("<>")) {
                // null-move
                lastMove = new Move(null);
                passMoveToOpponent(false);
                result = true;
            }
            else
            if (!move.isEmpty()) {
                char pieceLetter = move.charAt(0);
                ArrayList<Piece> pieces = new ArrayList<Piece>();

                if (pieceLetter == 'o') {
                    // castling
                    if (move.equals("o-o")) {
                        if (player == WHITE) {
                            result = fromtomove("e1g1", forceNewVariant);
                        }
                        else {
                            result = fromtomove("e8g8", forceNewVariant);
                        }
                    }
                    else
                    if (move.equals("o-o-o")) {
                        if (player == WHITE) {
                            result = fromtomove("e1c1", forceNewVariant);
                        }
                        else {
                            result = fromtomove("e8c8", forceNewVariant);
                        }
                    }
                }
                else
                if (move.length() > 2 && "kqrbn".indexOf(pieceLetter) >= 0) {
                    String to = move.substring(move.length() - 2);
                    Point p;

                    try {
                        p = getPointFromSquareName(to);
                    }
                    catch(ErrorIllegalSquareName E) {
                        p = null;
                    }

                    if (p != null) {
                        Piece targetPiece = squares[p.x][p.y];
                        boolean capturing;

                        move = move.substring(1, move.length() - 2);
                        capturing = move.length() > 0 && move.substring(move.length() - 1).equals("x");
                        if (capturing) {
                            move = move.substring(0, move.length() - 1);
                        }

                        if ((capturing && targetPiece != null && targetPiece.getColor() != player) ||
                                (targetPiece == null)) {

                            listPieces(Piece.Kind.kindByLetter(Character.toUpperCase(pieceLetter)), player, pieces);
                            for (int i = pieces.size() - 1; i >= 0; i--) {
                                if (!isMovePossible(pieces.get(i), p.x, p.y)) {
                                    pieces.remove(i);
                                }
                            }

                            if (pieces.size() > 1) {
                                // there are more than 1 piece can be placed on the square
                                char fromposchar = move.charAt(0);

                                if (Character.isDigit(fromposchar)) {
                                    int y = getLetterColumn(fromposchar);

                                    for (int i = pieces.size() - 1; i >= 0; i--) {
                                        if (pieces.get(i).getY() != y) {
                                            pieces.remove(i);
                                        }
                                    }
                                } else {
                                    int x = getLetterRow(fromposchar);

                                    for (int i = pieces.size() - 1; i >= 0; i--) {
                                        if (pieces.get(i).getX() != x) {
                                            pieces.remove(i);
                                        }
                                    }
                                }
                            }

                            if (pieces.size() == 1) {
                                // the only possible move is rest
                                result = movePieceTo(pieces.get(0), p, forceNewVariant);
                            }
                        }
                    }
                } else {
                    // pawn move - special case
                    String to;
                    Point p;

                    listPieces(PAWN, player, pieces);

                    transformation = move.charAt(move.length() - 1);

                    if ("qrbn".indexOf(transformation) >= 0) {
                        // promoting
                        move = move.substring(0, move.length() - 1);
                        if (move.length() > 0 && "/=".indexOf(move.charAt(move.length()-1)) >= 0) {
                            // notation like e8/Q or e8=Q - simply delete last character
                            move = move.substring(0, move.length() - 1);
                        }
                    }
                    else {
                        transformation = ' ';
                    }

                    to = move.substring(move.length() - 2);
                    try {
                        p = getPointFromSquareName(to);
                    }
                    catch(ErrorIllegalSquareName E) {
                        p = null;
                    }

                    if (p != null) {
                        move = move.substring(0, move.length() - 2);

                        if (move.length() == 2 && move.charAt(1) == 'x') {
                            // capturing
                            if (squares[p.x][p.y] != null && squares[p.x][p.y].getColor() != player) {
                                int fromRow = getLetterRow(move.charAt(0));

                                if (fromRow >= 0 && fromRow <= 7) {
                                    for (int i = pieces.size() - 1; i >= 0; i--) {
                                        if (pieces.get(i).getX() != fromRow || !isMovePossible(pieces.get(i), p.x, p.y)) {
                                            pieces.remove(i);
                                        }
                                    }

                                    if (pieces.size() == 1) {
                                        // the only possible move is rest
                                        result = movePieceTo(pieces.get(0), p, forceNewVariant);
                                    }
                                }
                            }
                        }
                        else
                        // move here should be empty, in other case this is a wrong move
                        if ("".equals(move)) {
                            for (int i = pieces.size() - 1; i >= 0; i--) {
                                if (!isMovePossible(pieces.get(i), p.x, p.y)) {
                                    pieces.remove(i);
                                }
                            }

                            if (pieces.size() == 1) {
                                // the only possible move is rest
                                result = movePieceTo(pieces.get(0), p, forceNewVariant);
                            }
                        }
                    }
                }
            }
        }

        return result;
    }
}
