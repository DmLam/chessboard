package dmlam.ru.chessboard;

import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import dmlam.ru.androidcommonlib.FileNameUtils;

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

    String fileName;
    ArrayList<Integer> gamesIndex = new ArrayList<Integer>();
    private ChessBoard chessboard;

    private String readLine(@NonNull BufferedReader in) throws IOException {
        String line;

        line = in.readLine();
        if (line != null) {
            line = line.trim();
            lineNum++;
        }

        return line;
    }

    private boolean readTag(@NonNull BufferedReader in, Game game) throws PGNError, IOException {
        boolean result = false;

        char[] c = new char[1];

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
                    } else {
                        throw new PGNError("Wrong PGN format (no space after tag name)");
                    }
                } else {
                    throw new PGNError("Unexpected end of line");
                }
            } else {
                throw new PGNError("Unexpected end of file: no moves section");
            }
        } else {
            in.reset();
        }

        return result;
    }

    private boolean readTags(BufferedReader in, Game game) throws PGNError, IOException {
        boolean result = false;

        while (readTag(in, game)) {
            result = true;
        }

        return result;
    }

    @NonNull
    private void skipSpace(@NonNull StringBuilder moves) {
        int i;

        for (i = 0; i < moves.length() && moves.charAt(i) == ' '; i++) ;
        if (i > 0) {
            moves.delete(0, i);
        }
    }

    private int parseNumericAnnotationGlyph(StringBuilder moves) {
        StringBuilder sb = new StringBuilder();
        int result = 0;

        if (!TextUtils.isEmpty(moves) && moves.charAt(0) == '$') {
            int i = 1;
            while (i < moves.length() && Character.isDigit(moves.charAt(i))) {
                sb.append(moves.charAt(i));
                i++;
            }
            moves.delete(0, i);
            skipSpace(moves);

            if (i > 1) {
                result = Integer.parseInt(sb.toString());
            }
        }

        return result;
    }

    private String parseAnnotation(StringBuilder moves) {
        String result = null;
        StringBuilder sb = new StringBuilder();

        if (!TextUtils.isEmpty(moves)) {
            for (int i = 0; i < moves.length() && "!?".indexOf(moves.charAt(i)) >= 0; i++)
                sb.append(moves.charAt(i));
            if (sb.length() > 0) {
                moves.delete(0, sb.length());
                result = sb.toString();
            }
        }

        return result;
    }

    private String parseComment(@NonNull StringBuilder moves) throws PGNError {
        String result = null;

        if (moves.length() > 0) {
            skipSpace(moves);

            char c = moves.charAt(0);

            if (c == '{') {
                int commentEnd = moves.indexOf("}");

                if (commentEnd > 0) {
                    result = moves.substring(1, commentEnd);
                    moves.delete(0, commentEnd + 1);
                    skipSpace(moves);
                } else {
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
                } else {
                    moveNumber = chessboard.getLastMove().getMoveNumber();
                }
            } else {
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
                    } else {
                        moveOrder = WHITE;
                        moves.delete(0, 1);
                    }
                } else {
                    throw new PGNError("Incorrect move number");
                }
            } else {
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

            if (moves.length() >= 4 && moves.substring(0, 4).equals("e.p.")) {
                sb.append(" e.p.");
                moves.delete(0, 4);
                skipSpace(moves);
            }

            if (!chessboard.shortMove(moveOrder, sb.toString(), true))
                throw new PGNError(String.format("Incorrect move '%s'", sb.toString()));

            chessboard.getLastMove().setNumericAnnotationGlyph(parseNumericAnnotationGlyph(moves));
            chessboard.getLastMove().setAnnotation(parseAnnotation(moves));

            skipSpace(moves);
        }

    }

    private boolean parseMoves(Game game, StringBuilder moves, int variantLevel) throws PGNError {
        boolean result = true;
        Move variantStart = null;
        String variantComment = parseComment(moves);  // parse comment if there is and add it to line comment after adding the first line's move

        if (moves.length() == 0 && variantComment != null) {
            // there is only comment in this variant
            if (chessboard.getLastMove() != null) {
                chessboard.getLastMove().getVariants().setComment(variantComment);
            }
            else {
                chessboard.getFirstMoveVariants().setComment(variantComment);
            }
        }

        while (moves.length() > 0) {
            do {
                if (moves.length() > 0) {
                    Game.GameResult r = null;

                    if (moves.charAt(0) == '*') {
                        r = UNKNOWN;
                    } else if (moves.length() > 2) {
                        String s = moves.substring(0, 3);

                        if (s.equals("1/2")) {
                            r = DRAW;
                        } else if (s.equals("1-0")) {
                            r = Game.GameResult.WHITE;
                        } else if (s.equals("0-1")) {
                            r = Game.GameResult.BLACK;
                        }
                    }

                    if (r != null) {
                        if (chessboard.getLastMove() != null) {
                            chessboard.getLastMove().setGameResult(r);
                        }
                        game.setResult(r);

                        return true;
                    }
                }

                parseMove(game, moves);
                if (variantComment != null) {
                    chessboard.getLastMove().getVariants().setComment(variantComment);
                    variantComment = null;
                }
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
                    } else if (moves.charAt(0) == '(') {
                        // variant
                        int lastMoveId = chessboard.getLastMove().getMoveId();

                        moves.delete(0, 1);

                        chessboard.rollback();
                        parseMoves(game, moves, variantLevel + 1);
                        chessboard.gotoMove(lastMoveId);
                        if (moves.length() > 0 && moves.charAt(0) == '{') {
                            String oldComment = chessboard.getLastMove().getComment();
                            String newComment = parseComment(moves);

                            if (oldComment != null) {
                                newComment = oldComment + ' ' + newComment;
                            }
                            chessboard.getLastMove().setComment(newComment);
                        }
                        if (moves.length() == 0 || "()".indexOf(moves.charAt(0)) < 0) {
                            // after a variant can be only another variant or earlier variant ending
                            break;
                        }
                    } else if (moves.charAt(0) == ')') {
                        // end of variant
                        moves.delete(0, 1);
                        skipSpace(moves);
                        chessboard.moveVariantDown(variantStart);
                        return true;
                    } else {
                        break;
                    }

                } while (true);
            }
            while (moves.length() > 0);
        }

        return result;
    }

        private boolean readMoves(BufferedReader in, @NonNull Game game) throws IOException, PGNError {
        boolean result;
        String FEN = game.tagByName("FEN");
        StringBuilder moves = new StringBuilder();
        String line;

        chessboard = new ChessBoard();
        if (FEN == null) {
            FEN = chessboard.STARTING_POSITION_FEN;
        }
        game.setStartPosition(FEN);
        chessboard.loadFromFEN(FEN);

        line = readLine(in);
        if (line == null) {
            throw new PGNError("Bad pgn: unexpected end of file");
        }
        // skip empty line between tags and moves
        if (!TextUtils.isEmpty(line)) {
            throw new PGNError("Bad pgn: empty line after tags expected");
        }

        line = readLine(in);
        while (line != null) {
            line = line.trim();

            if (moves.length() > 0) {
                moves.append(' ');
            }
            moves.append(line);

            line = readLine(in);
        }

        result = parseMoves(game, new StringBuilder(moves), 1);
        game.setMoves(chessboard.getFirstMoveVariants());

        return result;
    }

    private Game readGame(BufferedReader in) throws PGNError, IOException {
        Game result = new Game();

        if (!readTags(in, result) || !readMoves(in, result)) {
            result = null;
        }

        return result;
    }

    private void createFileIndex(String fileName, String indexFileName) throws PGNError {
        FileReader fr;
        FileOutputStream fos;

        try {
            fr = new FileReader(fileName);
        } catch (FileNotFoundException E) {
            Log.e(LOGTAG, String.format(LOGTAG + " PGN file not found (%s)", fileName));

            throw new PGNError(String.format("PGN file not found (%s)\n%s", fileName, E.toString()));
        }

        try {
            File f = new File(indexFileName);

            if (!f.exists()) {
                f.createNewFile();
                f.setWritable(true);
            }

            fos = new FileOutputStream(f);
        } catch (IOException E) {
            Log.e(LOGTAG, String.format(LOGTAG + " PGN file not found (%s)", fileName));

            throw new PGNError(String.format("Can't create pgn index (%s)", fileName));
        }

        BufferedReader in = new BufferedReader(fr);

        gamesIndex.clear();
        gamesIndex.add(0); // index of the first game
        try {
            int gameNo = 1;
            try {
                try {
                    while (readGame(in) != null) {
                        gamesIndex.add(lineNum);
                        gameNo++;
                    }
                    ;
                    gamesIndex.remove(gamesIndex.size() - 1);
                } catch (Exception E) {
                    Log.e(LOGTAG, String.format(LOGTAG + "Error reading PGN file %s (game %d, line %d):\n%s", fileName, gameNo, lineNum, E.getMessage()));
                }
            } finally {
                in.close();
            }
        } catch (IOException E) {
            Log.e(LOGTAG, String.format(LOGTAG + " Error reading file %s:\n%s", fileName, E.toString()));
        }

        byte[] buf = new byte[gamesIndex.size() * 4];

        for (int i = 0; i < gamesIndex.size(); i++) {
            int index = gamesIndex.get(i);
            buf[i * 4] = (byte) index;
            index /= 256;
            buf[i * 4 + 1] = (byte) index;
            index /= 256;
            buf[i * 4 + 2] = (byte) index;
            index /= 256;
            buf[i * 4 + 3] = (byte) index;
        }

        try {
            fos.write(buf);
            fos.close();
        } catch (IOException E) {
            Log.e(LOGTAG, String.format(LOGTAG + "Error writing index %s :\n%s", indexFileName, E.getMessage()));

            throw new PGNError(String.format("Error creating pgn index (%s)", fileName));
        }
    }

    private int bytes2int(byte b1, byte b2, byte b3, byte b4) {
        return (b1 >= 0 ? b1 : 256 + b1) +
                (b2 >= 0 ? b2 : 256 + b2) * 256 +
                (b3 >= 0 ? b3 : 256 + b3) * 256 * 256 +
                (b4 >= 0 ? b4 : 256 + b4) * 256 * 256 * 256;
    }

    private void loadIndexFile(String indexFileName) throws PGNError {
        File f = new File(indexFileName);
        int fileSize = (int) f.length();
        byte[] buf = new byte[fileSize];
        FileInputStream fis;

        if ((fileSize >> 2) << 2 != fileSize) {
            throw new PGNError(String.format("Error reading PGN index file [size mismatch %d] (%s)", fileSize, indexFileName));
        }

        try {
            fis = new FileInputStream(f);

            fis.read(buf);
        } catch (FileNotFoundException E) {
            Log.e(LOGTAG, String.format(LOGTAG + " PGN index file not found (%s)", indexFileName));
            throw new PGNError(String.format("PGN index file not found (%s)\n%s", indexFileName, E.toString()));
        } catch (IOException E) {
            Log.e(LOGTAG, String.format(LOGTAG + " Error reading PGN index file (%s)", indexFileName));
            throw new PGNError(String.format("Error reading PGN index file (%s)\n%s", indexFileName, E.toString()));
        }

        gamesIndex.clear();
        for (int i = 0; i < fileSize; i += 4) {
            int idx = bytes2int(buf[i], buf[i + 1], buf[i + 2], buf[i + 3]);

            gamesIndex.add(idx);
        }
    }

    private void updateIndexFile(String fileName) throws PGNError {
        String indexFileName = FileNameUtils.changeExtension(fileName, "gamesIndex");
        File f = new File(indexFileName);

        if (!f.exists()) {
            createFileIndex(fileName, indexFileName);
        } else {
            loadIndexFile(indexFileName);
        }
    }

    public int getGamesCount() {
        return gamesIndex.size();
    }

    public Game getGame(String fileName, int index) throws PGNError {
        Game result;

        if (!fileName.equals(this.fileName)) {
            updateIndexFile(fileName);

            this.fileName = fileName;
        }

        try {
            FileReader fr = new FileReader(fileName);
            BufferedReader reader = new BufferedReader(fr);

            for (int i = 0; i < gamesIndex.get(index); i++) {
                reader.readLine();
            }

            result = readGame(reader);
        } catch (IOException E) {
            Log.e(LOGTAG, String.format(LOGTAG + "Error reading PGN file %s (game %d, line %d):\n%s", fileName, index, lineNum, E.toString()));

            throw new PGNError(String.format(LOGTAG + "Error reading PGN file %s (game %d, line %d, message '%s')", fileName, index, lineNum, E.getMessage()));
        } catch (PGNError E) {
            Log.e(LOGTAG, String.format(LOGTAG + "Error reading PGN file %s (game %d, line %d):\n%s", fileName, index, lineNum, E.toString()));

            throw new PGNError(String.format(LOGTAG + "Error reading PGN file %s (game %d, line %d)", fileName, index, lineNum));
        }

        return result;
    }

    public Game getGame(String moves) throws PGNError {
        Game result;
        StringReader stringReader = new StringReader(moves);
        BufferedReader reader = new BufferedReader(stringReader);

        try {
            result = readGame(reader);
        } catch (IOException E) {
            Log.e(LOGTAG, String.format(LOGTAG + "Error reading PGN moves (%s, line %d): %s", moves, lineNum, E.toString()));

            throw new PGNError(String.format(LOGTAG + "Error reading PGN moves (%s, line %d): %s", moves, lineNum, E.getMessage()));
        } catch (PGNError E) {
            Log.e(LOGTAG, String.format(LOGTAG + "Error reading PGN moves (%s, line %d): %s", moves, lineNum, E.toString()));

            throw new PGNError(String.format(LOGTAG + "Error reading PGN moves (%s, line %d): %s", moves, lineNum, E.getMessage()));
        }

        return result;
    }

}
