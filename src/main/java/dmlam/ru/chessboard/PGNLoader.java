package dmlam.ru.chessboard;

import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by LamdanDY on 25.08.2016.
 */


public class PGNLoader {
    private static final String LOGTAG = PGNLoader.class.getName();

    private class WrongPGN extends Exception{};
    private ArrayList<Game> games = new ArrayList<Game>();

    private boolean readTag(BufferedReader in, Game game) throws WrongPGN {
        boolean result = false;

        try {
            char[] c = new char[1];
            int count = 0;

            in.mark(1);
            if (in.read(c) == 1 && c[0] == '[') {
                String line = in.readLine().trim();

                if (line != null && line.charAt(line.length()) == ']') {
                    int spacePos = line.indexOf(' ');
                    if (spacePos >= 0) {

                    }
                    else {
                        throw new WrongPGN();
                    }

                }
                else {
                    throw new WrongPGN();
                }
            }
            else {
                in.reset();
            }

        }
        catch (IOException E) {
            Log.e(LOGTAG, String.format(LOGTAG + " Error marking position: %s", E.toString()));
        }

        return result;
    }

    private boolean readTags(BufferedReader in, Game game) throws WrongPGN  {
        boolean result = false;

        while (readTag(in, game)) {
            result = true;
        }

        return result;
    }

    private boolean readMoves(BufferedReader in, Game game) {
        boolean result = false;

        return result;
    }

    private boolean readGame(BufferedReader in) throws WrongPGN  {
        boolean result = false;
        Game game = new Game();

        if (readTags(in, game) && readMoves(in, game)) {
            games.add(game);
            result = true;
        }

        return result;
    }

    public PGNLoader(String fileName) {

        try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));

            try {
                try {

                    while (readGame(in)) ;
                } catch (WrongPGN E) {

                }
            }
            finally {
                in.close();
            }
        }
        catch(IOException E) {
            Log.e(LOGTAG, String.format(LOGTAG + " PGN file not found (%s)", fileName));
        }
    }

    public int getGamesCount() {
        return games.size();
    }
}
