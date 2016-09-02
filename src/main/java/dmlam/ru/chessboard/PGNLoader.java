package dmlam.ru.chessboard;

import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import static dmlam.ru.chessboard.Piece.Color.BLACK;
import static dmlam.ru.chessboard.Piece.Color.WHITE;

/**
 * Created by LamdanDY on 25.08.2016.
 */


public class PGNLoader {
    private static final String LOGTAG = PGNLoader.class.getName();

    private int lineNum = 0;

    private class WrongPGN extends Exception{
        public WrongPGN(String message) {
            super(message);
        }
    };
    private ArrayList<Game> games = new ArrayList<Game>();
    private ChessBoard chessboard = new ChessBoard();

    private String readLine(BufferedReader in) throws IOException {
        String line;

        line = in.readLine();
        if (line != null) {
            line = line.trim();
            lineNum++;
        }

        return line;
    }

    private boolean readTag(BufferedReader in, Game game) throws WrongPGN, IOException {
        boolean result = false;

        char[] c = new char[1];
        int count = 0;

        in.mark(1);
        if (in.read(c) == 1 && c[0] == '[') {
            String line = readLine(in);

            if (line != null) {
                if (line.charAt(line.length() - 1) == ']') {
                    int spacePos = line.indexOf(' ');
                    if (spacePos >= 0) {
                        String name = line.substring(0, spacePos);
                        String value = line.substring(spacePos + 2, line.length() - 2);  // deleting also quotation

                        if (game.tagByName(name) == null) {
                            game.addTag(name, value);
                            result = true;
                        }
                    }
                    else {
                        throw new WrongPGN("Wrong PGN format (no space after tag name)");
                    }
                }
                else {
                    throw new WrongPGN("Unexpected end of line");
                }
            }
            else {
                throw new WrongPGN("Unexpected end of file: no moves section");
            }
        }
        else {
            in.reset();
        }

        return result;
    }

    private boolean readTags(BufferedReader in, Game game) throws WrongPGN, IOException  {
        boolean result = false;

        while (readTag(in, game)) {
            result = true;
        }

        String FEN = game.tagByName("FEN");
        if (FEN != null) {
            chessboard.loadFromFEN(FEN);
        }

        return result;
    }

    @NonNull
    private String skipSpace(String moves) {
        int i;

        for (i = 0; i < moves.length() && moves.charAt(i) == ' '; i++);
        if (i > 0) {
            moves = moves.substring(i);
        }
        return moves;
    }

    private String parseComment(String moves) throws WrongPGN {
        String result = null;

        if (moves.length() > 0) {
            moves = skipSpace(moves);

            char c = moves.charAt(0);

            if (c == '{') {
                int commentEnd = moves.indexOf('}');

                if (commentEnd > 0) {
                    result = moves.substring(1, commentEnd - 1);
                    moves = moves.substring(commentEnd + 1);
                }
                else {
                    throw new WrongPGN("End of comment not found");
                }
            }
        }


        return result;
    }

    private Move parseMove(Game game, String moves, Move lastMove) throws WrongPGN {
        Move move = null;
        int i;

        if (moves != null && !moves.isEmpty()) {
            int moveNumber;
            Piece.Color moveOrder;
            Piece piece;

            skipSpace(moves);

            char c = moves.charAt(0);

            if (!Character.isDigit(c)) {
                if (lastMove == null) {
                    moveNumber = 1;
                    moveOrder = WHITE;
                }
                else {
                    moveNumber = move.getMoveNumber();
                    moveOrder = move.getMoveOrder().opposite();
                }
            }
            else {
                StringBuilder num = new StringBuilder(c);

                for (i = 1; i <moves.length() && Character.isDigit(moves.charAt(i)); i++) {
                    num.append(moves.charAt(i));
                };
                moves = moves.substring(i);  // delete move number from moves string
                moveNumber = Integer.valueOf(num.toString());

                skipSpace(moves);
                if (!moves.isEmpty() && moves.charAt(0) == '.') {
                    if (moves.length() > 1) {
                        if (moves.length() > 2 && moves.charAt(1) == '.' && moves.charAt(2) == '.') {
                            moveOrder = BLACK;
                            moves = moves.substring(3);
                        }
                        else {
                            moveOrder = WHITE;
                            moves = moves.substring(1);
                        }
                    }
                    else {
                        throw new WrongPGN("Incorrect move number");
                    }
                }
                else {
                    moveOrder = WHITE;
                }
            }

            skipSpace(moves);

            StringBuilder sb = new StringBuilder();

            for (i = 0; i < moves.length() && Character.isLetterOrDigit(moves.charAt(i)); i++) {
                sb.append(moves.charAt(i));
            }
            if (i == 0) {
                throw new WrongPGN("Incorrect move");
            }

            moves = moves.substring(i);
            skipSpace(moves);

            chessboard.shortMove(sb.toString());
        }

        return move;
    }

    private boolean parseMoves(Game game, String moves) throws WrongPGN {
        boolean result = true;
        int currentMoveNumber = 1;
        Move lastMove = null;

        while (!moves.isEmpty()) {

            game.getMoves().setComment(parseComment(moves));

            do {
                lastMove = parseMove(game, moves, currentMoveNumber);
                lastMove.getVariants().add(lastMove);
            }
            while (lastMove != null);

        }

        return result;
    }

    private boolean readMoves(BufferedReader in, Game game) throws IOException, WrongPGN {
        boolean result = false;

        String moves = "";
        String line;
        boolean lastMoveLine = false;

        line = readLine(in);  // skip empty line between tags and moves

        line = readLine(in);
        while (line != null && !line.isEmpty()) {
            line = line.trim();

            if (!moves.isEmpty()) {
                moves = moves + ' ';
            }
            moves = moves + line;

            line = readLine(in);
        }

        result = parseMoves(game, moves);

        return result;
    }

    private boolean readGame(BufferedReader in) throws WrongPGN, IOException  {
        boolean result = false;
        Game game = new Game();

        if (readTags(in, game) && readMoves(in, game)) {
            games.add(game);
            result = true;
        }

        return result;
    }

    public PGNLoader(String fileName) {
        FileReader fr = null;

        try {
            fr = new FileReader(fileName);
        }
        catch(IOException E) {
            Log.e(LOGTAG, String.format(LOGTAG + " PGN file not found (%s)", fileName));
        }


        BufferedReader in = new BufferedReader(fr);

        try {
            try {
                try {

                    while (readGame(in)) ;
                } catch (WrongPGN E) {
                    Log.e(LOGTAG, String.format(LOGTAG + "Error reading PGN file %s (line %d):\n%s", fileName, lineNum, E.getMessage()));
                }
            } finally {
                in.close();
            }
        }
        catch (IOException E) {
            Log.e(LOGTAG, String.format(LOGTAG + " Error reading file %s:\n%s", fileName, E.toString()));
        }
    }

    public int getGamesCount() {
        return games.size();
    }
}
