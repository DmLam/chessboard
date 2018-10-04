package dmlam.ru.chessboard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.util.ArrayList;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static dmlam.ru.chessboard.BoardControlView.ButtonPosition.mostLeft;
import static dmlam.ru.chessboard.BoardControlView.ButtonPosition.mostRight;
import static dmlam.ru.chessboard.BoardControlView.ButtonPosition.toRightOf;
import static dmlam.ru.chessboard.ScreenBackground.SCREEN_BACKGROUND;
import static dmlam.ru.chessboard.ScreenBackground.SCREEN_BACKGROUND.WHITE;

public class BoardControlView extends RelativeLayout implements IOnMoveListener, View.OnClickListener {

    public enum Analysis {NONE, ANALYSIS, CANCEL_ANALYSIS}
    public enum CONTROLBUTTON {ROLLBACK, ROLLUP, ANALYSIS, CANCEL_ANALYSIS}
    public enum ButtonPosition {mostLeft, toLeftOf, toRightOf, mostRight}

    private ChessBoard chessBoard;
    private int bRollback, bRollup, bAnalysis, bCancelAnalysis;
    private ArrayList<Button> buttons = new ArrayList<>();
    private ArrayList<ButtonGroup> buttonGroups = new ArrayList<>();

    private int size = 0;
    private SCREEN_BACKGROUND background = WHITE;

    private class ButtonGroup {
        private int[] buttons;
        protected boolean enabled;

        ButtonGroup(int[] buttons) {
            this.buttons = buttons;
            enabled = true;
        }

        boolean buttonInGroup(int button) {
            for (int buttonIdx: buttons) {
                if (button == buttonIdx) {
                    return true;
                }
            }

            return false;
        }
    }

    private class Button {
        private ImageButton button;
        private int frameId = -1;
        boolean enabled = true;
        boolean visible = true; // для сохранения состояния при смене конфигурации
        private int mipmapResourceId;
        private int frameColor = Color.TRANSPARENT;
        private BoardControlView.ButtonPosition buttonPosition;
        private int anchorButtonIndex = -1;

        Button(ImageButton button, int frameId, int mipmapResourceId) {
            this.button = button;
            this.frameId = frameId;
            this.mipmapResourceId = mipmapResourceId;
        }

        Button(ImageButton button, int frameId, int mipmapResourceId, ButtonPosition buttonPosition, int anchorButtonIndex) {
            this(button, frameId, mipmapResourceId);
            this.buttonPosition = buttonPosition;
            this.anchorButtonIndex = anchorButtonIndex;
        }

        protected Button(Parcel in) {
            frameId = in.readInt();
            enabled = in.readByte() != 0;
            visible = in.readByte() != 0;
            mipmapResourceId = in.readInt();
            frameColor = in.readInt();
            anchorButtonIndex = in.readInt();
        }
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        for (Button button: buttons) {
            button.button.setMaxHeight(size);
        }
    }

    private void initialize(Context context) {
        size = (int) getResources().getDimension(R.dimen.control_button_size);

        bRollback = addButton(R.mipmap.rollback, mostLeft);
        bRollup = addButton(R.mipmap.rollup, toRightOf, bRollback);
        bCancelAnalysis = addButton(R.mipmap.cancel_analysis, mostRight);
        bAnalysis = addButton(R.mipmap.analysis, mostRight);

        setButtonEnabled(bRollback, false);
        setButtonEnabled(bRollup, false);
        setButtonVisibility(bAnalysis, false);
        setButtonVisibility(bCancelAnalysis, false);
    }

    public BoardControlView(Context context) {
        super(context);

        initialize(context);
    }

    public BoardControlView(Context context, AttributeSet attrs) {
        super(context, attrs);

        initialize(context);
    }

