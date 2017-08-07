package dmlam.ru.chessboard;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import dmlam.ru.androidcommonlib.ACRAUtils;
import dmlam.ru.androidcommonlib.StringUtils;

import static dmlam.ru.chessboard.Game.GameResult.UNKNOWN;

/**
 * Created by LamdanDY on 03.07.2015.
 */

//todo: при изменении ориентации экрана сохранять содержимое

public class ChessNotationView extends WebView implements IOnMoveListener{
    private static final String LOGTAG = ChessNotationView.class.getName();

    final public static int FONT_SIZE_SMALL = 0;
    final public static int FONT_SIZE_MIDDLE = 1;
    final public static int FONT_SIZE_BIG = 2;

    final private static boolean MAIN_LINE = true;
    final private static boolean SECONDARY_LINE = false;

    final private static String MOVE_CSS_CLASS = "move";
    final private static String LASTMOVE_CSS_CLASS = "lastmove";
    final private static String SECONDARY_MOVE_CSS_CLASS = "secmove";
    final private static String SECONDARY_LASTMOVE_CSS_CLASS = "seclastmove";
    final private static String COMMENT_CSS_CLASS = "comment";

    private ChessBoard chessBoard = null;
    private String moveColor;
    private String lastMoveColor;
    private String commentColor;
    private String notationHead;

    private String currentNotation = null;  // текст, находящийся в текущий момент в WebView. Нужен, чтобы сравнить с новым текстом в updateNotation и в случае совпадения не обновлять компонент
    private Move currentLastMove = null;

    // options
    private int fontSize = 0;
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

        StringUtils.replaceAll(result, "%MOVE_CSS_CLASS%", MOVE_CSS_CLASS);
        StringUtils.replaceAll(result, "%LASTMOVE_CSS_CLASS%", LASTMOVE_CSS_CLASS);
        StringUtils.replaceAll(result, "%SECONDARY_MOVE_CSS_CLASS%", SECONDARY_MOVE_CSS_CLASS);
        StringUtils.replaceAll(result, "%SECONDARY_LASTMOVE_CSS_CLASS%", SECONDARY_LASTMOVE_CSS_CLASS);
        StringUtils.replaceAll(result, "%COMMENT_CSS_CLASS%", COMMENT_CSS_CLASS);
        StringUtils.replaceAll(result, "%MOVECOLOR%", moveColor);
        StringUtils.replaceAll(result, "%LASTMOVECOLOR%", lastMoveColor);
        StringUtils.replaceAll(result, "%COMMENTCOLOR%", commentColor);

        return result.toString();
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

