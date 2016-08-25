package dmlam.ru.chessboard;

import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by LamdanDY on 25.08.2016.
 */
public class PGNLoader {
    private static final String LOGTAG = PGNLoader.class.getName();

    private int gamesCount = 0;

    private boolean readGame(BufferedReader in) {
        boolean result = false;
        String line = null;

        try {
            do {
                line = in.readLine();
                if (line != null) {
                    line = line.trim();
                }
            }
            while (line != null);
        }
        catch(IOException E) {
            Log.e(LOGTAG, String.format(LOGTAG + " Error reading pgn file : %s", E.toString()));
        }

        return result;
    }

    public PGNLoader(String fileName) {

        try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));

            while(readGame(in)) ;

            in.close();
        }
        catch(IOException E) {
            Log.e(LOGTAG, String.format(LOGTAG + " PGN file not found (%s)", fileName));
        }
    }

    public int getGamesCount() {
        return gamesCount;
    }
}
