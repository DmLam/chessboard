package dmlam.ru.chessboard;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;

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

    private Paint whiteSquarePaint;             // paint-ы для отрисовки полей, чтобы не создавать каждый раз в OnDraw
    private Paint blackSquarePaint;
    private Paint currentMoveSourceSquarePaint; // paint для отрисовки клетки с которой взяли фигуру для хода
    private Paint currentMoveTargetSquarePaint; // paint для отрисовки клетки, куда будет поставлена фигура
    private Paint lastMoveSourceSquarePaint;    // paint для отрисовки клетки откуда был сделан последний ход
    private Paint lastMoveTargetSquarePaint;    // paint для отрисовки клетки куда был сделан последний ход
    private Paint borderPaint;                  // paint для отрисовки границы доски
    private Paint coordinatesPaint;             // paint для отрисовки координат

    public ColorSetColorScheme(String name, ChessBoardView chessBoardView,
                               int whiteSquareStart, int whiteSquareFinish, int blackSquareStart, int blackSquareFinish,
                               int currentMoveSourceSquare, int currentMoveTargetSquare, int lastMoveSourceSquare, int lastMoveTargetSquare,
                               int border, int coordinates) {
        super(name, chessBoardView);

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

    protected void initPainting() {
        whiteSquarePaint = new Paint();
        blackSquarePaint = new Paint();
        currentMoveSourceSquarePaint = new Paint();
        currentMoveTargetSquarePaint = new Paint();
        lastMoveSourceSquarePaint = new Paint();
        lastMoveTargetSquarePaint = new Paint();
        borderPaint = new Paint();
        coordinatesPaint = new Paint();

        currentMoveSourceSquarePaint.setColor(currentMoveSourceSquare);
        currentMoveTargetSquarePaint.setColor(currentMoveTargetSquare);
        lastMoveSourceSquarePaint.setColor(lastMoveSourceSquare);
        lastMoveTargetSquarePaint.setColor(lastMoveTargetSquare);
        borderPaint.setColor(border);
        coordinatesPaint.setColor(coordinates);

        coordinatesPaint.setTextAlign(Paint.Align.CENTER);

    }

    protected  void updatePainters(float cellSize, float textSize) {
        if (whiteSquareStart == whiteSquareFinish) {
            whiteSquarePaint.setColor(whiteSquareStart);
        }
        else {
            whiteSquarePaint.setShader(new LinearGradient(0, 0, cellSize - 1, cellSize - 1,
                    whiteSquareStart,
                    whiteSquareFinish,
                    Shader.TileMode.MIRROR));
        }
        blackSquarePaint = new Paint();
        if (blackSquareStart == blackSquareFinish) {
            blackSquarePaint.setColor(blackSquareStart);

        }
        else {
            blackSquarePaint.setShader(new LinearGradient(0, 0, cellSize - 1, cellSize - 1,
                    blackSquareStart,
                    blackSquareFinish,
                    Shader.TileMode.MIRROR));
        }

        coordinatesPaint.setTextSize(textSize);
    }

    protected void donePainting() {
        whiteSquarePaint = null;
        blackSquarePaint = null;
        currentMoveSourceSquarePaint = null;
        currentMoveTargetSquarePaint = null;
        lastMoveSourceSquarePaint = null;
        lastMoveTargetSquarePaint = null;
        borderPaint = null;
        coordinatesPaint = null;
    }

    private void drawCoordinates(Canvas canvas, boolean doDraw, boolean reverseBoard) {
        float viewWidth = chessBoardView.getWidth(), viewHeight = chessBoardView.getHeight();

        if ( doDraw) {
            float char_width = coordinatesPaint.measureText("8", 0, 1);
            Rect boardRect = new Rect();
            Rect clipRect = canvas.getClipBounds();

            if (clipRect.left < chessBoardView.vLines[0] || clipRect.right > chessBoardView.vLines[8] ||
                    clipRect.top < chessBoardView.hLines[0] || clipRect.bottom > chessBoardView.hLines[8]) {

                // бордюр (где будут написаны координаты)
                boardRect.left = (int) chessBoardView.getGapLeft();
                boardRect.top = (int) chessBoardView.getGapTop();
                boardRect.right = (int) (viewWidth - chessBoardView.getGapLeft());
                boardRect.bottom = (int) (viewHeight - chessBoardView.getGapTop());
                canvas.drawRect(boardRect, borderPaint);

                // номера горизонталей
                for (int i = 1; i <= 8; i++) {
                    String Num = Integer.toString(reverseBoard ? i : 9 - i);
                    canvas.drawText(Num, chessBoardView.vLines[0] - char_width, chessBoardView.hLines[0] +
                            (chessBoardView.hLines[i - 1] + chessBoardView.hLines[i]) / 2 - coordinatesPaint.getTextSize(), coordinatesPaint);
                    canvas.drawText(Num, chessBoardView.vLines[8] + char_width, chessBoardView.hLines[0] +
                            (chessBoardView.hLines[i - 1] + chessBoardView.hLines[i]) / 2 - coordinatesPaint.getTextSize(), coordinatesPaint);
                }

                // буквы вертикалей
                for (int i = 0; i < 8; i++) {
                    String letter = Character.toString(ChessBoard.getLetter(reverseBoard ? 7 - i : i));
                    canvas.drawText(letter, (chessBoardView.vLines[i + 1] + chessBoardView.vLines[i]) / 2,
                            (chessBoardView.hLines[0] + coordinatesPaint.getTextSize()) / 2,
                            coordinatesPaint);
                    canvas.drawText(letter, (chessBoardView.vLines[i + 1] + chessBoardView.vLines[i]) / 2,
                            chessBoardView.hLines[8] + coordinatesPaint.getTextSize(),
                            coordinatesPaint);
                }
            }
        }
        else {
            Rect borderLine = new Rect();

            borderLine.left = (int) chessBoardView.getGapLeft();
            borderLine.top = (int) chessBoardView.getGapTop();
            borderLine.right = (int) (viewWidth - chessBoardView.getGapLeft());
            borderLine.bottom = (int) (viewHeight - chessBoardView.getGapTop());

            canvas.drawRect(borderLine, borderPaint);
        }
    }
    public void drawBoard(Canvas canvas) {
        ChessBoard chessBoard = chessBoardView.getChessBoard();
        boolean reverseBoard = chessBoardView.getReverseBoard();
        ChessBoardView.DraggingData draggingData = chessBoardView.getDraggingData();
        Paint paint = null;

        drawCoordinates(canvas, chessBoardView.getDrawCoordinates(), reverseBoard);

        for (int x = 0; x <= 7; x++)
            for (int y = 0; y <= 7; y++) {
                Piece piece = reverseBoard ? chessBoard.getPiece(7 - x, y) : chessBoard.getPiece(x, 7 - y);

                if (chessBoardView.getShowLastMoveSquares()) {
                    Move lastMove = chessBoard.getLastMove();

                    if (lastMove != null) {
                        if (lastMove.From().equals(reverseBoard ? 7 - x : x, reverseBoard ? y : 7 - y)) {
                            paint = lastMoveSourceSquarePaint;
                        } else if (lastMove.To().equals(reverseBoard ? 7 - x : x, reverseBoard ? y : 7 - y)) {
                            paint = lastMoveTargetSquarePaint;
                        }
                    }
                }

                if (draggingData != null) {
                    if (reverseBoard) {
                        if (7 - x == draggingData.startSquare.x && y == draggingData.startSquare.y) {
                            paint = currentMoveSourceSquarePaint;
                        } else if (7 - x == draggingData.currentSquare.x && y == draggingData.currentSquare.y) {
                            paint = currentMoveTargetSquarePaint;
                        }
                    } else {
                        if (x == draggingData.startSquare.x && 7 - y == draggingData.startSquare.y) {
                            paint = currentMoveSourceSquarePaint;
                        } else if (x == draggingData.currentSquare.x && 7 - y == draggingData.currentSquare.y) {
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
