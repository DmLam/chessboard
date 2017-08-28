package dmlam.ru.chessboard;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import dmlam.ru.androidcommonlib.ACRAUtils;

import static dmlam.ru.chessboard.Game.GameResult;

/**
 * Created by LamdanDY on 03.07.2015.
 */

//todo: при изменении ориентации экрана сохранять содержимое

public class ChessNotationView extends WebView implements IOnMoveListener{
    private static final String LOGTAG = ChessNotationView.class.getName();

    final public static int FONT_SIZE_SMALL = 0;
    final public static int FONT_SIZE_MIDDLE = 1;
    final public static int FONT_SIZE_BIG = 2;

    final private static String MOVE_CSS_CLASS = "move";
    final private static String LASTMOVE_CSS_CLASS = "lastmove";
    final private static String SECONDARY_MOVE_CSS_CLASS = "secmove";
    final private static String SECONDARY_LASTMOVE_CSS_CLASS = "seclastmove";
    final private static String TERTIARY_MOVE_CSS_CLASS = "termove";
    final private static String TERTIARY_LASTMOVE_CSS_CLASS = "terlastmove";
    final private static String COMMENT_CSS_CLASS = "comment";

    final private static int MAIN_LINE = 0;

    private ChessBoard chessBoard = null;
    private String notationHeadTemplate, notationHead;

    private String currentNotation = null;  // текст, находящийся в текущий момент в WebView. Нужен, чтобы сравнить с новым текстом в updateNotation и в случае совпадения не обновлять компонент
    private Move currentLastMove = null;

    // options
    private int fontSize = 0;
    private int backgroundColor;
    private int textColor;
    private int moveColor;
    private int lastMoveColor;
    private int commentColor;
    private boolean newLineOnEachVariant = true;

