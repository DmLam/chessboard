package dmlam.ru.chessboard;

import android.graphics.Canvas;

/**
 * Created by LamdanDY on 07.10.2016.
 */

abstract class ColorScheme {
    private String name;
    protected ChessBoardView chessBoardView;

    public ColorScheme(String name, ChessBoardView chessBoardView) {
        this.chessBoardView = chessBoardView;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    abstract protected void drawBoard(Canvas canvas);

    abstract protected void initPainting();

    abstract protected void donePainting();

    abstract protected  void updatePainters(float cellSize, float textSize);
}
