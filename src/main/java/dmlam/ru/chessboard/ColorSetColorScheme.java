package dmlam.ru.chessboard;

import android.graphics.Canvas;

/**
 * Created by LamdanDY on 02.02.2016.
 */
public class ColorSetColorScheme extends ColorScheme {
    private String name;
    private int whiteSquareStart, whiteSquareFinish;                // начальный и конечный цвета градиента для белых клеток
    private int blackSquareStart, blackSquareFinish;                // начальный и конечный цвета градиента для черных клеток
    private int currentMoveSourceSquare, currentMoveTargetSquare;   // цвета для начальной и конечной клетки текущего хода
    private int lastMoveSourceSquare, lastMoveTargetSquare;         // цвета для начальной и конечной клетки последнего сделанного хода
    private int border;                                             // цвет границы
    private int coordinates;                                        // цвет текста для координат

    public ColorSetColorScheme(String name,
                               int whiteSquareStart, int whiteSquareFinish, int blackSquareStart, int blackSquareFinish,
                               int currentMoveSourceSquare, int currentMoveTargetSquare, int lastMoveSourceSquare, int lastMoveTargetSquare,
                               int border, int coordinates) {
        super(name);

        this.name = name;
        this.whiteSquareStart = whiteSquareStart;
        this.whiteSquareFinish = whiteSquareFinish;
        this.blackSquareStart = blackSquareStart;
        this.blackSquareFinish = blackSquareFinish;
        this.currentMoveSourceSquare = currentMoveSourceSquare;
        this.currentMoveTargetSquare = currentMoveTargetSquare;
        this.lastMoveSourceSquare = lastMoveSourceSquare;
        this.lastMoveTargetSquare = lastMoveTargetSquare;
        this.border = border;
        this.coordinates = coordinates;

    }

    public String getName() {
        return name;
    }

    public int getWhiteSquareStart() { return whiteSquareStart; }

    public int getWhiteSquareFinish() { return whiteSquareFinish; }

    public int getBlackSquareStart() { return blackSquareStart; }

    public int getBlackSquareFinish() { return blackSquareFinish; }

    public int getCurrentMoveSourceSquare() { return currentMoveSourceSquare; }

    public int getCurrentMoveTargetSquare() { return currentMoveTargetSquare; }

    public int getLastMoveSourceSquare() { return lastMoveSourceSquare; }

    public int getLastMoveTargetSquare() { return lastMoveTargetSquare; }

    public int getBorder() { return border; }

    public int getCoordinates() { return coordinates; }

    public void drawBoard(ChessBoard board, Canvas canvas) {

    }
}
