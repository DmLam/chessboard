package dmlam.ru.chessboard;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;

/**
 * Created by LamdanDY on 02.02.2016.
 */
public class ColorSetColorScheme extends CoordinatesColorScheme {
    private int whiteSquareStart, whiteSquareFinish;                // начальный и конечный цвета градиента для белых клеток
    private int blackSquareStart, blackSquareFinish;                // начальный и конечный цвета градиента для черных клеток
    private int currentMoveSourceSquare, currentMoveTargetSquare;   // цвета для начальной и конечной клетки текущего хода
    private int lastMoveSourceSquare, lastMoveTargetSquare;         // цвета для начальной и конечной клетки последнего сделанного хода

    private Paint whiteSquarePaint;             // paint-ы для отрисовки полей, чтобы не создавать каждый раз в OnDraw
    private Paint blackSquarePaint;
    private Paint currentMoveSourceSquarePaint; // paint для отрисовки клетки с которой взяли фигуру для хода
    private Paint currentMoveTargetSquarePaint; // paint для отрисовки клетки, куда будет поставлена фигура
    private Paint lastMoveSourceSquarePaint;    // paint для отрисовки клетки откуда был сделан последний ход
    private Paint lastMoveTargetSquarePaint;    // paint для отрисовки клетки куда был сделан последний ход

    public ColorSetColorScheme(String name, ChessBoardView chessBoardView,
                               int whiteSquareStart, int whiteSquareFinish, int blackSquareStart, int blackSquareFinish,
                               int currentMoveSourceSquare, int currentMoveTargetSquare, int lastMoveSourceSquare, int lastMoveTargetSquare,
                               int border, int coordinates) {
        super(name, chessBoardView, border, coordinates);

        this.whiteSquareStart = whiteSquareStart;
        this.whiteSquareFinish = whiteSquareFinish;
        this.blackSquareStart = blackSquareStart;
        this.blackSquareFinish = blackSquareFinish;
        this.currentMoveSourceSquare = currentMoveSourceSquare;
        this.currentMoveTargetSquare = currentMoveTargetSquare;
        this.lastMoveSourceSquare = lastMoveSourceSquare;
        this.lastMoveTargetSquare = lastMoveTargetSquare;
    }


    private int getAverageColor(int c1, int c2) {
        int red, green, blue;

        red = (Color.red(c1) + Color.red(c2)) / 2;
        green = (Color.green(c1) + Color.green(c2)) / 2;
        blue = (Color.blue(c1) + Color.blue(c2)) / 2;

        return Color.rgb(red, green, blue);
    }

    @Override
    public int getWhiteColor() {
        return getAverageColor(getWhiteSquareStart(), getWhiteSquareFinish());
    }

    @Override
    public int getBlackColor() {
        return getAverageColor(getBlackSquareStart(), getBlackSquareFinish());
    }

    public int getWhiteSquareStart() { return whiteSquareStart; }

    public int getWhiteSquareFinish() { return whiteSquareFinish; }

    public int getBlackSquareStart() { return blackSquareStart; }

    public int getBlackSquareFinish() { return blackSquareFinish; }

    public int getCurrentMoveSourceSquare() { return currentMoveSourceSquare; }

    public int getCurrentMoveTargetSquare() { return currentMoveTargetSquare; }

    public int getLastMoveSourceSquare() { return lastMoveSourceSquare; }

    public int getLastMoveTargetSquare() { return lastMoveTargetSquare; }



    @Override
    protected void initPainting() {
        super.initPainting();

        whiteSquarePaint = new Paint();
        blackSquarePaint = new Paint();
        currentMoveSourceSquarePaint = new Paint();
        currentMoveTargetSquarePaint = new Paint();
        lastMoveSourceSquarePaint = new Paint();
        lastMoveTargetSquarePaint = new Paint();

        currentMoveSourceSquarePaint.setColor(currentMoveSourceSquare);
        currentMoveTargetSquarePaint.setColor(currentMoveTargetSquare);
        lastMoveSourceSquarePaint.setColor(lastMoveSourceSquare);
        lastMoveTargetSquarePaint.setColor(lastMoveTargetSquare);
    }

