package dmlam.ru.chessboard;

import android.content.Context;
import android.content.res.Resources;

import java.util.ArrayList;

/**
 * Created by LamdanDY on 02.02.2016.
 */
public class ColorSchemeList {
    ChessBoardView chessBoardView;
    private ArrayList<ColorSetColorScheme> list = new ArrayList<ColorSetColorScheme>();

    public ColorSchemeList (ChessBoardView chessBoardView) {
        this.chessBoardView = chessBoardView;
    }

    public ColorSchemeList (ChessBoardView chessBoardView, String[] schemes){
        this.chessBoardView = chessBoardView;

        for (int i = 0; i < schemes.length; i++ ) {
            addFromResource(schemes[i], chessBoardView);
        }
    }

    public void add(ColorSetColorScheme scheme) {
        list.add(scheme);
    }

    public String addFromResource(String resourceNamePrefix, ChessBoardView chessBoardView) {
        Context context = chessBoardView.getContext();
        Resources r = context.getResources();
        String name = r.getString(r.getIdentifier(resourceNamePrefix + "_name", "string", context.getPackageName()));

        add(new ColorSetColorScheme(name, chessBoardView,
                r.getColor(r.getIdentifier(resourceNamePrefix + "_white_square_start", "color", context.getPackageName())),
                r.getColor(r.getIdentifier(resourceNamePrefix + "_white_square_finish", "color", context.getPackageName())),
                r.getColor(r.getIdentifier(resourceNamePrefix + "_black_square_start", "color", context.getPackageName())),
                r.getColor(r.getIdentifier(resourceNamePrefix + "_black_square_finish", "color", context.getPackageName())),
                r.getColor(r.getIdentifier(resourceNamePrefix + "_current_move_source_square", "color", context.getPackageName())),
                r.getColor(r.getIdentifier(resourceNamePrefix + "_current_move_target_square", "color", context.getPackageName())),
                r.getColor(r.getIdentifier(resourceNamePrefix + "_last_move_source_square", "color", context.getPackageName())),
                r.getColor(r.getIdentifier(resourceNamePrefix + "_last_move_target_square", "color", context.getPackageName())),
                r.getColor(r.getIdentifier(resourceNamePrefix + "_border", "color", context.getPackageName())),
                r.getColor(r.getIdentifier(resourceNamePrefix + "_coordinates", "color", context.getPackageName()))
        ));

        return name;
    }

    public ColorSetColorScheme find(String name) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getName().equals(name)) {
                return list.get(i);
            }
        }
        return null;
    }

    public int size() { return list.size(); }

    public ColorSetColorScheme get(int i) { return list.get(i); }

    public String[] getNames() {
        String[] result = new String[size()];

        for (int i = 0; i < size(); i++) {
            result[i] = get(i).getName();
        }

        return result;
    }
}
