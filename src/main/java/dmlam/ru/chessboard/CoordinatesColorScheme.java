package dmlam.ru.chessboard;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

/**
 * Created by LamdanDY on 14.10.2016.
 */

public class CoordinatesColorScheme extends ColorScheme {

    private int border;                                             // цвет границы
    private int coordinates;                                        // цвет текста для координат

    private Paint borderPaint;                  // paint для отрисовки границы доски
    private Paint coordinatesPaint;             // paint для отрисовки координат

    public CoordinatesColorScheme(String name, ChessBoardView chessBoardView, int border, int coordinates) {
        super(name, chessBoardView);

        this.border = border;
        this.coordinates = coordinates;

        borderPaint = new Paint();
        coordinatesPaint = new Paint();

        borderPaint.setColor(Color.BLACK);
        coordinatesPaint.setColor(Color.BLACK);
    }

    public int getBorder() { return border; }

    public int getCoordinates() { return coordinates; }

    @Override
    public int getWhiteColor() {
        return Color.rgb(255, 255, 255);
    }

    @Override
    public int getBlackColor() {
        return Color.rgb(102, 160, 163);
    }

    protected void initPainting() {
        borderPaint.setColor(border);
        coordinatesPaint.setColor(coordinates);

        coordinatesPaint.setTextAlign(Paint.Align.CENTER);
    }

    protected  void updatePainters(float cellSize, float textSize) {
        coordinatesPaint.setTextSize(textSize);
    }

    protected void donePainting() {
        borderPaint = null;
        coordinatesPaint = null;
    }

    private void drawCoordinates(Canvas canvas, boolean doDraw, boolean reverseBoard) {
        float viewWidth = chessBoardView.getWidth(), viewHeight = chessBoardView.getHeight();

        if (doDraw) {
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

    protected void drawBoard(Canvas canvas) {
        boolean reverseBoard = chessBoardView.getReverseBoard();

        drawCoordinates(canvas, chessBoardView.getDrawCoordinates(), reverseBoard);
    }
}