    private String loadNotationHead(Context context) {
        StringBuilder result = new StringBuilder();
        BufferedReader buffreader = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(R.raw.chessnotationviewhead)));
        String line;

        try {
            while (( line = buffreader.readLine()) != null) {
                result.append(line);
                result.append('\n');
            }
        } catch (IOException e) {
            return null;
        }

        return result.toString();
    }

    public void updateNotationHead() {
        notationHead = notationHeadTemplate;

        notationHead = notationHead.replace("%BACKGROUNDCOLOR%", String.format("#%06X", backgroundColor & 0xFFFFFF));
        notationHead = notationHead.replace("%TEXTCOLOR%", String.format("#%06X", textColor & 0xFFFFFF));
        notationHead = notationHead.replace("%MOVE_CSS_CLASS%", MOVE_CSS_CLASS);
        notationHead = notationHead.replace("%MOVE_CSS_CLASS%", MOVE_CSS_CLASS);
        notationHead = notationHead.replace("%LASTMOVE_CSS_CLASS%", LASTMOVE_CSS_CLASS);
        notationHead = notationHead.replace("%SECONDARY_MOVE_CSS_CLASS%", SECONDARY_MOVE_CSS_CLASS);
        notationHead = notationHead.replace("%SECONDARY_LASTMOVE_CSS_CLASS%", SECONDARY_LASTMOVE_CSS_CLASS);
        notationHead = notationHead.replace("%TERTIARY_MOVE_CSS_CLASS%", TERTIARY_MOVE_CSS_CLASS);
        notationHead = notationHead.replace("%TERTIARY_LASTMOVE_CSS_CLASS%", TERTIARY_LASTMOVE_CSS_CLASS);
        notationHead = notationHead.replace("%COMMENT_CSS_CLASS%", COMMENT_CSS_CLASS);
        notationHead = notationHead.replace("%MOVECOLOR%", String.format("#%06X", moveColor & 0xFFFFFF));
        notationHead = notationHead.replace("%LASTMOVECOLOR%", String.format("#%06X", lastMoveColor & 0xFFFFFF));
        notationHead = notationHead.replace("%COMMENTCOLOR%", String.format("#%06X", commentColor & 0xFFFFFF));

        updateNotation(true);  // force notation update to redraw it
    }

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

    private void initialize(final Context context, AttributeSet attrs, int defStyle) {

        if (!isInEditMode()) {
            notationHeadTemplate = loadNotationHead(context);

            setBackgroundColor(getResources().getColor(R.color.chessnotationview_background));
            setTextColor(getResources().getColor(R.color.chessnotationview_text));
            setMoveColor(getResources().getColor(R.color.chessnotationview_move));
            setLastMoveColor(getResources().getColor(R.color.chessnotationview_lastmove));
            setCommentColor(getResources().getColor(R.color.chessnotationview_comment));

            WebSettings settings = getSettings();
            // настройки для более быстрой отрисовки
            settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
            settings.setAppCacheEnabled(false);
            settings.setLoadsImagesAutomatically(false);
            settings.setGeolocationEnabled(false);
            settings.setNeedInitialFocus(false);
            settings.setSupportZoom(false);
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            setTextSize(settings);
            configureLayerType();
//            setWebChromeClient(new WebChromeClient());  // нужно только для отладки JS - чтобы работал alert
            // установим обработчик ссылок для просмотра записи партии

            setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    if (url.substring(0, 5).equals("move:")) {
                        NotationMoveSelected(url.substring(5, url.length()));
                    }
                    return true;
                }

                // необходимо перекрывать onPageStarted если перекрывается onPageFinished. Иначе будут глюки с отрисовкой последнего хода
                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon)  {
                    super.onPageStarted(view, url, favicon);
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);

                    if  (chessBoard != null) {
                        showLastMove(chessBoard.getLastMove());
                    }
                }
            });
        }
    }

    public void clear() {
        loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
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
            currentLastMove = null;
            updateNotation();
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

    public void setBackgroundColor(int color) {
        super.setBackgroundColor(color);
        backgroundColor = color;

        if (!isInEditMode()) {
            updateNotationHead();
        }
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setTextColor(int color) {
        textColor = color;
        updateNotationHead();
    }

    public int getTextColor() {
        return textColor;
    }

    public void setMoveColor(int color) {
        moveColor = color;
        updateNotationHead();
    }

    public int getMoveColor() {
        return moveColor ;
    }

    public void setLastMoveColor(int color) {
        lastMoveColor = color;
        updateNotationHead();
    }

    public int getLastMoveColor() {
        return lastMoveColor;
    }

    public void setCommentColor(int color) {
        commentColor = color;
        updateNotationHead();
    }

    public int getCommentColor() {
        return commentColor;
    }

    public void setNewLineOnEachVariant(boolean newLineOnEachVariant) {
        this.newLineOnEachVariant = newLineOnEachVariant;
    }

    public boolean getNewLineOnEachVariant() {
        return newLineOnEachVariant;
    }

    private void hideLastMove(Move lastMove) {
        if (lastMove != null) {
            loadUrl("javascript: hideLastMove(" + Integer.toString(lastMove.getMoveId()) + ")");
        }
    }

    private void showLastMove(Move lastMove) {
        currentLastMove = lastMove;

        if (currentLastMove != null) {
            loadUrl("javascript: showLastMove(" + Integer.toString(lastMove.getMoveId()) + ")");
        }
    }

    private void scrollToLastMove() {
        Move lastMove = chessBoard.getLastMove();

        if (lastMove != null) {
            loadUrl("javascript:scrollAnchor(" + Integer.toString(lastMove.getMoveId()) + ");");
        }
    }

    private void NotationMoveSelected(String url) {
        chessBoard.deleteOnMoveListener(this);
        try {
            int id = Integer.valueOf(url);

            hideLastMove(currentLastMove);
            chessBoard.gotoMove(id);
            showLastMove(chessBoard.getLastMove());
        }
        finally {
            chessBoard.addOnMoveListener(this);
        }
    }

    private String annotationString(int annotationGlyph) {
        switch (annotationGlyph) {
            case 1:
                return "!";
            case 2:
                return "?";
            case 3:
                return "&#x203C";
            case 4:
                return "&#x2047";
            case 5:
                return "&#x2049";
            case 6:
                return "&#x2048";
            case 7:
                return "&#x25A1";  // white square - forced move
            case 10:
                return "=";
            case 13:
                return "&infin;";  // ∞
            case 14:
                return "&#x2A72";  // ⩲
            case 15:
                return "&#x2A71";  // ⩱
            case 16:
                return "&plusmn";  // ±
            case 17:
                return "&#x2213";  // ∓
            case 18:
                return "+-";
            case 19:
                return "-+";
            default:
                return "";
        }
    }

    private String gameResultString(GameResult result) {
        if (result == null) {
            return "";
        }

        switch(result) {
            case WHITE:
                return "&nbsp;1-0";
            case DRAW:
                return"&nbsp;1/2-1/2";
            case BLACK:
                return "&nbsp;0-1";
            default:
                return "";
        }
    }

    private String commentString(String comment) {
        if (comment != null) {
            return String.format("<span class=\"%s\"> %s</span>", COMMENT_CSS_CLASS, comment);
        }
        else
            return "";
    }

    private StringBuilder moveNotation(Move move, int level) {
        StringBuilder result = new StringBuilder();
        String htmlElementClass;

        htmlElementClass = level == MAIN_LINE ? MOVE_CSS_CLASS : (level == 1 ? SECONDARY_MOVE_CSS_CLASS : TERTIARY_MOVE_CSS_CLASS);

        result.append(String.format("<a id=\"%d\" class=\"%s\" href=\"move:%d\">", move.getMoveId(), htmlElementClass, move.getMoveId())).
                append(move.getNotation()).
                append(annotationString(move.getNumericAnnotationGlyph())).
                append("</a>").
                append(commentString(move.getComment())).
                // после хода черных добавим еще один пробел для визуальной приятности
                append(move.getMoveOrder() == Piece.Color.WHITE ? " " : "");

        return result;
    }

    private StringBuilder variantsNotation(MoveList variants, int level) {
        StringBuilder result = new StringBuilder();;

        if (level == MAIN_LINE) {
            if (variants.getComment() != null) {
                result.append("<span class=\"").append(COMMENT_CSS_CLASS).append("\">&nbsp;")
                        .append(' ')
                        .append(variants.getComment())
                        .append("</span>&nbsp;");
            }

            result.append(branchNotation(variants.get(0), level));
        }

        if (variants.size() > 1) {
            if (level == 1) {
                if (newLineOnEachVariant) {
                    result.append("<br>");
                }
                else {
                    result.append("&nbsp;");
                }
                result.append(String.format("<div class = '%s'>[&nbsp;", SECONDARY_MOVE_CSS_CLASS));
            }
            else
            if (level > 1) {
                result.append(String.format("&nbsp;<span class = '%s'>(&nbsp;", TERTIARY_MOVE_CSS_CLASS));
            }

            if (variants.getComment() != null) {
                result.append("<span class=\"").append(COMMENT_CSS_CLASS).append("\">&nbsp;")
                        .append(' ')
                        .append(variants.getComment())
                        .append("</span>&nbsp;");
            }
            for (int i = 1; i < variants.size(); i++) {
                result.append(branchNotation(variants.get(i), level));
                if (i < variants.size() - 1) {
                    if (level == 1) {
                        result.append("&nbsp;]");
                        if (newLineOnEachVariant) {
                            result.append("<br>");
                        }
                        else {
                            result.append("&nbsp;");
                        }
                        result.append("[&nbsp;");
                    }
                    else if (level > 1) {
                        result.append(';');
                    }
                }
            }

            if (level == 1) {
                result.append("&nbsp;]</div>");
            }
            else
            if (level > 1) {
                result.append("&nbsp;)&nbsp;</span>");
            }
        }

        if (level == MAIN_LINE && chessBoard.game.getResult() != GameResult.UNKNOWN) {
            result.append("<br>").append(gameResultString(chessBoard.game.getResult()));
        }

        return result;
    }

    // todo Функция должна принимать в качестве параметра не Move, а MoveList чтобы можно было отображать ветки первого хода
    private StringBuilder branchNotation(Move move, int level) {
        StringBuilder result = new StringBuilder();
        Move prevMove;
        boolean FirstPass = true, wasBranch = false;

        // Выведем номер хода. Если после данного хода наступила очередь белых, то этот ход был черных. Добавим многоточие вместо хода белых.
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
                            .append('.')
                            .append("&nbsp");  // не переводим строку после номера хода
                }
            }
            result.append(moveNotation(move, level));

            prevMove = move.getPrevMove();
            move = move.getMainVariant();
            wasBranch = false;
            if (!FirstPass || level == MAIN_LINE) {
                if (prevMove != null && prevMove.getVariantCount() > 1) {

                    result.append(variantsNotation(prevMove.getVariants(), level + 1));
                    wasBranch = true;
                }
            }
            FirstPass = false;

        } while (move != null);

        return result;
    }

    public void updateNotation() {
        updateNotation(false);
    }

    public void updateNotation(boolean forceUpdate) {
        String newNotation;

        if (chessBoard == null || chessBoard.getFirstMoveVariants() == null || chessBoard.getFirstMoveVariants().size() == 0) {
            newNotation = "";
        }
        else {
            newNotation = variantsNotation(chessBoard.getFirstMoveVariants(), MAIN_LINE).toString();
        }

        hideLastMove(currentLastMove);
        if (!newNotation.equals(currentNotation) || forceUpdate) {
            StringBuilder notation = new StringBuilder();

            notation.append("<html>").
                    append(notationHead).
                    append("<body>").
                    append(newNotation).
                    append("</body>").
                    append("</html>");

            ACRAUtils.putCustomData("Notation", newNotation);
            loadDataWithBaseURL(null, notation.toString(), "text/html", "utf-8", null);
            currentNotation = newNotation;
        }
        else {
            if (chessBoard != null) {
                showLastMove(chessBoard.getLastMove());
            }
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
