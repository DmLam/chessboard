package dmlam.ru.chessboard;

import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import static dmlam.ru.chessboard.Game.GameResult.DRAW;
import static dmlam.ru.chessboard.Game.GameResult.UNKNOWN;
import static dmlam.ru.chessboard.Piece.Color.BLACK;
import static dmlam.ru.chessboard.Piece.Color.WHITE;

/**
 * Created by LamdanDY on 25.08.2016.
 */


public class PGNLoader {
    private static final String LOGTAG = PGNLoader.class.getName();

    private int lineNum = 0;

    public class PGNError extends Exception{
        public PGNError(String message) {
            super(message);
        }
    };

    private ArrayList<Game> games = new ArrayList<Game>();
    private ChessBoard chessboard;

    private String readLine(BufferedReader in) throws IOException {
        String line;

        line = in.readLine();
        if (line != null) {
            line = line.trim();
            lineNum++;
        }

        return line;
    }

    private boolean readTag(BufferedReader in, Game game) throws PGNError, IOException {
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
                        throw new PGNError("Wrong PGN format (no space after tag name)");
                    }
                }
                else {
                    throw new PGNError("Unexpected end of line");
                }
            }
            else {
                throw new PGNError("Unexpected end of file: no moves section");
            }
        }
        else {
            in.reset();
        }

        return result;
    }

    private boolean readTags(BufferedReader in, Game game) throws PGNError, IOException  {
        boolean result = false;

        while (readTag(in, game)) {
            result = true;
        }

        return result;
    }

    @NonNull
    private void skipSpace(StringBuilder moves) {
        int i;

        for (i = 0; i < moves.length() && moves.charAt(i) == ' '; i++);
        if (i > 0) {
            moves.delete(0, i);
        }
    }

    private int parseNumericAnnotationGlyph(StringBuilder moves) {
        StringBuilder sb = new StringBuilder();

        if (moves.charAt(0) == '$') {
            int i = 1;
            while (i < moves.length() && Character.isDigit(moves.charAt(i))) {
                sb.append(moves.charAt(i));
                i++;
            }
            moves.delete(0, i);
            skipSpace(moves);
        }

        return Integer.parseInt(sb.toString());
    }

    private String parseComment(StringBuilder moves) throws PGNError {
        String result = null;

        if (moves.length() > 0) {
            skipSpace(moves);

            char c = moves.charAt(0);

            if (c == '{') {
                int commentEnd = moves.indexOf("}");

                if (commentEnd > 0) {
                    result = moves.substring(1, commentEnd - 1);
                    moves.delete(0, commentEnd + 1);
                }
                else {
                    throw new PGNError("End of comment not found");
                }
            }
        }

        return result;
    }

    private void parseMove(Game game, StringBuilder moves) throws PGNError {
        int i;

        if (moves != null && moves.length() > 0) {
            int moveNumber;
            Piece.Color moveOrder;

            skipSpace(moves);

            char c = moves.charAt(0);

            if (!Character.isDigit(c)) {
                if (chessboard.getLastMove() == null) {
                    moveNumber = 1;
                }
                else {
                    moveNumber = chessboard.getLastMove().getMoveNumber();
                }
            }
            else
            {
                StringBuilder num = new StringBuilder();

                num.append(c);

                for (i = 1; i < moves.length() && Character.isDigit(moves.charAt(i)); i++) {
                    num.append(moves.charAt(i));
                }

                moves.delete(0, i);  // delete move number from moves string
                moveNumber = Integer.valueOf(num.toString());
                skipSpace(moves);
            }

            if (chessboard.getLastMove() == null) {
                chessboard.setNextMoveNumber(moveNumber);
            }

            if (moves.length() > 0 && moves.charAt(0) == '.') {
                if (moves.length() > 1) {
                    if (moves.length() > 2 && moves.charAt(1) == '.' && moves.charAt(2) == '.') {
                        moveOrder = BLACK;
                        moves.delete(0, 3);
                    }
                    else {
                        moveOrder = WHITE;
                        moves.delete(0, 1);
                    }
                }
                else {
                    throw new PGNError("Incorrect move number");
                }
            }
            else {
                moveOrder = chessboard.getLastMove().getMoveOrder();
            }

            skipSpace(moves);

            StringBuilder sb = new StringBuilder();

            for (i = 0; i < moves.length() && "12345678abcdefghkqrbnx+#o-=/".indexOf(Character.toLowerCase(moves.charAt(i))) >= 0; i++) {
                sb.append(moves.charAt(i));
            }

            if (i == 0) {
                throw new PGNError("Incorrect move");
            }

            moves.delete(0, i);
            skipSpace(moves);

            if (!chessboard.shortMove(moveOrder, sb.toString(), true))
                throw new PGNError(String.format("Incorrect move '%s'", sb.toString()));
        }
    }

    private boolean parseMoves(Game game, StringBuilder moves, int variantLevel) throws PGNError {
        boolean result = true;
        Move variantStart = null;
        String comment = parseComment(moves);

        if (!"".equals(comment)) {
            if (chessboard.getLastMove() == null) {
                chessboard.getFirstMoveVariants().setComment(comment);
            } else {
                chessboard.getLastMove().getVariants().setComment(comment);
            }
        }

        while (moves.length() > 0) {
            do {
                if (moves.length() > 0) {
                    Game.GameResult r = null;

                    if (moves.charAt(0) == '*') {
                        r = UNKNOWN;
                    }
                    else
                    if (moves.length() > 2) {
                        String s = moves.substring(0, 3);

                        if (s.equals("1/2")) {
                            r = DRAW;
                        } else
                        if (s.equals("1-0")) {
                            r = Game.GameResult.WHITE;
                        } else
                        if (s.equals("0-1")) {
                            r = Game.GameResult.BLACK;
                        }
                    }

                    if (r != null) {
                        if (chessboard.getLastMove() != null) {
                            chessboard.getLastMove().setGameResult(r);
                            return true;
                        }
                        else {
                            game.setResult(r);
                            return true;
                        }
                    }
                }

                parseMove(game, moves);
                if (variantStart == null) {
                    variantStart = chessboard.getLastMove();
                }

                do {
                    if (moves.length() == 0) {
                        break;
                    }

                    if (moves.charAt(0) == '{') {
                        chessboard.getLastMove().setComment(parseComment(moves));
                        skipSpace(moves);
                    }
                    else
                    if (moves.charAt(0) == '$') {
                        chessboard.getLastMove().setNumericAnnotationGlyph(parseNumericAnnotationGlyph(moves));
                    }
                    else
                    if (moves.charAt(0) == '(') {
                        // variant
                        int lastMoveId = chessboard.getLastMove().getMoveId();
                        MoveList prevVariants = chessboard.getLastMove().getPrevVariants();
                        int prevMoveIndex = prevVariants == null ? 0 : prevVariants.size() - 1;

                        moves.delete(0, 1);

                        chessboard.rollback();
                        parseMoves(game, moves, variantLevel + 1);
                        chessboard.gotoMove(lastMoveId);
                        if (moves.length() == 0 || "()".indexOf(moves.charAt(0)) < 0) {
                            // after a variant can be only another variant or earlier variant ending
                            break;
                        }
                    }
                    else
                    if (moves.charAt(0) == ')') {
                        // end of variant
                        moves.delete(0, 1);
                        skipSpace(moves);
                        chessboard.moveVariantDown(variantStart);

                        return true;
                    }
                    else {
                        break;
                    }

                } while (true);
            }
            while (moves.length() > 0);
        }

        return result;
    }

    private boolean readMoves(BufferedReader in, Game game) throws IOException, PGNError {
        boolean result = false;
        String FEN = game.tagByName("FEN");
        String moves = "";
        String line;

        chessboard = new ChessBoard();
        if (FEN != null) {
            game.setStartPosition(FEN);
            chessboard.loadFromFEN(FEN);
        }

        readLine(in);  // skip empty line between tags and moves

        line = readLine(in);
        while (!"".equals(line)) {
            line = line.trim();

            if (!moves.isEmpty()) {
                moves = moves + ' ';
            }
            moves = moves + line;

            line = readLine(in);
        }

        result = parseMoves(game, new StringBuilder(moves), 1);
        game.setMoves(chessboard.getFirstMoveVariants());

        return result;
    }

    private boolean readGame(BufferedReader in) throws PGNError, IOException  {
        boolean result = false;
        Game game = new Game();

        if (readTags(in, game) && readMoves(in, game)) {
            games.add(game);
            result = true;
        }

        return result;
    }

    public void createFileIndex(String fileName, String indexFileName) throws PGNError {
        FileReader fr = null;
        FileWriter fw = null;
        ArrayList<Integer> idx = new ArrayList<Integer>();

        try {
            fr = new FileReader(fileName);
        }
        catch(FileNotFoundException E) {
            Log.e(LOGTAG, String.format(LOGTAG + " PGN file not found (%s)", fileName));

            throw new PGNError(String.format("PGN file not found (%s)\n%s", fileName, E.toString()));
        }

        try {
            File f = new File(indexFileName);

            if (!f.exists()) {
                f.createNewFile();
                f.setWritable(true);
            }

            fw = new FileWriter(f);
        }
        catch(IOException E) {
            Log.e(LOGTAG, String.format(LOGTAG + " PGN file not found (%s)", fileName));

            throw new PGNError(String.format("Can't create pgn index (%s)", fileName));
        }

        BufferedReader in = new BufferedReader(fr);

        idx.add(0); // index of the first game
        try {
            int gameNo = 1;
            try {
                try {
                    while (readGame(in)) {
                        idx.add(lineNum);
                        gameNo++;
                    };
                    idx.remove(idx.size() - 1);
                }
                catch (Exception E) {
                    Log.e(LOGTAG, String.format(LOGTAG + "Error reading PGN file %s (game %d, line %d):\n%s", fileName, gameNo, lineNum, E.getMessage()));
                }
            } finally {
                in.close();
            }
        }
        catch (IOException E) {
            Log.e(LOGTAG, String.format(LOGTAG + " Error reading file %s:\n%s", fileName, E.toString()));
        }

        BufferedWriter out = new BufferedWriter(fw);

        try {
            for (int i = 0; i < idx.size(); i++) {
                int index = idx.get(i);
                for (int j = 1; j <= 4; j++) {
                    int c = index % 256;

                    index = index >> 8;
                    out.write(c);
                }
            }
        }catch (IOException E) {
            Log.e(LOGTAG, String.format(LOGTAG + "Error writing index %s :\n%s", indexFileName, E.getMessage()));

            throw new PGNError(String.format("Error creating pgn index (%s)", fileName));
        }
    }

    public PGNLoader(String fileName) throws PGNError {
        FileReader fr = null;
        String indexFileName = fileName;
        int pointPos = indexFileName.lastIndexOf(".");

        if (pointPos > 0) {
            indexFileName = indexFileName.substring(0, pointPos);
        }
        indexFileName.concat(".idx");

        createFileIndex(fileName, indexFileName);

        try {
            fr = new FileReader(fileName);
        }
        catch(IOException E) {
            Log.e(LOGTAG, String.format(LOGTAG + " PGN file not found (%s)", fileName));
        }


        BufferedReader in = new BufferedReader(fr);

        try {
            int gameNo = 1;
            try {
                try {
                    while (readGame(in)) {
                        gameNo++;
                    };
                }
                catch (Exception E) {
                    Log.e(LOGTAG, String.format(LOGTAG + "Error reading PGN file %s (game %d, line %d):\n%s", fileName, gameNo, lineNum, E.getMessage()));
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

    public Game getGame(int index) {
        return games.get(index);
    }

}
