package dmlam.ru.chessboard;

import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.Buffer;
import java.util.ArrayList;

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

        return result;
    }

    private boolean readMoves(BufferedReader in, Game game) throws IOException {
        boolean result = false;

        String moves = "";
        String line;
        boolean lastMoveLine = false;

        line = readLine(in);
        while (line != null && !lastMoveLine) {
            if (!line.isEmpty()) {
                line = line.trim();

                if (line.endsWith("1-0")) {
                    line = line.substring(0, line.length() - 4);
                    lastMoveLine = true;
                    game.setGameResult(Game.GameResult.WHITE);
                }
                if (line.endsWith("0-1")) {
                    line = line.substring(0, line.length() - 4);
                    lastMoveLine = true;
                    game.setGameResult(Game.GameResult.BLACK);
                }
                if (line.endsWith("1/2-1/2")) {
                    line = line.substring(0, line.length() - 8);
                    lastMoveLine = true;
                    game.setGameResult(Game.GameResult.DRAW);
                }
                if (line.endsWith("*")) {
                    line = line.substring(0, line.length() - 2);
                    lastMoveLine = true;
                    game.setGameResult(Game.GameResult.WHITE);
                }

                moves = moves + line;
            }
            line = readLine(in);
        }

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
