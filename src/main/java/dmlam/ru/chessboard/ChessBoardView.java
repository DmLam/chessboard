package dmlam.ru.chessboard;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.ShowableListMenu;
import androidx.appcompat.widget.ForwardingListener;
import androidx.appcompat.widget.PopupMenu;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListPopupWindow;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import dmlam.ru.chessboard.Piece.Color;
import dmlam.ru.androidcommonlib.ClipboardUtils;

import static dmlam.ru.chessboard.Piece.Color.BLACK;
import static dmlam.ru.chessboard.Piece.Color.WHITE;
import static dmlam.ru.chessboard.Piece.Color.valueOf;
import static java.lang.Math.min;
import static java.lang.Math.sqrt;

//import android.support.annotation.NonNull;

/**
 * Created by Lam on 12.06.2015.
 *
 * Класс-наследник View, отображающий шахматную доску
 *
 */

public class ChessBoardView extends View implements SelectPawnTransformationDialogFragment.SelectPawnTransformationDialogListener, ChessBoard.OnNeedPieceForTransformation,
        IOnMoveListener{
    private static final String LOGTAG = ChessBoardView.class.getName();

    private final int LONG_PRESS_DELAY = 800; // минимальная длительность длинного нажатия для вызова меню (мс.)
    private final int LONG_PRESS_MAX_SHIFT = 20; // максимальное расстояние точки отпускания от точки нажатия для срабатывания длительного нажатия
    private final int MOVE_ANIMATION_TICK_COUNT = 50;
    private ChessBoard chessBoard = new ChessBoard();
    private String pieceSetName = "Classic";

    private boolean showLastMoveSquares = true;
    private boolean reverseBoard = false;
    private boolean drawCoordinates = true;

    private ArrayList<IOnMoveListener> mOnMoveListeners = new ArrayList<>();
    private Paint antialiasPaint = new Paint();               // paint для гладкого масштабирования фигур
    private Paint[] arrowPaints;                              // paint-ы для рисования стрелки
    protected float[] hLines = new float[9], vLines = new float[9];

    private float cellSize;
    private float size, borderSize, gapLeft, gapTop;
    // константы для фигур в массиве
    private final static int WK = 0, WQ = 1, WR = 2, WB = 3, WN = 4, WP = 5, BK = 6, BQ = 7, BR = 8, BB = 9, BN = 10, BP = 11;
    private Bitmap[] bitmaps = new Bitmap[12];
    private DraggingData draggingData = null;
    private Timer animationTimer = null;
    private Move animatingMove = null;

    private ColorSchemeList colorSchemes;
    private PieceSetList pieceSets;
    private ColorScheme colorScheme;
    private boolean soundEnabled = true;
    private boolean allowTargetSquareFist = false;

    // признак того, что мы находимся в процессе выбора фигура для превращения пешки (показан диалог).
    // используется для правильного восстановления диалога при повороте экрана
    private boolean selectingPawnTransformation = false, selectPawnTransformationOnStart = false;
    private Point pawnTransformationSourceSquare = null;
    private Color transformingPawnColor;
    private PointF pressDownXY = new PointF(-1, -1);  // точка, где было нажатие. Нужна чтобы определять при длинном нажатии совпадение координат нажатия и отпускания
                                                      // нельзя использовать draggingData.startPoint, т.к. лонгтап может быть и не на фигуре, а на пустой клетке
    private int longPressMaxShift = 0;                // Максимальное смещение после нажатия до отпускания. Нужно для определения long press

    // стрелка для анализа
    private int maxArrowCount = 0;
    private int[] arrowColors = null;
    private String[] arrowsCoordinates = null;

    public boolean isAllowTargetSquareFist() {
        return allowTargetSquareFist;
    }

    public void setAllowTargetSquareFist(boolean allowTargetSquareFist) {
        this.allowTargetSquareFist = allowTargetSquareFist;
    }

    // класс для хранения данных о перетаскиваемой фигуре во время совершения хода перетаскиванием
    class DraggingData {
        public Piece piece = null;
        public Point startSquare = null, currentSquare = null, endSquare = null;
        public PointF startPoint = null, currentPoint = null;
        public boolean dragging = false;      // в данный момент фигура перетаскивается (ACTION_DOWN была, а ACTION_UP не было)
        public boolean animating = false;
        public PointF animateToPoint = null;  // если View находится в режиме анимации хода, то в animateTo хранится координаты куда фигура должна прибыть
        public Point animateToSquare = null;
        public int animateIndex = 0;     // в режиме анимации индекс текущего положения анимации

        public DraggingData(Piece piece, Point startSquare) {
            this.piece = piece;
            this.startSquare = startSquare;
            this.currentSquare = new Point(startSquare);
            if (!reverseBoard) {
                this.startPoint = new PointF((vLines[startSquare.x] + vLines[startSquare.x + 1]) / 2, (hLines[7 - startSquare.y] + hLines[7 - startSquare.y + 1]) / 2);
            }
            else {
                this.startPoint = new PointF((vLines[7 - startSquare.x] + vLines[7 - startSquare.x + 1]) / 2, (hLines[startSquare.y] + hLines[startSquare.y + 1]) / 2);
            }
            this.currentPoint = new PointF(startPoint.x, startPoint.y);
        }

        public DraggingData(Piece piece, Point startSquare, PointF startPoint) {
            this.piece = piece;
            this.startSquare = startSquare;
            this.endSquare = endSquare;
            this.startPoint = startPoint;
            this.currentSquare = new Point(startSquare);
            this.currentPoint = new PointF(startPoint.x, startPoint.y);
        }

        public DraggingData(Point endSquare) {
            this.endSquare = endSquare;
        }

        public void animateTo(int x, int y) {
            animating = true;
            animateToSquare = new Point(x, y);
            if (!reverseBoard) {
                animateToPoint = new PointF((vLines[x] + vLines[x + 1]) / 2, (hLines[7 - y] + hLines[7 - y + 1]) / 2);
            }
            else {
                animateToPoint = new PointF((vLines[7 - x] + vLines[7 - x + 1]) / 2, (hLines[y] + hLines[y + 1]) / 2);
            }
        }

        public boolean startedByFirstSquare() {
            return startSquare != null;
        }

        public boolean startedByLastSquare() {
            return endSquare != null;
        }
    }

    private void initialize(Context context, AttributeSet attrs, int defStyle) {

        if (!isInEditMode()) {
            arrowColors = getResources().getIntArray(R.array.arrow_colors);
            maxArrowCount = arrowColors.length;
            arrowsCoordinates = new String[maxArrowCount];
            arrowPaints = new Paint[maxArrowCount];
            for (int i = 0; i < maxArrowCount; i++) {
                arrowPaints[i] = new Paint();
            }

            colorSchemes = new ColorSchemeList(this, getResources().getStringArray(R.array.color_schemes));
            pieceSets = new PieceSetList(getResources().getStringArray(R.array.piecesets));

            setColorScheme(colorSchemes.get(0));  // цветовая схема по умолчанию - первая в списке
            antialiasPaint.setFilterBitmap(true);
            antialiasPaint.setAntiAlias(true);
            antialiasPaint.setDither(true);

            chessBoard.addOnMoveListener(this);
            chessBoard.setOnNeedPieceForTransformation(this);

            loadPieceSet();

            chessBoard.SetupInitialPosition();
        }
        else {
            colorSchemes = new ColorSchemeList(this);
            colorSchemes.add(
                    new ColorSetColorScheme("Cadet grey", this,
                            0xfffdfdfd, 0xfffdfdfd,
                            0xff7795a3, 0xff9aabaf,
                            0xff444444, 0xff888888,
                            0xff9f9fff, 0xf557fff,
                            0xff363a3d, 0xffcccccc));
            colorScheme = colorSchemes.get(0);
            colorScheme.initPainting();
        }

    }

    public String[] getColorSchemeNames() {
        return colorSchemes.getNames();
    }

    public String getColorSchemeName() {
        if (colorScheme == null) {
            return null;
        }
        else {
            return colorScheme.getName();
        }
    }

    public ColorScheme getColorScheme() {
        return colorScheme;
    }

    private void setColorScheme(ColorScheme colorScheme) {
        if (this.colorScheme != null) {
            this.colorScheme.donePainting();
        }

        this.colorScheme = colorScheme;

        if (this.colorScheme != null) {
            this.colorScheme.initPainting();
        }
    }

    public void setColorScheme(String colorSchemeName) {
        ColorScheme cs = colorSchemes.find(colorSchemeName);

        if (cs == null) {
            throw new RuntimeException(String.format("Color scheme %s not found", colorSchemeName));
        }

        setColorScheme(cs);
        updateBoardPainters();
        invalidate();
    }

    public String getPieceSet() {
        return pieceSetName;
    }

    public void setPieceSet(String pieceSetName) {
        if (!this.pieceSetName.equals(pieceSetName)) {
            this.pieceSetName = pieceSetName;

            loadPieceSet();
        }
    }

    public String[] getPieceSetNames() {
        return pieceSets.getNames();
    }

    public String[] getPieceSetValues() {
        return pieceSets.getValues();
    }

    protected DraggingData getDraggingData() {
        return draggingData;
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
    }

    public void pauseAnimation() {
        if (animationTimer != null) {
            animationTimer.cancel();
            animationTimer = null;
        }
    }

    public ChessBoardView(Context context) {
        super(context);

        initialize(context, null, 0);
    }

    public ChessBoardView (Context context, AttributeSet attrs) {
        super(context, attrs);

        initialize(context, attrs, 0);
    }

    public ChessBoardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        initialize(context, attrs, defStyle);
    }

    public void addOnMoveListener(IOnMoveListener l) {
        mOnMoveListeners.add(l);
    }

    private boolean doOnMove(Move move) {
        boolean result = true;
        for (int i = 0; i < mOnMoveListeners.size(); i++) {
            result &= mOnMoveListeners.get(i).onMove(move);
        }

        return result;
    }

    private void doOnRollback(Move move) {
        for (int i = 0; i < mOnMoveListeners.size(); i++) {
            mOnMoveListeners.get(i).onRollback(move);
        }
    }

    private void doOnRollup(Move move) {
        for (int i = 0; i < mOnMoveListeners.size(); i++) {
            mOnMoveListeners.get(i).onRollup(move);
        }
    }

    private void doOnGoto(Move move) {
        for (int i = 0; i < mOnMoveListeners.size(); i++) {
            mOnMoveListeners.get(i).onGoto(move);
        }
    }

    private Point getPointFromSquareName(String square) {
        Point p = chessBoard.getPointFromSquareName(square);

        return new Point(reverseBoard ? 7 - p.x : p.x, reverseBoard ? p.y : 7 - p.y);
    }

    public void setShowLastMoveSquares(boolean showLastMoveSourceSquare) {
        this.showLastMoveSquares = showLastMoveSourceSquare;
    }

    public boolean getShowLastMoveSquares() {
        return showLastMoveSquares;
    }

    public boolean getDrawCoordinates() {
        return drawCoordinates;
    }

    public void setDrawCoordinates(boolean drawCoordinates) {
        if (drawCoordinates != this.drawCoordinates) {
            this.drawCoordinates = drawCoordinates;
            setBounds();
            invalidate();
        }
    }

    // размер квадрата доски
    public float getSize() {
        return size;
    }

    public float getBorderSize() {
        return borderSize;
    }

    // отсуп доски слева (для координат)
    public float getGapLeft() {
        return gapLeft;
    }

    // отступ доски сверху (для координат)
    public float getGapTop() {
        return gapTop;
    }

    public float getCellSize() {
        return cellSize;
    }

    public void setReverseBoard(boolean reverseBoard) {
        this.reverseBoard = reverseBoard;

        invalidate();
    }

    public boolean getReverseBoard() {
        return reverseBoard;
    }

    // рассчитываем высоту и ширину так, чтобы View был квадратным, сторона квадрата - минимум из высоты и ширины
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int w = getMeasuredWidth(), h = getMeasuredHeight();
        int size;
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthMode == MeasureSpec.UNSPECIFIED && heightMode == MeasureSpec.EXACTLY) {
            size = Math.min(h, heightSize);
        }
        else
        if (widthMode == MeasureSpec.UNSPECIFIED && heightMode == MeasureSpec.AT_MOST) {
            size = h;
        }
        else
        if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.UNSPECIFIED) {
            size = Math.min(widthSize, w);
        }
        else
        if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
            size = Math.min(widthSize, heightSize);
        }
        else
        if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.AT_MOST) {
            size = Math.min(h, heightSize);
        }
        else
        if (widthMode == MeasureSpec.AT_MOST && heightMode == MeasureSpec.UNSPECIFIED) {
            size = Math.max(w, widthSize);
        }
        else
        if (widthMode == MeasureSpec.AT_MOST && heightMode == MeasureSpec.EXACTLY) {
            size = heightSize;
        }
        else
        if (widthMode == MeasureSpec.AT_MOST && heightMode == MeasureSpec.AT_MOST) {
            size = Math.min(widthSize, heightSize);
        }
        else {
            //  widthMode == MeasureSpec.UNSPECIFIED && heightMode == MeasureSpec.UNSPECIFIED
            size = Math.min(w, h);
        }

        setMeasuredDimension(size, size);
    }

    public Bitmap getBitmap(Piece.Kind kind, Color color) {
        int index;

        switch (kind) {
            case KING:
                index = 0;
                break;
            case QUEEN:
                index = 1;
                break;
            case ROOK:
                index = 2;
                break;
            case BISHOP:
                index = 3;
                break;
            case KNIGHT:
                index = 4;
                break;
            case PAWN:
                index = 5;
                break;
            default:
                index = -100;
        }

        if (color == BLACK) {
            index += 6;
        }

        return bitmaps[index];
    }

    private Bitmap loadPiece(Resources resources, String prefix, String piece) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap result = BitmapFactory.decodeResource(resources, resources.getIdentifier(prefix + piece, "drawable", getContext().getPackageName()), options);

        if (result == null) {
            throw new RuntimeException(String.format("Can't load piece %s from piece set %s", piece, pieceSetName));
        }

        return result;
    }

    private void loadPieceSet() {
        Resources resources = getContext().getResources();
        String prefix = pieceSets.find(pieceSetName).getPrefix();

        bitmaps[WK] = loadPiece(resources, prefix, "wk");
        bitmaps[WQ] = loadPiece(resources, prefix, "wq");
        bitmaps[WR] = loadPiece(resources, prefix, "wr");
        bitmaps[WB] = loadPiece(resources, prefix, "wb");
        bitmaps[WN] = loadPiece(resources, prefix, "wn");
        bitmaps[WP] = loadPiece(resources, prefix, "wp");
        bitmaps[BK] = loadPiece(resources, prefix, "bk");
        bitmaps[BQ] = loadPiece(resources, prefix, "bq");
        bitmaps[BR] = loadPiece(resources, prefix, "br");
        bitmaps[BB] = loadPiece(resources, prefix, "bb");
        bitmaps[BN] = loadPiece(resources, prefix, "bn");
        bitmaps[BP] = loadPiece(resources, prefix, "bp");

        invalidate();
    }

    public ChessBoard getChessBoard(){
        return chessBoard;
    }

    public void setChessBoard(ChessBoard board) {
        if (chessBoard != board) {
            if (board == null) {
                chessBoard.deleteOnMoveListener(this);
                chessBoard.setOnNeedPieceForTransformation(null);
            } else {
                board.addOnMoveListener(this);
                board.setOnNeedPieceForTransformation(this);
                invalidate();
            }
            chessBoard = board;
        }
    }

    public void onBoardChange() {
        if (draggingData == null || !(draggingData.animating || draggingData.dragging))
        {
            mouseUp();
            invalidate();
        }
    }

    private Point getBoardCoordinatesFromScreen(float x, float y) {
        Point result = null;

        if (x >= vLines[0] && x < vLines[8] && y >= hLines[0] && y < hLines[8]) {
            result = new Point(0, 0);

            result.x = 0;
            while(x >= vLines[result.x + 1]) {
                result.x++;
            }
            result.y = 0;
            while(y >= hLines[result.y + 1]) {
                result.y++;
            }
            result.y = 7 - result.y;

            if (reverseBoard) {
                result.x = 7 - result.x;
                result.y = 7 - result.y;
            }
        }

        return result;
    }

    private void setBounds() {
        float w = getWidth(), h = getHeight();

        if (drawCoordinates) {
            borderSize = min(w, h) / 10;
            size = borderSize * 9;
            borderSize /= 2;
        }
        else {
            borderSize = 1;
            size = min(w, h) - 2;
        }
        gapLeft = (w - size - borderSize * 2) / 2;
        gapTop = (h - size - borderSize * 2) / 2;

        cellSize = size / 8;

        vLines[0] = gapLeft + borderSize;
        hLines[0] = gapTop + borderSize;

        for (int i = 1; i <= 8; i++){
            vLines[i] = vLines[i - 1] + cellSize;
        }
        for (int i = 1; i <= 8; i++){
            hLines[i] = hLines[i - 1] + cellSize;
        }

        updateBoardPainters();
    }

    private void updateBoardPainters() {
        colorScheme.updatePainters(cellSize, min(hLines[0], vLines[0]) * 7 / 10);

        for (int i = 0; i < maxArrowCount; i++) {
            arrowPaints[i].setColor(arrowColors[i]);
            arrowPaints[i].setAntiAlias(true);
        }
    }

    @Override
    protected void onLayout (boolean changed, int left, int top, int right, int bottom) {
        setBounds();

        // если view находится в процессе восстановления активности после смены конфигурации устройства (поворота), то необходимо проверить, не произошла ли
        // смена конфигурации в момент когда был активен диалог выбора фигуры в которую должна трансформироваться пешка
        // Если была - покажем диалог снова с сохраненными условиями. Сделать это можно только здесь, т.к. здесь расчитываются размеры клеток, которые нужны для отображения фигур в диалоге
        if (selectPawnTransformationOnStart) {
            selectPromotedPawnTransformation(pawnTransformationSourceSquare, transformingPawnColor);
        }
    }

    public void onNeedPieceForTransformation(Point sourceSquare) {
        if (animatingMove != null) {
            ChessBoard.PromoteTo promotion = animatingMove.getPromotePawnTo();

            if (promotion != null) {
                chessBoard.promotePawnTo(sourceSquare, promotion, false);
            }
            else {
                throw new RuntimeException("Can't promote pawn at animated move");
            }
        }
        else {
            selectPromotedPawnTransformation(sourceSquare, chessBoard.getMoveOrder());
        }
    }

    public void onSelectPawnTransformation(ChessBoard.PromoteTo result) {
        selectingPawnTransformation = false;

        if (chessBoard.promotePawnTo(pawnTransformationSourceSquare, result, false)) {
            pawnTransformationSourceSquare = null;
            transformingPawnColor = null;
        }
        else {
            invalidate();
        }
    }


    private void selectPromotedPawnTransformation(Point sourceSquare, Color color) {
        if (!selectingPawnTransformation) {
            SelectPawnTransformationDialogFragment selectPromotiondialog = new SelectPawnTransformationDialogFragment();
            Bundle args = new Bundle();

            selectingPawnTransformation = true;
            pawnTransformationSourceSquare = sourceSquare;  // сохраним sourceSquare чтоб не передавать его в диалог и обратно.
            transformingPawnColor = color;

            args.putInt(SelectPawnTransformationDialogFragment.SPT_IMAGESIZE, (int) (vLines[1] - vLines[0]) * 2);
            args.putParcelable(SelectPawnTransformationDialogFragment.SPT_QUEEN, bitmaps[color == WHITE ? WQ : BQ]);
            args.putParcelable(SelectPawnTransformationDialogFragment.SPT_ROOK, bitmaps[color == WHITE ? WR : BR]);
            args.putParcelable(SelectPawnTransformationDialogFragment.SPT_BISHOP, bitmaps[color == WHITE ? WB : BB]);
            args.putParcelable(SelectPawnTransformationDialogFragment.SPT_KNIGHT, bitmaps[color == WHITE ? WN : BN]);

            selectPromotiondialog.setSelectPawnTransformationDialogListener(this);
            selectPromotiondialog.setArguments(args);
            selectPromotiondialog.setCancelable(false);
            selectPromotiondialog.show(((AppCompatActivity) getContext()).getSupportFragmentManager(), "");
        }
    }

    static class SavedState extends BaseSavedState {
        private boolean selectingPawnTransformation;
        private Point sourceSquare;
        private Color color;

        SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);

            out.writeByte((byte) (this.selectingPawnTransformation ? 1 : 0));
            if (this.selectingPawnTransformation) {
                out.writeParcelable(sourceSquare, 0);
                out.writeString(color.name());
            }
        }

        private SavedState(Parcel in) {
            super(in);

            this.selectingPawnTransformation = in.readByte() != 0;
            if (this.selectingPawnTransformation) {
                sourceSquare = in.readParcelable(Point.class.getClassLoader());
                color = valueOf(in.readString());
            }
        }

        //required field that makes Parcelables from a Parcel
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }

    @Override
    protected Parcelable onSaveInstanceState () {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.selectingPawnTransformation = this.selectingPawnTransformation;
        ss.sourceSquare = this.pawnTransformationSourceSquare;
        ss.color = this.transformingPawnColor;

        return ss;
    }

    @Override
    protected void onRestoreInstanceState (Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        selectPawnTransformationOnStart = ss.selectingPawnTransformation;
        pawnTransformationSourceSquare = ss.sourceSquare;
        transformingPawnColor = ss.color;
    }


    private void makeMove() {
        Piece piece = draggingData.piece;
        Point source = draggingData.startSquare, target = draggingData.endSquare;

        if (chessBoard.isMovePossible(draggingData.piece, draggingData.endSquare)) {

            draggingData = null;    // необходимо сначала обнулить draggingData, т.к. в moveTo будет вызван OnMove и может быть, например,
                                    // запущена анимация хода компьютера, которая использует draggingData
            chessBoard.movePieceTo(piece, target);
        }
        else {
            chessBoard.setPieceAt(source, piece);
            draggingData = null;
        }
    }

    private void copyFENtoClipboard() {
        String FEN = getChessBoard().saveToFEN();

        ClipboardUtils.copyTextToClipboard(getContext(), "FEN", FEN);
    }

    private void showPopupMenu(int x, int y) {
        PopupMenu popupMenu = new PopupMenu(getContext(), this);

        popupMenu.inflate(R.menu.board_local_menu);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.action_copy_fen) {
                    copyFENtoClipboard();
                    return true;
                }
                else {
                    return false;
                }
            }
        });

        popupMenu.show();

        View.OnTouchListener dragToOpenListener = popupMenu.getDragToOpenListener();

        // сдвинем меню в центр доски
        if (dragToOpenListener instanceof ForwardingListener)
        {
            ShowableListMenu popup = ((ForwardingListener) dragToOpenListener).getPopup();

            if (popup instanceof ListPopupWindow) {
                ListPopupWindow listPopup = (ListPopupWindow) popup;
                listPopup.setHorizontalOffset(x);
                listPopup.setVerticalOffset(-(getHeight() - y));
                listPopup.show();
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        if (!isEnabled()) {
            return false;
        }


        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mouseDown(event.getX(), event.getY());
                break;

            case MotionEvent.ACTION_MOVE:
                mouseMove(event.getX(), event.getY());
                break;

            case MotionEvent.ACTION_UP:
                long eventDuration = event.getEventTime() - event.getDownTime();
                double dx = event.getX() - pressDownXY.x, dy = event.getY() - pressDownXY.y;
                int moveDistance = (int) sqrt(dx * dx + dy * dy);

                mouseUp(event.getX(), event.getY());
                invalidate();

                if (eventDuration >= LONG_PRESS_DELAY && moveDistance < LONG_PRESS_MAX_SHIFT && longPressMaxShift < LONG_PRESS_MAX_SHIFT) {
                    showPopupMenu((int) pressDownXY.x, (int) pressDownXY.y);
                }
                break;
            default:
                break;
        }

        return true;
    }

    private void mouseDown(float startX, float startY) {
        Point p;
        pressDownXY.x = startX;
        pressDownXY.y = startY;
        longPressMaxShift = 0;

        p = getBoardCoordinatesFromScreen(startX, startY);
        if (p != null) {
            Piece piece;

            // Получим фигуру, находяющуюся на данной клетке
            // Если фигура для хода уже выбрана (draggingData != null и draggingData.startSquare != null), то с доски она удалена и хранится
            // в draggingData.piece
            if (draggingData != null && draggingData.startedByFirstSquare() && draggingData.startSquare.equals(p)) {
                piece = draggingData.piece;
            } else {
                piece = chessBoard.getPiece(p);
            }

            if (piece == null) {
                // Ход еще не был начат и кликнули в пустую клетку. Если в настройках позволено выбирать сначала целевую клетку - сделаем это
                if ((draggingData == null || draggingData.startedByLastSquare()) && allowTargetSquareFist) {
                    draggingData = new DraggingData(p);
                    invalidate();
                }
            }
            else {
                if ((draggingData == null || draggingData.startedByLastSquare()) && allowTargetSquareFist) {
                    if (piece.getColor() != chessBoard.getMoveOrder() ) {
                        // если тапнули на фигуре чужого цвета, то начнем ход указанием целевой клетки (если позволено в настройках)
                        draggingData = new DraggingData(p);
                        invalidate();
                    }
                }
                // в этом методе обрабатываем только начало хода перетаскиванием.
                // Случай когда это тап на целевой клетке (тап на исходной уже сделан) будет обработан на ACTION_UP
                if (draggingData != null && draggingData.startedByFirstSquare() && piece.getColor() == draggingData.piece.getColor() && draggingData.piece != piece) {
                    // Если кликнули на своей фигуре, и при этом была уже другая выбрана для хода, то вернем ее на доску
                    chessBoard.setPieceAt(draggingData.startSquare, draggingData.piece);
                    draggingData = null;
                }

                if ((draggingData == null || draggingData.startedByFirstSquare()) && piece.getColor() == chessBoard.getMoveOrder()) {
                    draggingData = new DraggingData(piece, p, new PointF(startX, startY));
                    draggingData.dragging = true;
                    chessBoard.setPieceAt(p, null);
                    invalidate();
                }
            }
        }
    }

    private void mouseUp() {
        mouseUp(-1, -1);
    }

    private void mouseUp(float stopX, float stopY) {
        Point p = getBoardCoordinatesFromScreen(stopX, stopY);

        if (draggingData != null) {
            if (draggingData.startedByFirstSquare()) {
                // был начат ход указанием начальной клетки с фигурой
                if (p == null) {
                    // если фигуру отпустили за пределами доски, то вернем ее на место
                    chessBoard.setPieceAt(draggingData.startSquare, draggingData.piece);
                    draggingData = null;
                } else {
                    // Если клетка на которой был UP не совпадает с той, где был DOWN, то попробуем сделать ход
                    if (!p.equals(draggingData.startSquare)) {
                        draggingData.endSquare = p;
                        makeMove();
                    } else {
                        //если мы в режиме перетаскивания и оно закончилось в той же клетке, где началось - выключим режим перетаскивания,
                        // будем ждать выбора клетки назначения для фигуры
                        if (draggingData.dragging) {
                            draggingData.dragging = false;

                            // Если совпадает и текущая точка совпадает с точкой в которой был DOWN,
                            // то останемся в состоянии ожидания нажатия на клетку куда надо переместиться
                            if (!draggingData.startPoint.equals(stopX, stopY)) {
                                chessBoard.setPieceAt(draggingData.startSquare, draggingData.piece);
                            }
                        }
                        // если мы не в режиме перетаскивания, то повторный щелчок по той же фигуре отменяет ее выбор в качестве фигуры для передвижения
                        else {
                            chessBoard.setPieceAt(draggingData.startSquare, draggingData.piece);
                            draggingData = null;
                        }
                    }
                }
            }
            else {
                // был начат ход с указанием сначала конечной клетки
                if (p != null && !p.equals(draggingData.endSquare)) {
                    // проверим, есть на указанной клетке фигура того цвета, который должен ходить и может ли она пойти на конечную клетку
                    Piece piece = chessBoard.getPiece(p);

                    if (piece != null && piece.getColor() == chessBoard.getMoveOrder() && chessBoard.isMovePossible(piece, draggingData.endSquare)) {
                        draggingData.startSquare = p;
                        draggingData.piece = piece;
                        makeMove();
                    }
                    else {
                        draggingData = null;
                    }
                }
            }
        }
    }

    private void mouseMove(float pieceX, float pieceY) {
        if (draggingData != null && draggingData.dragging) {
            Point p = getBoardCoordinatesFromScreen(pieceX, pieceY);

            if (p != null) {
                double dx = pieceX - pressDownXY.x, dy = pieceY - pressDownXY.y;
                int shift = (int) sqrt(dx * dx + dy * dy);  // расстояние от точки нажатия

                draggingData.currentSquare.set(p.x, p.y);
                draggingData.currentPoint.set(pieceX, pieceY);
                if (longPressMaxShift < shift) {
                    longPressMaxShift = shift;
                }
                invalidate();
            }
        }
    }

    // отрисовка перетаскиваемой фигуры
    private void drawDragging(Canvas canvas) {
        if (draggingData != null && draggingData.piece != null) {
            Bitmap bitmap = bitmaps[draggingData.piece.getID()];
            RectF dstRect;

            if (draggingData.animating) {
                float halfPieceSize = (hLines[1] - hLines[0]) / 2;

                dstRect = new RectF(draggingData.currentPoint.x - halfPieceSize, draggingData.currentPoint.y - halfPieceSize,
                        draggingData.currentPoint.x + halfPieceSize, draggingData.currentPoint.y + halfPieceSize);
            } else if (draggingData.dragging && !draggingData.currentPoint.equals(draggingData.startPoint)) {
                int pieceSize = ((int) (hLines[1] - hLines[0])) * 3 / 2;
                dstRect = new RectF(draggingData.currentPoint.x - pieceSize / 2, draggingData.currentPoint.y - pieceSize,
                        draggingData.currentPoint.x + pieceSize / 2, draggingData.currentPoint.y);
            } else {
                int x = reverseBoard ? 7 - draggingData.currentSquare.x : draggingData.currentSquare.x;
                int y = reverseBoard ? draggingData.currentSquare.y : 7 - draggingData.currentSquare.y;

                dstRect = new RectF(vLines[x], hLines[y], vLines[x + 1], hLines[y + 1]);
            }

            canvas.drawBitmap(bitmap, null, dstRect, antialiasPaint);
        }
    }

    private void drawArrow(Canvas canvas, Paint paint, float xStart, float yStart, float xEnd, float yEnd) {
        final float ARROW_WIDTH = (hLines[1] - hLines[0]) / 3;
        final float ARROW_HEAD_WIDTH = ARROW_WIDTH * 2;
        final float ARROW_HEAD_LENGTH = ARROW_WIDTH;
        final float ARROW_LENGTH = (float) sqrt((xEnd - xStart) * (xEnd - xStart) + (yEnd - yStart) * (yEnd - yStart));
        float angle = (float) (Math.asin(Math.abs(xEnd - xStart) / ARROW_LENGTH) / Math.PI * 180);

        if (xEnd > xStart) {
            if (yEnd > yStart) {
                angle = -angle;
            }
            else {
                angle = angle - 180;
            }
        }
        else {
            if (xStart == xEnd) {
                if (yEnd < yStart) {
                    angle = 180;
                }
            }
            else
            if (yEnd < yStart) {
                angle = 180 - angle;
            }
        }

        Path arrow = new Path();
        Matrix m = new Matrix();

        arrow.moveTo(-ARROW_WIDTH / 2, 0);
        arrow.lineTo(-ARROW_WIDTH / 2, ARROW_LENGTH - ARROW_HEAD_LENGTH);
        arrow.lineTo(-ARROW_HEAD_WIDTH / 2, ARROW_LENGTH - ARROW_HEAD_LENGTH);
        arrow.lineTo(0, ARROW_LENGTH);
        arrow.lineTo(ARROW_HEAD_WIDTH / 2, ARROW_LENGTH - ARROW_HEAD_LENGTH);
        arrow.lineTo(ARROW_WIDTH / 2, ARROW_LENGTH - ARROW_HEAD_LENGTH);
        arrow.lineTo(ARROW_WIDTH / 2, 0);
        arrow.close();

        m.postRotate(angle);
        arrow.transform(m);
        arrow.offset(xStart, yStart);
        canvas.drawPath(arrow, paint);
    }

    private int indexOfArrow(String coord, int maxIndex) {
        for (int i = 0; i <= maxIndex; i++) {
            if (arrowsCoordinates[i] != null && arrowsCoordinates[i].equals(coord)) {
                return i;
            }
        }

        return -1;
    }

    private void drawArrows(Canvas canvas) {
        if (!isInEditMode()) {
            for (int arrowIndex = 0; arrowIndex < arrowsCoordinates.length; arrowIndex++) {
                if (arrowsCoordinates[arrowIndex] != null) {
                    // проверим, не нарисована ли уже стрелка с такими же координатами, но с меньшим индексом.
                    // предполагаем, что стрелка с меньшим индексом обозначаем более хороший ход
                    int sameArrowIndex = indexOfArrow(arrowsCoordinates[arrowIndex], arrowIndex - 1);

                    if (sameArrowIndex == -1) {
                        float xStart, yStart, xEnd, yEnd;
                        String fromSquare = arrowsCoordinates[arrowIndex].substring(0, 2), toSquare = arrowsCoordinates[arrowIndex].substring(2, 4);
                        Point fromPoint = getPointFromSquareName(fromSquare), toPoint = getPointFromSquareName(toSquare);

                        xStart = (vLines[fromPoint.x + 1] + vLines[fromPoint.x]) / 2;
                        yStart = (hLines[fromPoint.y + 1] + hLines[fromPoint.y]) / 2;
                        xEnd = (vLines[toPoint.x + 1] + vLines[toPoint.x]) / 2;
                        yEnd = (hLines[toPoint.y + 1] + hLines[toPoint.y]) / 2;

                        drawArrow(canvas, arrowPaints[arrowIndex], xStart, yStart, xEnd, yEnd);
                    }
                }
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // рисуем доску
        colorScheme.drawBoard(canvas);

        // .. и фигуры
        for (int x = 0; x <= 7; x++)
            for (int y = 0; y <= 7; y++) {
                Piece piece = reverseBoard ? chessBoard.getPiece(7 - x, y) : chessBoard.getPiece(x, 7 - y);

                if (piece != null) {
                    Bitmap bitmap = bitmaps[piece.getID()];
                    RectF dstRect = new RectF(vLines[x], hLines[y], vLines[x + 1], hLines[y + 1]);

                    canvas.drawBitmap(bitmap, null, dstRect, antialiasPaint);
                }
            }


        drawArrows(canvas);
        drawDragging(canvas);
    }

    private void playMoveSound(Move move) {
        if (soundEnabled) {
            int soundid;
            MediaPlayer mp;

            if (move.isOpponentChecked() || move.isOpponentCheckmated()) {
                soundid = R.raw.check;
            } else if (move.getPiece2Kind() != null) {
                soundid = R.raw.take;
            } else {
                soundid = R.raw.move;
            }
            mp = MediaPlayer.create(getContext(), soundid);
            if (mp != null) {
                mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        mp.reset();
                    }
                });
                mp.start();
            }
        }
    }

    public boolean onMove(Move move) {

        return doOnMove(move);
    }

    public void onRollback(Move move) {
        draggingData = null;

        doOnRollback(move);
    }

    public void onRollup(Move move) {
        draggingData = null;

        playMoveSound(move);
        doOnRollup(move);
    }

    public void afterMove(Move move) {
        playMoveSound(move);
    }

    public void onGoto(Move move) {
        draggingData = null;
        doOnGoto(move);
    }

    // Если доска находится в процессе анимации хода, то анимация заканчивается вне зависимости от того, на какой стадии находится
    // Происходит вызов события doOnMove
    public void finishAnimation() {
        if (animatingMove != null) {
            Piece piece = draggingData.piece;
            ChessBoard.PromoteTo promoteTo = animatingMove.getPromotePawnTo();

            animatingMove = null;
            chessBoard.setPieceAt(draggingData.startSquare, draggingData.piece);
            chessBoard.movePieceTo(draggingData.piece, draggingData.animateToSquare, promoteTo);
            draggingData = null;
            invalidate();
            doOnMove(piece.lastMove);
            setEnabled(true);
        }
    }

    class AnimationTimerTask extends TimerTask {

        @Override
        public void run() {

            if (draggingData == null || draggingData.animateIndex == MOVE_ANIMATION_TICK_COUNT) {
                animationTimer.cancel();
                animationTimer.purge();
                animationTimer = null;

                ((Activity) getContext()).runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        finishAnimation();
                    }
                });
            }
            else {
                draggingData.animateIndex++;
                draggingData.currentPoint.x = draggingData.currentPoint.x + (draggingData.animateToPoint.x - draggingData.startPoint.x) / MOVE_ANIMATION_TICK_COUNT;
                draggingData.currentPoint.y = draggingData.currentPoint.y + (draggingData.animateToPoint.y - draggingData.startPoint.y) / MOVE_ANIMATION_TICK_COUNT;
            }

            ((Activity) getContext()).runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    invalidate();
                }
            });
        }
    }

    public void animateMove(Move move) {
        int fromX = move.From().x, fromY = move.From().y, toX = move.To().x, toY = move.To().y;
        Piece piece = chessBoard.getPiece(fromX, fromY);
        int distance = (int) sqrt((fromX - toX) * (fromX - toX) + (fromY - toY) * (fromY - toY));

        if (piece != null && chessBoard.isMovePossible(piece, toX, toY)) {
            animatingMove = move;
            draggingData = new DraggingData(piece, new Point(fromX, fromY));
            chessBoard.setPieceAt(fromX, fromY, null);
            draggingData.animateTo(toX, toY);

            animationTimer = new Timer();
            animationTimer.schedule(new AnimationTimerTask(), 0, 100 * distance / MOVE_ANIMATION_TICK_COUNT);
            setEnabled(false);
        }
    }

    public void setArrowCoordinates(int arrowIndex, String coordinates) {
        if ("".equals(coordinates)) {
            coordinates = null;
        }

        if (coordinates != null) {
            if (coordinates.equals(arrowsCoordinates[arrowIndex])) {
                return;
            }
        }
        else {
            if (arrowsCoordinates[arrowIndex] == null) {
                return;
            }
        }

        arrowsCoordinates[arrowIndex] = coordinates;

        // перерисовать доску
        invalidate();
    }

    public void hideArrow(int arrowIndex) {
        setArrowCoordinates(arrowIndex, null);
    }

    public void hideArrows() {
        for (int i = 0; i < maxArrowCount; i++) {
            arrowsCoordinates[i] = null;
        }
        invalidate();
    }
}
