package dmlam.ru.chessboard;

import android.text.TextUtils;

import java.util.ArrayList;

import static dmlam.ru.chessboard.Piece.Color.BLACK;

/**
 * Created by LamdanDY on 29.08.2016.
 */
public class Game {
    private static final String LOGTAG = Game.class.getName();

    public enum GameResult {WHITE, DRAW, BLACK, UNKNOWN};

    private class Tag {
        protected String name, value;

        public Tag(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    public Game(){
    }

    public Game(String startPosition) {
        New(startPosition);
    }

    private String startPosition = null;  // FEN for starting position
    private ArrayList<Tag> tags = new ArrayList<>();
    private MoveList moves = new MoveList();  // variants of the first move
    private GameResult result = null;

    public void addTag(String name, String value) {
        if (name == null) {
            throw new RuntimeException("Tag name can't be empty");
        }
        if (tagByName(name) != null) {
            throw new RuntimeException(String.format("Tag %s already exists", name));
        }

        tags.add(new Tag(name.toUpperCase(), value));
    }

    public String tagByName(String name) {

        if (name != null) {
            name = name.toUpperCase();

            for (Tag tag : tags) {
                if (tag.name.equals(name)) {
                    return tag.value;
                }
            }
        }

        return null;
    }

    public GameResult getResult() {
        return result;
    }

    public void setResult (GameResult gameResult) {
        this.result = gameResult;
    }

    public String getStartPosition() {
        return startPosition;
    }

    public void New() {
        tags.clear();
        moves.clear();
        result = GameResult.UNKNOWN;
        startPosition = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    }

    public void New(String startPosition) {
        New();
        this.startPosition = startPosition;
    }

    public MoveList getMoves() {
        return moves;
    }

    public void setMoves(MoveList moves) {
        this.moves = moves;
    }

    public void setStartPosition(String startPosition) {
        this.startPosition = startPosition;
    }

    public void clearMoves() {
        if (moves != null) {
            moves.clear();
        }
    }

    private StringBuilder formatComment(String comment) {
        StringBuilder result = new StringBuilder();

        if (!TextUtils.isEmpty(comment)) {
            result.append('{')
                .append(comment)
                .append("} ");
        }

        return result;
    }

    private StringBuilder formatMoveNumber(Move move) {
        StringBuilder result = new StringBuilder(String.valueOf(move.getMoveNumber()) + '.');

        if (move.getMoveOrder() == BLACK) {
            result.append(" ...");
        }

        return result;
    }

    private StringBuilder formatAnnotationGlyph(int glyph) {
        StringBuilder result = new StringBuilder();

        if (glyph != 0) {
            result.append(" $").append(glyph);
        }

        return result;
    }

    private String formatAnnotation(String annotation) {
        return annotation == null ? "" : annotation;
    }

    private final boolean WITH_MOVE_NUMBER = false;
    private final boolean WITHOUT_MOVE_NUMBER = true;

    private StringBuilder formatMove(Move move, boolean appendMoveNumber) {
        StringBuilder result;

        if (appendMoveNumber) {
            result = formatMoveNumber(move);
        }
        else {
            result = new StringBuilder();
        }

        result.append(' ')
                .append(move.getPGNMove())
                .append(formatAnnotation(move.getAnnotation()))
                .append(formatAnnotationGlyph(move.getNumericAnnotationGlyph()))
                .append(' ')
                .append(formatComment(move.getComment()));

        return result;
    }

    private StringBuilder formatMoveList(MoveList moves) {
        boolean wantMoveNumber = true;
        Move currentMove;
        StringBuilder result = formatComment(moves.getComment());

        if (moves.size() > 0) {
            currentMove = moves.get(0);

            while (currentMove != null) {
                result.append(formatMove(moves.get(0), wantMoveNumber));
                wantMoveNumber = currentMove.getMoveOrder() == BLACK;

                if (currentMove.getVariantCount() > 1) {
                    for (int i = 1; i < currentMove.getVariantCount(); i++) {
                        result.append('(')
                                .append(formatMove(moves.get(i), WITH_MOVE_NUMBER))
                                .append(' ')
                                .append(formatMoveList(moves.get(i).getVariants()))
                                .append(')');
                    }
                    wantMoveNumber = false;
                }

                currentMove = currentMove.getMainVariant();
            }
        }

        return result;
    }

    public String saveToPGN(){
        if (startPosition == null)
            return null;

        StringBuilder pgn = new StringBuilder();
        boolean fenPresent = false;

        for (Tag tag : tags) {
            pgn.append(String.format("[%s %s]\n", tag.name, tag.value));
            if (tag.name.toUpperCase().equals("FEN")) {
                fenPresent = true;
            }
        }

        if (!fenPresent) {
            pgn.append(String.format("[FEN \"%s\"]\n", startPosition));
        }
        pgn.append("\n")
                .append(formatMoveList(moves));


        return pgn.toString();
    }
}
