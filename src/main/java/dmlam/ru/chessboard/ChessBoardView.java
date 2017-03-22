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
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

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

    private final int MOVE_ANIMATION_TICK_COUNT = 50;
    private ChessBoard chessBoard = new ChessBoard();
    private String pieceSetName = "Classic";

    private boolean showLastMoveSquares = true;
    private boolean enabled = true;
    private boolean reverseBoard = false;
    private boolean drawCoordinates = true;

    private ArrayList<IOnMoveListener> mOnMoveListeners = new ArrayList<>();
    private Paint antialiasPaint = new Paint();               // paint для гладкого масштабирования фигур
    private Paint arrowPaint = new Paint();                   // paint для рисования стрелки
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

    // признак того, что мы находимся в процессе выбора фигура для превращения пешки (показан диалог).
    // используется для правильного восстановления диалога при повороте экрана
    private boolean selectingPawnTransformation = false, selectPawnTransformationOnStart = false;
    private Point pawnTransformationSourceSquare = null;
    private Piece.Color transformingPawnColor;

    // стрелка для анализа
    private String arrowCoordinates = null;

    // класс для хранения данных о перетаскиваемой фигуре во время совершения хода перетаскиванием
    class DraggingData {
        public Piece piece;
        public Point startSquare, currentSquare;
        public PointF startPoint, currentPoint;
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
            this.currentSquare = new Point(startSquare);
            this.startPoint = startPoint;
            this.currentPoint = new PointF(startPoint.x, startPoint.y);
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

    }

    private void initialize(Context context, AttributeSet attrs, int defStyle) {

        if (!isInEditMode()) {
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
    }

    public ChessBoardView (Context context, AttributeSet attrs) {
        super(context, attrs);

        initialize(context, attrs, 0);
    }

    public ChessBoardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        initialize(context, attrs, defStyle);
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
        int w = getMeasuredWidth(), h = getMeasuredHeight(), size = Math.min(w, h);

/*
        if (widthMeasureSpec == MeasureSpec.UNSPECIFIED && heightMeasureSpec == MeasureSpec.UNSPECIFIED) {

        }
        else
*/
        if (widthMeasureSpec == MeasureSpec.UNSPECIFIED && heightMeasureSpec == MeasureSpec.EXACTLY) {
            size = h;
        }
        else
/*
        if (widthMeasureSpec == MeasureSpec.UNSPECIFIED && heightMeasureSpec == MeasureSpec.AT_MOST) {
            size = h;
        }
*/
        if (widthMeasureSpec == MeasureSpec.EXACTLY && heightMeasureSpec == MeasureSpec.UNSPECIFIED) {
            size = w;
        }
/*
        else
        if (widthMeasureSpec == MeasureSpec.EXACTLY && heightMeasureSpec == MeasureSpec.EXACTLY) {
            size = h;
        }
        else
        if (widthMeasureSpec == MeasureSpec.EXACTLY && heightMeasureSpec == MeasureSpec.AT_MOST) {
                size = h;
        }
        if (widthMeasureSpec == MeasureSpec.AT_MOST && heightMeasureSpec == MeasureSpec.UNSPECIFIED) {
            size = w;
        }
        else
        if (widthMeasureSpec == MeasureSpec.AT_MOST && heightMeasureSpec == MeasureSpec.EXACTLY) {
            size = w;
        }
        else
        if (widthMeasureSpec == MeasureSpec.AT_MOST && heightMeasureSpec == MeasureSpec.AT_MOST) {
            size = w;
        }
*/

        setMeasuredDimension(size, size);
    }

    public Bitmap getBitmap(Piece.Kind kind, Piece.Color color) {
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

        if (color == Piece.Color.BLACK) {
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
        if (board == null) {
            chessBoard.deleteOnMoveListener(this);
            chessBoard.setOnNeedPieceForTransformation(null);
        }
        else {
            board.addOnMoveListener(this);
            board.setOnNeedPieceForTransformation(this);
            invalidate();
        }
        chessBoard = board;
    }

    public void onBoardChange() {
        if (draggingData == null || !draggingData.animating)
            invalidate();
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

        arrowPaint.setColor(0xa0ffaa00);
        arrowPaint.setAntiAlias(true);
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
        chessBoard.promotePawnTo(pawnTransformationSourceSquare, result, false);
        selectingPawnTransformation = false;
        pawnTransformationSourceSquare = null;
        transformingPawnColor = null;
    }


    private void selectPromotedPawnTransformation(Point sourceSquare, Piece.Color color) {
        if (!selectingPawnTransformation) {
            SelectPawnTransformationDialogFragment selectPromotiondialog = new SelectPawnTransformationDialogFragment();
            Bundle args = new Bundle();

            selectingPawnTransformation = true;
            pawnTransformationSourceSquare = sourceSquare;  // сохраним sourceSquare чтоб не передавать его в диалог и обратно.
            transformingPawnColor = color;

            args.putInt(SelectPawnTransformationDialogFragment.SPT_IMAGESIZE, (int) (vLines[1] - vLines[0]) * 2);
            args.putParcelable(SelectPawnTransformationDialogFragment.SPT_QUEEN, bitmaps[color == Piece.Color.WHITE ? WQ : BQ]);
            args.putParcelable(SelectPawnTransformationDialogFragment.SPT_ROOK, bitmaps[color == Piece.Color.WHITE ? WR : BR]);
            args.putParcelable(SelectPawnTransformationDialogFragment.SPT_BISHOP, bitmaps[color == Piece.Color.WHITE ? WB : BB]);
            args.putParcelable(SelectPawnTransformationDialogFragment.SPT_KNIGHT, bitmaps[color == Piece.Color.WHITE ? WN : BN]);

            selectPromotiondialog.setSelectPawnTransformationDialogListener(this);
            selectPromotiondialog.setArguments(args);
            selectPromotiondialog.setCancelable(false);
            selectPromotiondialog.show(((AppCompatActivity) getContext()).getSupportFragmentManager(), "");
        }
    }

    static class SavedState extends BaseSavedState {
        boolean selectingPawnTransformation;
        Point sourceSquare;
        Piece.Color color;

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
                color = Piece.Color.valueOf(in.readString());
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


    private void makeMove(Point target) {

        if (chessBoard.isMovePossible(draggingData.piece, target.x, target.y)) {
            Piece piece = draggingData.piece;

            draggingData = null;    // необходимо сначала обнулить draggingData, т.к. в moveTo будет вызван OnMove и может быть, например,
                                    // запущена анимация хода компьютера, которая использует draggingData
            chessBoard.movePieceTo(piece, target);
        }
        else {
            chessBoard.setPieceAt(draggingData.startSquare, draggingData.piece);
            draggingData = null;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        Point p;

        if (enabled) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    p = getBoardCoordinatesFromScreen(event.getX(), event.getY());
                    if (p != null) {
                        Piece piece;
                        if (draggingData != null && draggingData.startSquare.equals(p)) {
                            piece = draggingData.piece;
                        } else {
                            piece = chessBoard.getPiece(p);
                        }

                        // здесь обрабатываем только начало хода перетаскиванием.
                        // Случай когда это тап на целевой клетке (тап на исходной уже сделан) будет обработан на ACTION_UP
                        if (draggingData != null && (piece != null && piece.getColor() == draggingData.piece.getColor() && draggingData.piece != piece)) {
                            // Если кликнули на своей фигуре, и при этом была уже другая выбрана для хода, то вернем ее на доску
                            chessBoard.setPieceAt(draggingData.startSquare, draggingData.piece);
                            draggingData = null;
                            invalidate();
                        }

                        if (piece != null && piece.getColor() == chessBoard.getMoveOrder()) {
                            draggingData = new DraggingData(piece, p, new PointF(event.getX(), event.getY()));
                            draggingData.dragging = true;
                            chessBoard.setPieceAt(p, null);
                            invalidate();
                        }
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (draggingData != null && draggingData.dragging) {
                        p = getBoardCoordinatesFromScreen(event.getX(), event.getY());
                        if (p != null) {
                            draggingData.currentSquare.set(p.x, p.y);
                            draggingData.currentPoint.set(event.getX(), event.getY());
                            invalidate();
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    if (draggingData != null) {
                        p = getBoardCoordinatesFromScreen(event.getX(), event.getY());

                        // если фигуру отпустили за пределами доски, то вернем ее на место
                        if (p == null) {
                            chessBoard.setPieceAt(draggingData.startSquare, draggingData.piece);
                            draggingData = null;
                        } else {
                            // Если клетка на которой был UP не совпадает с той, где был DOWN, то попробуем сделать ход
                            if (!p.equals(draggingData.startSquare)) {
                                makeMove(p);
                            } else {
                                //если мы в режиме перетаскивания и оно закончилось в той же клетке, где началось - выключим режим перетаскивания,
                                // будем ждать выбора клетки назначения для фигуры
                                if (draggingData.dragging) {
                                    draggingData.dragging = false;
                                    // Если совпадает и текущая точка совпадает с точкой в которой был DOWN,
                                    // то останемся в состоянии ожидания нажатия на клетку куда надо переместиться
                                    if (!draggingData.startPoint.equals(event.getX(), event.getY())) {
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
                        invalidate();
                    }
                    break;
                default:
                    break;
            }
        }
        return true;
    }

    // отрисовка перетаскиваемой фигуры
    private void drawDragging(Canvas canvas) {
        Bitmap bitmap = bitmaps[draggingData.piece.getID()];
        RectF dstRect;

        if (draggingData.animating) {
            float halfPieceSize = (hLines[1] - hLines[0]) / 2;

            dstRect = new RectF(draggingData.currentPoint.x - halfPieceSize, draggingData.currentPoint.y - halfPieceSize,
                    draggingData.currentPoint.x + halfPieceSize, draggingData.currentPoint.y + halfPieceSize);
        }
        else
        if (draggingData.dragging && !draggingData.currentPoint.equals(draggingData.startPoint)) {
            int pieceSize = ((int) (hLines[1] - hLines[0])) * 3 / 2;
            dstRect = new RectF(draggingData.currentPoint.x - pieceSize / 2, draggingData.currentPoint.y - pieceSize,
                                    draggingData.currentPoint.x + pieceSize / 2, draggingData.currentPoint.y);
        }
        else {
            int x = reverseBoard ? 7 - draggingData.currentSquare.x : draggingData.currentSquare.x;
            int y = reverseBoard ? draggingData.currentSquare.y : 7 - draggingData.currentSquare.y;
            dstRect = new RectF(vLines[x], hLines[y], vLines[x + 1], hLines[y + 1]);

        }

        canvas.drawBitmap(bitmap, null, dstRect, antialiasPaint);
    }

    private void drawArrow(Canvas canvas, float xStart, float yStart, float xEnd, float yEnd) {
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
        canvas.drawPath(arrow, arrowPaint);
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


        if (arrowCoordinates != null) {
            float xStart, yStart, xEnd, yEnd;
            String fromSquare = arrowCoordinates.substring(0, 2), toSquare = arrowCoordinates.substring(2, 4);
            Point fromPoint = getPointFromSquareName(fromSquare), toPoint = getPointFromSquareName(toSquare);

            xStart = (vLines[fromPoint.x + 1] + vLines[fromPoint.x]) / 2;
            yStart = (hLines[fromPoint.y + 1] + hLines[fromPoint.y]) / 2;
            xEnd = (vLines[toPoint.x + 1] + vLines[toPoint.x]) / 2;
            yEnd = (hLines[toPoint.y + 1] + hLines[toPoint.y]) / 2;

            drawArrow(canvas, xStart, yStart, xEnd, yEnd);
        }

        if (draggingData != null) {
            drawDragging(canvas);
        }
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
            MediaPlayer.create(getContext(), soundid).start();
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

            if (draggingData.animateIndex == MOVE_ANIMATION_TICK_COUNT) {
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

    public void setArrowCoordinates(String coordinates) {
        if ("".equals(coordinates)) {
            coordinates = null;
        }

        if (coordinates != null) {
            if (coordinates.equals(arrowCoordinates)) {
                return;
            }
        }
        else {
            if (arrowCoordinates == null) {
                return;
            }
        }

        arrowCoordinates = coordinates;

        // перерисовать доску
        invalidate();
    }

}
