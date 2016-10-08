package dmlam.ru.chessboard;

import android.graphics.Canvas;

/**
 * Created by LamdanDY on 07.10.2016.
 */

abstract public class ColorScheme {
    private String name;

    public ColorScheme(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    abstract public void drawBoard(ChessBoard board, Canvas canvas);

    abstract public void initPainting();

    abstract public void donePainting();
}
