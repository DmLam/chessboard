package dmlam.ru.chessboard;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;

/**
 * Created by Lam on 15.06.2015.
 */
public class SelectPawnTransformationDialogFragment extends DialogFragment implements View.OnClickListener {
    static final String SPT_IMAGESIZE = "imagesize";
    static final String SPT_QUEEN = "queen";
    static final String SPT_ROOK = "rook";
    static final String SPT_BISHOP = "bishop";
    static final String SPT_KNIGHT = "knight";

    Point sourceSquare = null;
    Activity activity;
    private SelectPawnTransformationDialogListener mListener;
    private int imageSize;
    private ImageView queenIV, rookIV, bishopIV, knightIV;
    private Bitmap queen, rook, bishop, knight;

    public interface SelectPawnTransformationDialogListener {
        void onSelectPawnTransformation(ChessBoard.PromoteTo result);
    }

    private ImageView createImage(Bitmap bitmap) {
        ImageView result = new ImageView(activity);

        result.setImageBitmap(bitmap);
        result.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        result.setAdjustViewBounds(true);
        result.setScaleType(ImageView.ScaleType.FIT_XY);
        result.setMaxHeight(imageSize);
        result.setMaxWidth(imageSize);
        result.setOnClickListener(this);

        return result;
    }

    public void setSelectPawnTransformationDialogListener(SelectPawnTransformationDialogListener listener) {
        mListener = listener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        // request a window without the title
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
//
//      На время отладки только!
//
//       setCancelable(false);
//
//

        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LinearLayout layout;

        queenIV = createImage(queen);
        rookIV = createImage(rook);
        bishopIV = createImage(bishop);
        knightIV = createImage(knight);
        layout = new LinearLayout(activity);

        layout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        layout.setGravity(Gravity.CENTER);
        layout.addView(queenIV);
        layout.addView(rookIV);
        layout.addView(bishopIV);
        layout.addView(knightIV);

        return layout;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        this.imageSize = args.getInt(SPT_IMAGESIZE);
        this.queen = args.getParcelable(SPT_QUEEN);
        this.rook = args.getParcelable(SPT_ROOK);
        this.bishop = args.getParcelable(SPT_BISHOP);
        this.knight = args.getParcelable(SPT_KNIGHT);

        setRetainInstance(true);
    }

    public void onClick(View v) {
        ChessBoard.PromoteTo selected = ChessBoard.PromoteTo.QUEEN;

        if (v == rookIV) {selected = ChessBoard.PromoteTo.ROOK;}
        else
        if (v == bishopIV) {selected = ChessBoard.PromoteTo.BISHOP;}
        else
        if (v == knightIV) {selected = ChessBoard.PromoteTo.KNIGHT;}

        if (mListener != null)
        {
            mListener.onSelectPawnTransformation(selected);
        }

        dismiss();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        this.activity = activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        activity = null;
        mListener = null;
    }

}
