package dmlam.ru.chessboard;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.ArrayList;

/**
 * Created by LamdanDY on 03.07.2015.
 */

//todo: при изменении ориентации экрана сохранять содержимое

public class ChessNotationView extends WebView implements IOnMoveListener{
    private static final String LOGTAG = ChessNotationView.class.getName();

    private ChessBoard chessBoard = null;
    private String textColor = String.format("#%06X", 0xFFFFFF & getResources().getColor(R.color.chessnotationview_text));
    private String lastMoveColor = String.format("#%06X", 0xFFFFFF & getResources().getColor(R.color.chessnotationview_lastmove));
    private String commentColor = String.format("#%06X", 0xFFFFFF & getResources().getColor(R.color.chessnotationview_comment));
//    private ArrayList<String> branches = new ArrayList<String>();
    private Move lastMove;
    private int fontSize = 0;

    final public static int FONT_SIZE_SMALL = 0;
    final public static int FONT_SIZE_MIDDLE = 1;
    final public static int FONT_SIZE_BIG = 2;

    // используем функцию для установки размера шрифта в WebView, т.к. на API до 14 для этого была функция setTextSize, которая ныне deprecated
    // сейчас же используется setTextZoom, которой нет в API ранее 14-го
    // Чтобы в зависимости от API использовать подходящую функцию и не иметь warning-ов - сделана эта функция
    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    protected void setTextSize(WebSettings settings){
        WebSettings.TextSize size;

        switch (fontSize) {
            case FONT_SIZE_SMALL:
                size = WebSettings.TextSize.SMALLEST;
                break;
            case FONT_SIZE_MIDDLE:
                size = WebSettings.TextSize.SMALLER;
                break;
            case FONT_SIZE_BIG:
                size = WebSettings.TextSize.NORMAL;
                break;
            default:
                size = WebSettings.TextSize.SMALLEST;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            if (size == WebSettings.TextSize.SMALLEST) {
                settings.setTextZoom(60);
            }
            else if (size == WebSettings.TextSize.SMALLER) {
                settings.setTextZoom(80);
            }
            else if (size == WebSettings.TextSize.NORMAL) {
                settings.setTextZoom(100);
            }
        } else {
            settings.setTextSize(size);
        }
    }

