package dmlam.ru.chessboard;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

/**
 * Created by LamdanDY on 12.10.2016.
 */

public class ImageColorScheme extends CoordinatesColorScheme {

    Bitmap image = null;
    private int imageResourceId;
    private int currentMoveSourceSquare, currentMoveTargetSquare;   // цвета для начальной и конечной клетки текущего хода
    private int lastMoveSourceSquare, lastMoveTargetSquare;         // цвета для начальной и конечной клетки последнего сделанного хода

    private Paint currentMoveSourceSquarePaint; // paint для отрисовки клетки с которой взяли фигуру для хода
    private Paint currentMoveTargetSquarePaint; // paint для отрисовки клетки, куда будет поставлена фигура
    private Paint lastMoveSourceSquarePaint;    // paint для отрисовки клетки откуда был сделан последний ход
    private Paint lastMoveTargetSquarePaint;    // paint для отрисовки клетки куда был сделан последний ход

    private int squareSize;                     // размер клетки
    private Rect imageRect;

    public ImageColorScheme(String name, ChessBoardView chessBoardView,
                            int imageResourceId,
                            int currentMoveSourceSquare, int currentMoveTargetSquare, int lastMoveSourceSquare, int lastMoveTargetSquare,
                            int border, int coordinates) {
        super(name, chessBoardView, border, coordinates);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(chessBoardView.getContext().getResources(), imageResourceId, options);

        if ((options.outWidth % 8 != 0) || (options.outHeight % 8 != 0)) {
            throw new RuntimeException(String.format("Board image width (%d) and height (%d) should be divisible by 8", options.outWidth, options.outHeight));
        }
        if (options.outWidth != options.outHeight) {
            throw new RuntimeException(String.format("Board image width (%d) and height (%d) should be equal", options.outWidth, options.outHeight));
        }

        this.currentMoveSourceSquare = currentMoveSourceSquare;
        this.currentMoveTargetSquare = currentMoveTargetSquare;
        this.lastMoveSourceSquare = lastMoveSourceSquare;
        this.lastMoveTargetSquare = lastMoveTargetSquare;
        this.imageResourceId = imageResourceId;
    }

    public int getCurrentMoveSourceSquare() { return currentMoveSourceSquare; }

    public int getCurrentMoveTargetSquare() { return currentMoveTargetSquare; }

    public int getLastMoveSourceSquare() { return lastMoveSourceSquare; }

    public int getLastMoveTargetSquare() { return lastMoveTargetSquare; }

    private int getAverageColor(Rect r) {
        int redBucket = 0;
        int greenBucket = 0;
        int blueBucket = 0;
        int pixelCount = 0;

        for (int y = r.top; y < r.bottom; y++)
        {
            for (int x = r.left; x < r.right; x++)
            {
                int c = image.getPixel(x, y);

                pixelCount++;
                redBucket += Color.red(c);
                greenBucket += Color.green(c);
                blueBucket += Color.blue(c);
            }
        }

        return Color.rgb(redBucket / pixelCount, greenBucket / pixelCount, blueBucket / pixelCount);
    }

    @Override
    public int getWhiteColor() {
        return getAverageColor(new Rect(0, 0, image.getWidth() / 8, image.getWidth() / 8));
    }

    @Override
    public int getBlackColor() {
        return getAverageColor(new Rect(image.getWidth() / 8, 0, image.getWidth() / 4, image.getWidth() / 8));
    }

    @Override
    protected void drawBoard(Canvas canvas) {
        ChessBoard chessBoard = chessBoardView.getChessBoard();
        boolean reverseBoard = chessBoardView.getReverseBoard();
        ChessBoardView.DraggingData draggingData = chessBoardView.getDraggingData();

        super.drawBoard(canvas);

        canvas.drawBitmap(image, null, imageRect, null);

        if (chessBoardView.getShowLastMoveSquares()) {
            Move lastMove = chessBoard.getLastMove();

            if (lastMove != null) {
                int xFrom = lastMove.From().x, yFrom = lastMove.From().y, xTo = lastMove.To().x, yTo = lastMove.To().y;

                if (reverseBoard) {
                    xFrom = 7 - xFrom;
                    xTo = 7 - xTo;
                }
                else {
                    yFrom = 7 - yFrom;
                    yTo = 7 - yTo;
                }

                canvas.drawRect(chessBoardView.vLines[xFrom], chessBoardView.hLines[yFrom], chessBoardView.vLines[xFrom + 1], chessBoardView.hLines[yFrom + 1], lastMoveSourceSquarePaint);
                canvas.drawRect(chessBoardView.vLines[xTo], chessBoardView.hLines[yTo], chessBoardView.vLines[xTo + 1], chessBoardView.hLines[yTo + 1], lastMoveTargetSquarePaint);
            }
        }

        if (draggingData != null) {
            int xFrom, xTo, yFrom, yTo;

            if (draggingData.startSquare != null) {
                // отрисовываем начальную клетку хода
                xFrom = draggingData.startSquare.x;
                yFrom = draggingData.startSquare.y;

                if (reverseBoard) {
                    xFrom = 7 - xFrom;
                }
                else {
                    yFrom = 7 - yFrom;
                }

                canvas.drawRect(chessBoardView.vLines[xFrom], chessBoardView.hLines[yFrom], chessBoardView.vLines[xFrom + 1], chessBoardView.hLines[yFrom + 1], currentMoveSourceSquarePaint);
            }

            // отрисовываем текущую (или конечную) клетку хода
            if (draggingData.startSquare != null) {
                // ход делается указанием сначала начальной клетки потом конечной
                xTo = draggingData.currentSquare.x;
                yTo = draggingData.currentSquare.y;
            }
            else {
                // ход делается указанием сначала конечной клетки потом начальной
                xTo = draggingData.endSquare.x;
                yTo = draggingData.endSquare.y;
            }

            if (reverseBoard) {
                xTo = 7 - xTo;
            }
            else {
                yTo = 7 - yTo;
            }

            canvas.drawRect(chessBoardView.vLines[xTo], chessBoardView.hLines[yTo], chessBoardView.vLines[xTo + 1], chessBoardView.hLines[yTo + 1], currentMoveTargetSquarePaint);
        }
    }

    @Override
    protected void initPainting() {
        super.initPainting();

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        image = BitmapFactory.decodeResource(chessBoardView.getContext().getResources(), imageResourceId, options);
        squareSize = image.getWidth() / 8;

        currentMoveSourceSquarePaint = new Paint();
        currentMoveTargetSquarePaint = new Paint();
        lastMoveSourceSquarePaint = new Paint();
        lastMoveTargetSquarePaint = new Paint();

        currentMoveSourceSquarePaint.setColor(currentMoveSourceSquare);
        currentMoveTargetSquarePaint.setColor(currentMoveTargetSquare);
        lastMoveSourceSquarePaint.setColor(lastMoveSourceSquare);
        lastMoveTargetSquarePaint.setColor(lastMoveTargetSquare);
    };

    @Override
    protected void donePainting() {
        super.donePainting();

        currentMoveSourceSquarePaint = null;
        currentMoveTargetSquarePaint = null;
        lastMoveSourceSquarePaint = null;
        lastMoveTargetSquarePaint = null;
        image = null;
    };

    @Override
    protected void updatePainters(float cellSize, float textSize){
        super.updatePainters(cellSize, textSize);

        imageRect = new Rect((int) chessBoardView.vLines[0], (int) chessBoardView.hLines[0], (int) chessBoardView.vLines[8], (int) chessBoardView.hLines[8]);
    };

}