    @Override
    protected  void updatePainters(float cellSize, float textSize) {
        super.updatePainters(cellSize, textSize);

        if (whiteSquarePaint != null && blackSquarePaint != null) {
            if (whiteSquareStart == whiteSquareFinish) {
                whiteSquarePaint.setColor(whiteSquareStart);
            } else {
                whiteSquarePaint.setShader(new LinearGradient(0, 0, cellSize - 1, cellSize - 1,
                        whiteSquareStart,
                        whiteSquareFinish,
                        Shader.TileMode.MIRROR));
            }
            if (blackSquareStart == blackSquareFinish) {
                blackSquarePaint.setColor(blackSquareStart);

            } else {
                blackSquarePaint.setShader(new LinearGradient(0, 0, cellSize - 1, cellSize - 1,
                        blackSquareStart,
                        blackSquareFinish,
                        Shader.TileMode.MIRROR));
            }
        }
    }

    @Override
    protected void donePainting() {
        super.donePainting();

        whiteSquarePaint = null;
        blackSquarePaint = null;
        currentMoveSourceSquarePaint = null;
        currentMoveTargetSquarePaint = null;
        lastMoveSourceSquarePaint = null;
        lastMoveTargetSquarePaint = null;
    }

    @Override
    public void drawBoard(Canvas canvas) {
        ChessBoard chessBoard = chessBoardView.getChessBoard();
        boolean reverseBoard = chessBoardView.getReverseBoard();
        ChessBoardView.DraggingData draggingData = chessBoardView.getDraggingData();

        super.drawBoard(canvas);

        for (int x = 0; x <= 7; x++)
            for (int y = 0; y <= 7; y++) {
                Paint paint = null;

                if (chessBoardView.getShowLastMoveSquares()) {
                    Move lastMove = chessBoard.getLastMove();

                    while (lastMove != null && lastMove.isNullMove()) {
                        lastMove = lastMove.getPrevMove();
                    }

                    if (lastMove != null) {
                        if (lastMove.From().equals(reverseBoard ? 7 - x : x, reverseBoard ? y : 7 - y)) {
                            paint = lastMoveSourceSquarePaint;
                        } else if (lastMove.To().equals(reverseBoard ? 7 - x : x, reverseBoard ? y : 7 - y)) {
                            paint = lastMoveTargetSquarePaint;
                        }
                    }
                }

                if (draggingData != null) {
                    int xFrom, yFrom, xTo, yTo;

                    if (draggingData.startSquare != null) {
                        xFrom = draggingData.startSquare.x;
                        yFrom = draggingData.startSquare.y;
                        xTo = draggingData.currentSquare.x;
                        yTo = draggingData.currentSquare.y;
                        if (reverseBoard) {
                            if (7 - x == xFrom && y == yFrom) {
                                paint = currentMoveSourceSquarePaint;
                            }
                        } else {
                            if (x == xFrom && 7 - y == yFrom) {
                                paint = currentMoveSourceSquarePaint;
                            }
                        }
                    }
                    else {
                        xTo = draggingData.endSquare.x;
                        yTo = draggingData.endSquare.y;
                    }

                    if (reverseBoard) {
                        if (7 - x == xTo && y == yTo) {
                            paint = currentMoveTargetSquarePaint;
                        }
                    } else {
                        if (x == xTo && 7 - y == yTo) {
                            paint = currentMoveTargetSquarePaint;
                        }
                    }
                }

                if (paint == null) {
                    paint = (x + y) % 2 == 0 ? whiteSquarePaint : blackSquarePaint;
                }

                canvas.drawRect(chessBoardView.vLines[x], chessBoardView.hLines[y], chessBoardView.vLines[x + 1], chessBoardView.hLines[y + 1], paint);
            }
    }
}