    @TargetApi(11)
    protected void configureLayerType() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
        }
    }

    private void initialize(Context context, AttributeSet attrs, int defStyle) {

        if (!isInEditMode()) {
            textColor = String.format("#%06X", 0xFFFFFF & getResources().getColor(R.color.chessnotationview_text));
            lastMoveColor = String.format("#%06X", 0xFFFFFF & getResources().getColor(R.color.chessnotationview_lastmove));
            WebSettings settings = getSettings();

            // настройки для более быстрой отрисовки
            settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
            settings.setAppCacheEnabled(false);
            settings.setLoadsImagesAutomatically(false);
            settings.setGeolocationEnabled(false);
            settings.setNeedInitialFocus(false);
            settings.setSupportZoom(false);
            setTextSize(settings);
            configureLayerType();
            // установим обработчик ссылок для просмотра записи партии
            setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    if (url.substring(0, 5).equals("move:")) {
                        NotationMoveSelected(url.substring(5, url.length()));
                    }
                    return true;
                }
            });
        }
    }

    public ChessNotationView(Context context) {
        super(context);
    }

    public ChessNotationView(Context context, AttributeSet attrs) {
        super(context, attrs);

        initialize(context, attrs, 0);
    }

    public ChessBoard getChessBoard() {
        return chessBoard;
    }

    public void setChessBoard(ChessBoard board) {
        if (board == null) {
            chessBoard.deleteOnMoveListener(this);
            chessBoard = null;
        }
        else {
            chessBoard = board;
            board.addOnMoveListener(this);
            requestLayout();
            updateNotation();
        }
    }

    public void setFontSize(int size) {
        fontSize = size;
        setTextSize(getSettings());

        invalidate();
    }

    public int getFontSize() {
        return fontSize;
    }

    private void NotationMoveSelected(String url) {
        chessBoard.gotoMove(Integer.valueOf(url));
    }

    private StringBuilder moveNotation(Move move) {
        StringBuilder result = new StringBuilder();

        result.append("<a href=\"move:")
                .append(move.getMoveId())
                .append("\">");

        if (move == lastMove) {
            result.append("<span style=\"color:").append(lastMoveColor).append("\">");
        }
        result.append(move.getNotation());
        if (move == lastMove) {
            result.append("</span>");
        }
        if (move.getComment() != null) {
            result.append("</span class=\"comment\">")
                    .append(' ')
                    .append(move.getComment())
                    .append("</span>");
        }
        result.append("</a>");
        if (move.getMoveOrder() == Piece.Color.WHITE) {
            // после хода черных добавим еще один пробел для визуальной приятности
            result.append(" ");
        }

        return result;
    }


    // todo Функция должна принимать в качестве параметра не Move, а MoveList чтобы можно было отображать ветки первого хода
    private StringBuilder branchNotation(Move move, boolean mainLine) {
        StringBuilder result = new StringBuilder();
        Move prevMove;
        boolean FirstPass = true, wasBranch = false;

        // Выведем номер хода.
        // Если после данного хода наступила очередь белых, то этот ход был черных. Добавим многоточие вместо хода белых.
        if (move.getMoveOrder() == Piece.Color.WHITE) {
            // в Move сохраняется состояние всех параметров доски ПОСЛЕ хода, но это не касается номера хода. Он сохраняется по состоянию ПЕРЕД ходом
            result.append(move.getMoveNumber())
                    .append('.')
                    .append("&nbsp;&hellip;");
        }
        do {
            if (wasBranch) {
                if (move != null) {
                    result.append(move.getMoveNumber())
                            .append(".&nbsp;");
                    if (move.getMoveOrder() == Piece.Color.WHITE) {
                        result.append("&hellip;");
                    }
                }
            }
            else {
                if (move.getMoveOrder() == Piece.Color.WHITE) {
                    result.append(" ");  // между ходами белых и черных переводить можно переводить строку
                } else {
                    result.append(move.getMoveNumber())
                            .append(".")
                            .append("&nbsp");  // не переводим строку после номера хода
                }
            }
            result.append(moveNotation(move));

            prevMove = move.getPrevMove();
            move = move.getMainVariant();
            wasBranch = false;
            if (!FirstPass || mainLine) {
                if (prevMove != null && prevMove.getVariantCount() > 1) {
                    ArrayList<String> branches = new ArrayList<String>();

                    for (int i = 1; i < prevMove.getVariantCount(); i++) {
                        if (prevMove.getVariants(i) != null) {
                            String variant = branchNotation(prevMove.getVariants(i), false).
                                    insert(0, "<br>&nbsp;&nbsp;(").append(')')
                                    .toString();

                            branches.add(variant);
                        }
                    }
                    for (int i = 0; i < branches.size(); i++) {
                        result.append("<span class=\"").append("secbranch").append("\">")
                                .append(branches.get(i))
                                .append("</span>");

                    }

//                    result.append("<br>");

                    wasBranch = true;
                }
            }
            FirstPass = false;

        } while (move != null);

        return result;
    }

    // todo разобраться почему приходим сюда два раза после каждого хода
    public void updateNotation() {
        Move move;
        StringBuilder notation = new StringBuilder(), m = new StringBuilder(), sb = new StringBuilder();

        lastMove = chessBoard.getLastMove();

        if (chessBoard.getFirstMoveVariants().size() > 0) {
            move = chessBoard.getFirstMoveVariants().get(0);

            notation.append("<html>").
                    append("<head>").
                    append("<style type=\"text/css\">").
                    append(String.format("body{color;color:%s;font-weight:bold;}", textColor)).
                    append(String.format("a:link{text-decoration:none;color:%s;}", textColor)).
                    append(String.format("a:visited{text-decoration:none;color:%s;}", textColor)).
                    append(String.format(".lastmove a:link{color:%s;}", lastMoveColor)).
                    append(String.format(".lastmove a:visited{color:%s;}", lastMoveColor)).
                    append(String.format(".secbranch {color:%s;font-weight:normal}", textColor)).
                    append(String.format(".secbranch a:visited{color:%s;font-weight:normal}", textColor)).
                    append(String.format(".secbranch a:link{color:%s;font-weight:normal;}", textColor)).
                    append(String.format(".comment {color:%s;font-weight:normal;font-style:italic}", commentColor)).
                    append("</style>").
                    append("</head>").
                    append("<body>").
                    append(branchNotation(move, true)).
                            append("</body>").
                    append("</html>");


            String html = notation.toString();
            loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
        }
    }

    @Override
    public boolean onMove(Move move) {
        return true;
    }

    @Override
    public void afterMove(Move move) {
    }

    @Override
    public void onRollback(Move move) {
    }

    @Override
    public void onRollup(Move move) {
    }

    @Override
    public void onGoto(Move move) {
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!isInEditMode()) {
            super.onDraw(canvas);
        }
        else {
            Paint paint = new Paint();

            paint.setColor(Color.BLUE);
            paint.setTextSize(getHeight() / 5);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("e2-e4", getWidth() / 2, getHeight() / 2, paint);
        }
    }

    public void onBoardChange () {
        updateNotation();
    }

}
