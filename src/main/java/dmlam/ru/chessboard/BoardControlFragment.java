package dmlam.ru.chessboard;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.core.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.util.ArrayList;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static dmlam.ru.chessboard.ScreenBackground.SCREEN_BACKGROUND.WHITE;

/**
 * Created by Lam on 28.07.2015.
 */
public class BoardControlFragment extends Fragment {
    public enum Analysis {NONE, ANALYSIS, CANCEL_ANALYSIS}

    public enum CONTROLBUTTON {ROLLBACK, ROLLUP, ANALYSIS, CANCEL_ANALYSIS}
    public enum ButtonPosition {mostLeft, toLeftOf, toRightOf, mostRight}

    private FragmentActivity activity = null;
    private RelativeLayout rlBoardControl = null;
    private ImageButton ibRollback, ibRollup, ibAnalysis, ibCancelAnalysis;

    private ArrayList<Button> buttons = new ArrayList<>();
    private int bRollback, bRollup, bAnalysis, bCancelAnalysis;
    private ArrayList<ButtonGroup> buttonGroups = new ArrayList<>();

    private int size = 0;
    private ScreenBackground.SCREEN_BACKGROUND background = WHITE;

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
        private ButtonPosition buttonPosition;
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
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        for (Button button: buttons) {
            button.button.setMaxHeight(size);
        }

        this.size = size;
    }

    private int registerButton(ImageButton button, int mipmapResourceId) {
        int index = buttons.size();
        Button newButton = new Button(button,((View) button.getParent()).getId(), mipmapResourceId);
        buttons.add(newButton);
        newButton.visible = button.getVisibility() == View.VISIBLE;

        return index;
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
        int alignment = buttonSide == ButtonSide.LEFT ? RelativeLayout.ALIGN_PARENT_LEFT : RelativeLayout.ALIGN_PARENT_RIGHT;
        int anchorButtonFrameId = ((View) buttons.get(anchorButtonIndex).button.getParent()).getId();

        // найдем все кнопки, которые уже привязаны к указанной стороне и привяжем их слева (справа) к новой кнопке
        for( int i = 0; i < buttons.size(); i++) {
            View buttonFrame = (View) buttons.get(i).button.getParent();
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) buttonFrame.getLayoutParams();
            if (params != null) {  // params == null для только что созданной кнопки
                int rule = getRelativeLayoutParamsRule(params, alignment);

                if (rule != 0) {
                    removeRelativeLayoutParamsRule(params, alignment);
                    params.addRule(buttonSide == ButtonSide.LEFT ? RelativeLayout.RIGHT_OF : RelativeLayout.LEFT_OF, anchorButtonFrameId);
                    buttonFrame.setLayoutParams(params);
                }
            }
        }
        frameLayoutParams.addRule(alignment);
    }

    // добавляет кнопку справа или слева от указанной
    private void addSideButtonTo(RelativeLayout.LayoutParams frameLayoutParams, int oldAnchorButtonIndex, int newAnchorButtonIndex, ButtonSide buttonSide) {
        int alignment = buttonSide == ButtonSide.LEFT ? RelativeLayout.LEFT_OF : RelativeLayout.RIGHT_OF;
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
        LinearLayout frame = new LinearLayout(activity);
        ImageButton imageButton = new ImageButton(activity);
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
        rlBoardControl.addView(frame, frameLayoutParams);

        DrawButton(result);

        return result;
    }

    public int addButton(int mipmapResourceId, ButtonPosition buttonPosition) {
        return addButton(mipmapResourceId, buttonPosition, -1);
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

    public int getButton(CONTROLBUTTON CONTROLBUTTON) {
        switch (CONTROLBUTTON) {
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

    @Override
    public void onAttach (Activity activity) {
        super.onAttach(activity);

        this.activity = (FragmentActivity) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        activity = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setEnabled(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.boardcontrol_fragment, container, false);

        size = (int) getResources().getDimension(R.dimen.board_control_view_size);

        rlBoardControl = (RelativeLayout) rootView.findViewById(R.id.rlBoardControl);
        ibRollback = (ImageButton) rootView.findViewById(R.id.ibRollback);
        ibRollup = (ImageButton) rootView.findViewById(R.id.ibRollup);
        ibAnalysis = (ImageButton) rootView.findViewById(R.id.ibAnalysis);
        ibCancelAnalysis = (ImageButton) rootView.findViewById(R.id.ibCancelAnalysis);

        if (savedInstanceState == null) {
            bRollback = registerButton(ibRollback, R.mipmap.rollback);                              // 0
            bRollup = registerButton(ibRollup, R.mipmap.rollup);                                    // 1
            bAnalysis = registerButton(ibAnalysis, R.mipmap.analysis);                              // 2
            bCancelAnalysis = registerButton(ibCancelAnalysis, R.mipmap.cancel_analysis);           // 3
        }
        else {
            // savedInstanceState != null - fragment is being restored after changing configuration. Buttons already registered
            for (int i = 0; i < buttons.size(); i++) {
                Button button = buttons.get(i);
                ViewGroup frame = (ViewGroup) rlBoardControl.findViewById(button.frameId);
                if (frame != null) {
                    // Button is already exists with its frame - standard button created in the layout
                    button.button = (ImageButton) frame.getChildAt(0);

                }
                else {
                    // button and its frame doesn't exists - dynamically created button, need to recreate it using info in buttons
                    addButton(i, button.mipmapResourceId, button.buttonPosition, button.anchorButtonIndex);
                }

                setButtonVisibility(i, button.visible);
            }
        }

        return rootView;
    }

    public void setBackground(ScreenBackground.SCREEN_BACKGROUND background) {
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

    public void setAnalysisButtonVisibility(Analysis button) {
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
        Button button = buttons.get(buttonIndex);

        if (activity != null) {
            setImageButtonDrawable(button.enabled && buttonGroupsEnabled(buttonIndex), button.button, button.mipmapResourceId, button.frameColor);
        }
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
        Drawable originalIcon = ContextCompat.getDrawable(activity, iconResId);

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