    private void initialize(Context context, AttributeSet attrs, int defStyle) {

        if (!isInEditMode()) {
            moveColor = String.format("#%06X", 0xFFFFFF & getResources().getColor(R.color.chessnotationview_move));
            lastMoveColor = String.format("#%06X", 0xFFFFFF & getResources().getColor(R.color.chessnotationview_lastmove));
            commentColor = String.format("#%06X", 0xFFFFFF & getResources().getColor(R.color.chessnotationview_comment));

            notationHead = loadNotationHead(context);

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
            setWebChromeClient(new WebChromeClient());  // нужно только для отладки JS - чтобы работал alert
            // установим обработчик ссылок для просмотра записи партии

            setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    if (url.substring(0, 5).equals("move:")) {
                        NotationMoveSelected(url.substring(5, url.length()));
                    }
                    return true;
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);

                    scrollToLastMove();
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
            loadUrl(String.format("javascript: showLastMove(%d, %d)", currentLastMove.getMoveId(), getHeight()));
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
            hideLastMove(currentLastMove);

            chessBoard.gotoMove(Integer.valueOf(url));

            showLastMove(chessBoard.getLastMove());
        }
        finally {
            chessBoard.addOnMoveListener(this);
        }
    }

    private StringBuilder moveNotation(Move move, boolean mainLine) {
        StringBuilder result = new StringBuilder();
        String htmlElementClass;

        htmlElementClass = mainLine ? MOVE_CSS_CLASS : SECONDARY_MOVE_CSS_CLASS;

        result.append(String.format("<a id=\"%d\" class=\"%s\" href=\"move:%d\">", move.getMoveId(), htmlElementClass, move.getMoveId())).
                append(move.getNotation());


        switch (move.getNumericAnnotationGlyph()) {
            case 1:
                result.append('!');
                break;
            case 2:
                result.append('?');
                break;
            case 3:
                result.append("&#x203C");
                break;
            case 4:
                result.append("&#x2047");
                break;
            case 5:
                result.append("&#x2049");
                break;
            case 6:
                result.append("&#x2048");
                break;
            case 7:
                result.append("&#x25A1");  // white square - forced move
                break;
            case 10:
                result.append("=");
                break;
            case 13:
                result.append("&infin;");  // ∞
                break;
            case 14:
                result.append("&#x2A72");  // ⩲
                break;
            case 15:
                result.append("&#x2A71");  // ⩱
                break;
            case 16:
                result.append("&plusmn");  // ±
                break;
            case 17:
                result.append("&#x2213");  // ∓
                break;
            case 18:
                result.append("+-");
                break;
            case 19:
                result.append("-+");
                break;
            default:
                break;
        }

        if (move.getGameResult() != UNKNOWN) {
            switch(move.getGameResult()) {
                case WHITE:
                    result.append("&nbsp;1-0");
                    break;
                case DRAW:
                    result.append("&nbsp;1/2-1/2");
                    break;
                case BLACK:
                    result.append("&nbsp;0-1");
                    break;
            }
        }

        result.append("</a>");
        if (move.getComment() != null) {
            result.append("<span class=\"").append(COMMENT_CSS_CLASS).append("\">")
                    .append(' ')
                    .append(move.getComment())
                    .append("</span>");
        }
        if (move.getMoveOrder() == Piece.Color.WHITE) {
            // после хода черных добавим еще один пробел для визуальной приятности
            result.append(" ");
        }

        return result;
    }

    private StringBuilder variantsNotation(MoveList variants, boolean mainLine) {
        StringBuilder result = new StringBuilder();;

        if (mainLine) {
            if (variants.getComment() != null) {
                result.append("<span class=\"").append(COMMENT_CSS_CLASS).append("\">&nbsp;")
                        .append(' ')
                        .append(variants.getComment())
                        .append("</span>&nbsp;");
            }

            result.append(branchNotation(variants.get(0), mainLine));
        }

        if (variants.size() > 1) {
            result.append("<br>&nbsp;&nbsp;(&nbsp;");
            if (variants.getComment() != null) {
                result.append("<span class=\"").append(COMMENT_CSS_CLASS).append("\">&nbsp;")
                        .append(' ')
                        .append(variants.getComment())
                        .append("</span>&nbsp;");
            }
            for (int i = 1; i < variants.size(); i++) {
                result.append(branchNotation(variants.get(i), SECONDARY_LINE));
                if (i < variants.size() - 1) {
                    result.append(";<br>");
                }
            }
            result.append(")");
        }

        return result;
    }

    // todo Функция должна принимать в качестве параметра не Move, а MoveList чтобы можно было отображать ветки первого хода
    private StringBuilder branchNotation(Move move, boolean mainLine) {
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
                            .append(".")
                            .append("&nbsp");  // не переводим строку после номера хода
                }
            }
            result.append(moveNotation(move, mainLine));

            prevMove = move.getPrevMove();
            move = move.getMainVariant();
            wasBranch = false;
            if (!FirstPass || mainLine) {
                if (prevMove != null && prevMove.getVariantCount() > 1) {

                    result.append(variantsNotation(prevMove.getVariants(), SECONDARY_LINE))
                          .append("<br>");
                    wasBranch = true;
                }
            }
            FirstPass = false;

        } while (move != null);

        return result;
    }

    public void updateNotation() {
        StringBuilder notation = new StringBuilder();
        String newNotation;

        if (chessBoard.getFirstMoveVariants().size() > 0) {

            newNotation = variantsNotation(chessBoard.getFirstMoveVariants(), MAIN_LINE).toString();

            notation.append("<html>").
                    append(notationHead).
                    append("<body>").
                    append(newNotation).
                    append("</body>").
                    append("</html>");

            hideLastMove(currentLastMove);
            if (!newNotation.equals(currentNotation)) {
                String sNotation = notation.toString();

                ACRAUtils.putCustomData("Notation", sNotation);
                loadDataWithBaseURL(null, sNotation, "text/html", "utf-8", null);
                currentNotation = newNotation;
            }
            // выделяем в нотации последний сделанный ход. Не делаем это сразу при формировании html, т.к. тогда нельзя будет сравнивать сформированный html с предыдущим вариантом
            // (там могли быть те же самые ходы, но последний другой)
            showLastMove(chessBoard.getLastMove());
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