    public BoardControlView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        initialize(context);
    }

    public void setChessBoard(ChessBoard chessBoard) {
        if (chessBoard != null) {
            chessBoard.deleteOnMoveListener(this);
            setButtonEnabled(bRollback, false);
            setButtonEnabled(bRollup, false);
        }

        this.chessBoard = chessBoard;

        if (chessBoard != null) {
            chessBoard.addOnMoveListener(this);
            setButtonEnabled(bRollback, true);
            setButtonEnabled(bRollup, true);
            setButtonEnabled(bRollback, chessBoard.rollbackEnabled());
            setButtonEnabled(bRollup, chessBoard.rollupEnabled());
        }
    }

    @Override
    public boolean onMove(Move move) {
        return true;
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
    public void onBoardChange() {
        setButtonEnabled(bRollback, chessBoard.rollbackEnabled());
        setButtonEnabled(bRollup, chessBoard.rollupEnabled());
    }

    @Override
    public void afterMove(Move move) {

    }

    @Override
    public void onClick(View v) {
        if (chessBoard != null) {
            if (v == buttons.get(bRollback).button) {
                chessBoard.rollback();
            }
            else if (v == buttons.get(bRollup).button) {
                chessBoard.rollup();
            }
        }
    }

    // используем т.к. до API 23 не было getRule
    private int getRelativeLayoutParamsRule(RelativeLayout.LayoutParams params, int rule) {
        return params.getRules()[rule];
    }

    // используем т.к. до API 17 не было removeRule
    private void removeRelativeLayoutParamsRule(RelativeLayout.LayoutParams params, int rule) {
        params.addRule(rule, 0);
    }

    private enum ButtonSide {LEFT, RIGHT}

    // добавляет кнопку в начале или в конце
    private void addStartingButton(RelativeLayout.LayoutParams frameLayoutParams, ButtonSide buttonSide, int anchorButtonIndex) {
        int alignment = buttonSide == BoardControlView.ButtonSide.LEFT ? RelativeLayout.ALIGN_PARENT_LEFT : RelativeLayout.ALIGN_PARENT_RIGHT;
        int anchorButtonFrameId = ((View) buttons.get(anchorButtonIndex).button.getParent()).getId();

        // найдем все кнопки, которые уже привязаны к указанной стороне и привяжем их слева (справа) к новой кнопке
        for( int i = 0; i < buttons.size(); i++) {
            View buttonFrame = (View) buttons.get(i).button.getParent();
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) buttonFrame.getLayoutParams();
            if (params != null) {  // params == null для только что созданной кнопки
                int rule = getRelativeLayoutParamsRule(params, alignment);

                if (rule != 0) {
                    removeRelativeLayoutParamsRule(params, alignment);
                    params.addRule(buttonSide == BoardControlView.ButtonSide.LEFT ? RelativeLayout.RIGHT_OF : RelativeLayout.LEFT_OF, anchorButtonFrameId);
                    buttonFrame.setLayoutParams(params);
                }
            }
        }
        frameLayoutParams.addRule(alignment);
    }

    // добавляет кнопку справа или слева от указанной
    private void addSideButtonTo(RelativeLayout.LayoutParams frameLayoutParams, int oldAnchorButtonIndex, int newAnchorButtonIndex, ButtonSide buttonSide) {
        int alignment = buttonSide == BoardControlView.ButtonSide.LEFT ? RelativeLayout.LEFT_OF : RelativeLayout.RIGHT_OF;
        int oldAnchorButtonFrameId = ((View) buttons.get(oldAnchorButtonIndex).button.getParent()).getId();
        int newAnchorButtonFrameId = ((View) buttons.get(newAnchorButtonIndex).button.getParent()).getId();

        // найдем все кнопки, которые уже привязаны слева (справа) к указаной и  и привяжем их слева (справа) к новой кнопке
        for( int i = 0; i < buttons.size(); i++) {
            View buttonFrame = (View) buttons.get(i).button.getParent();
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) buttonFrame.getLayoutParams();
            if (params != null) {  // params == null для только что созданной кнопки
                int rule = getRelativeLayoutParamsRule(params, alignment);

                if (rule == oldAnchorButtonFrameId) {
                    removeRelativeLayoutParamsRule(params, alignment);
                    params.addRule(alignment, newAnchorButtonFrameId);
                    buttonFrame.setLayoutParams(params);
                }
            }
        }

        frameLayoutParams.addRule(alignment, oldAnchorButtonFrameId);
    }

    public int addButton(int mipmapResourceId, ButtonPosition buttonPosition, int anchorButtonIndex) {
        return addButton(-1, mipmapResourceId, buttonPosition, anchorButtonIndex);
    }

    // buttonIndex - индекс кнопки в массиве button. При смене конфигурации массив buttons содержит описания кнопок, а соответствующие view уже удалены
    // поэтому нужно пересоздать только View - для этого указывает индекс уже существующих элементов в списке buttons. Иначе buttonIndex должен быть -1
    private int addButton(int buttonIndex, int mipmapResourceId, ButtonPosition buttonPosition, int anchorButtonIndex) {
        int result = buttonIndex == -1 ? buttons.size() : buttonIndex;
        LinearLayout frame = new LinearLayout(getContext());
        ImageButton imageButton = new ImageButton(getContext());
        LinearLayout.LayoutParams buttonLayoutParams = new LinearLayout.LayoutParams(getSize(), getSize());
        RelativeLayout.LayoutParams frameLayoutParams = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);

        imageButton.setMinimumHeight(35);
        imageButton.setBackgroundColor(Color.TRANSPARENT);
        frame.setId(result + 1);
        frame.addView(imageButton, buttonLayoutParams);
        if (buttonIndex == -1) {
            buttons.add(new Button(imageButton, frame.getId(), mipmapResourceId, buttonPosition, anchorButtonIndex));
        }
        else {
            Button button = buttons.get(result);

            button.button = imageButton;
        }

        switch (buttonPosition) {
            case mostLeft:
                addStartingButton(frameLayoutParams, ButtonSide.LEFT, result);
                break;
            case toLeftOf:
                addSideButtonTo(frameLayoutParams, anchorButtonIndex, result, ButtonSide.LEFT);
                break;
            case toRightOf:
                addSideButtonTo(frameLayoutParams, anchorButtonIndex, result, ButtonSide.RIGHT);
                break;
            case mostRight:
                addStartingButton(frameLayoutParams, ButtonSide.RIGHT, result);
                break;
            default:
                assert(false);
        }
        addView(frame, frameLayoutParams);

        DrawButton(result);

        imageButton.setOnClickListener(this);

        return result;
    }

    public int addButton(int mipmapResourceId, ButtonPosition buttonPosition) {
        if (buttonPosition == mostLeft || buttonPosition == mostRight) {
            return addButton(mipmapResourceId, buttonPosition, -1);
        }
        else {
            throw new RuntimeException("Anchor button index is absent");
        }
    }

    public void setButtonClickListener(int buttonIndex,View.OnClickListener onClickListener) {
        Button button = buttons.get(buttonIndex);

        button.button.setOnClickListener(onClickListener);
    }

    public int registerButtonGroup(int... buttons) {
        int[] array = new int[buttons.length];
        int index = buttonGroups.size();

        for (int i = 0; i < buttons.length; i++) {
            array[i] = buttons[i];
        }
        buttonGroups.add(new ButtonGroup(array));

        return index;
    }

    public void setButtonGroupEnabled(int groupId, boolean enabled) {
        if (buttonGroups.get(groupId).enabled != enabled) {
            buttonGroups.get(groupId).enabled = enabled;

            for (int buttonIdx: buttonGroups.get(groupId).buttons) {
                DrawButton(buttonIdx);
            }
        }
    }

    public int getButton(CONTROLBUTTON ControlButton) {
        switch (ControlButton) {
            case ROLLBACK:
                return bRollback;
            case ROLLUP:
                return bRollup;
            case ANALYSIS:
                return bAnalysis;
            case CANCEL_ANALYSIS:
                return bCancelAnalysis;
        }

        return -1;
    }

    public void setButtonImage(int buttonIndex, int mipmapResourceId) {
        Button button = buttons.get(buttonIndex);

        if (button != null && button.mipmapResourceId != mipmapResourceId) {
            button.mipmapResourceId = mipmapResourceId;

            DrawButton(buttonIndex);
        }
    }

    public void setButtonFrame(int buttonIndex, int frameColor) {
        Button button = buttons.get(buttonIndex);

        if (button != null && button.frameColor != frameColor) {
            button.frameColor = frameColor;

            DrawButton(buttonIndex);
        }
    }

    public void clearButtonFrame(int buttonIndex) {
        setButtonFrame(buttonIndex, Color.TRANSPARENT);
    }

    public void setBackground(SCREEN_BACKGROUND background) {
        this.background = background;

        for (int i = 0; i < buttons.size(); i++) {
            DrawButton(i);
        }
    }

    public void setButtonVisibility(int buttonIndex, boolean visible) {
        Button button = buttons.get(buttonIndex);

        button.visible = visible;
        button.button.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public boolean isButtonVisible(int buttonIndex) {
        return buttons.get(buttonIndex).visible;
    }

    public void setButtonEnabled(int buttonIndex, boolean enabled) {
        Button button = buttons.get(buttonIndex);

        if (button.enabled != enabled) {
            button.enabled = enabled;
            button.button.setEnabled(enabled);
            DrawButton(buttonIndex);
        }
    }

    public boolean isButtonSelected(int buttonIndex) {
        return buttons.get(buttonIndex).button.isSelected();
    }

    public void setButtonSelected(int buttonIndex, boolean selected) {
        Button button = buttons.get(buttonIndex);

        if (button.button.isSelected() != selected) {
            button.button.setSelected(selected);
        }
    }

    public void setAnalysisButtonVisibility(BoardControlView.Analysis button) {
        switch (button) {
            case NONE:
                setButtonVisibility(getButton(CONTROLBUTTON.ANALYSIS), false);
                setButtonVisibility(getButton(CONTROLBUTTON.CANCEL_ANALYSIS), false);
                break;
            case ANALYSIS:
                setButtonVisibility(getButton(CONTROLBUTTON.ANALYSIS), true);
                setButtonVisibility(getButton(CONTROLBUTTON.CANCEL_ANALYSIS), false);
                break;
            case CANCEL_ANALYSIS:
                setButtonVisibility(getButton(CONTROLBUTTON.ANALYSIS), false);
                setButtonVisibility(getButton(CONTROLBUTTON.CANCEL_ANALYSIS), true);
                break;
            default:
                assert(false);
        }
    }

    private boolean buttonGroupsEnabled(int button) {
        boolean result = true;

        for (int i = 0; i < buttonGroups.size(); i++) {
            if (buttonGroups.get(i).buttonInGroup(button)) {
                result &= buttonGroups.get(i).enabled;
            }
        }

        return result;
    }

    private void DrawButton(int buttonIndex) {
        BoardControlView.Button button = buttons.get(buttonIndex);

        setImageButtonDrawable(button.enabled && buttonGroupsEnabled(buttonIndex), button.button, button.mipmapResourceId, button.frameColor);
    }

    public void setEnabled(boolean enabled) {
        for (int i = 0; i < buttonGroups.size(); i++) {
            setButtonGroupEnabled(i, enabled);
        }
    }

    /**
     * Sets the specified image button to the given state, while modifying or
     * "graying-out" the icon as well
     *
     * @param enabled The state of the menu item
     * @param item The menu item to modify
     * @param iconResId The icon ID
     *
     * (C) Oleg Vaskevich http://stackoverflow.com/questions/8196206/disable-an-imagebutton
     */
    private void setImageButtonDrawable(boolean enabled, ImageButton item, int iconResId, int frameColor) {
        boolean imageEnabled = background == WHITE ? enabled : !enabled;

        item.setEnabled(enabled);

        // используем ContextCompat т.к. getDrawable(int) deprecated в API >= 21
        Drawable originalIcon = ContextCompat.getDrawable(getContext(), iconResId);

        Drawable icon = imageEnabled ? originalIcon : convertDrawableToGrayScale(originalIcon);

        // при необходимости отобразим признак наличия истории на кнопке информации о задаче
        if (frameColor != Color.TRANSPARENT) {
            Bitmap bitmap = Bitmap.createBitmap(icon.getIntrinsicWidth(), icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);

            Canvas canvas = new Canvas(bitmap);
            icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            icon.draw(canvas);

            Paint paint = new Paint();
            paint.setColor(frameColor);
            paint.setStrokeWidth(2);
            paint.setStyle(Paint.Style.STROKE);

            canvas.drawRoundRect(new RectF(1, 1, bitmap.getWidth() - 1, bitmap.getHeight() - 1), 3, 3, paint);

            item.setImageBitmap(bitmap);
        }
        else {
            item.setImageDrawable(icon);
        }
    }

    /**
     * Mutates and applies a filter that converts the given drawable to a Gray
     * image. This method may be used to simulate the color of disable icons in
     * Honeycomb's ActionBar.
     *
     * @return a mutated version of the given drawable with a color filter
     *         applied.
     */
    public static Drawable convertDrawableToGrayScale(Drawable drawable) {
        if (drawable == null) {
            return null;
        }
        Drawable r = drawable.mutate();

        r.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
        return r;
    }

}
