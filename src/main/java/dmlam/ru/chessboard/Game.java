package dmlam.ru.chessboard;

import java.util.ArrayList;

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
    private ArrayList<Tag> tags = new ArrayList<Tag>();
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
}
