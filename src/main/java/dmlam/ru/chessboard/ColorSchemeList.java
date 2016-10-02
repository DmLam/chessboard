package dmlam.ru.chessboard;

import android.content.Context;
import android.content.res.Resources;

import java.util.ArrayList;

/**
 * Created by LamdanDY on 02.02.2016.
 */
public class ColorSchemeList {
    Context context;
    private ArrayList<ColorScheme> list = new ArrayList<ColorScheme>();

    public ColorSchemeList(Context context) {
        this.context = context;
    }

    public ColorSchemeList (Context context, String[] schemes){
        this.context = context;

        for (int i = 0; i < schemes.length; i++ ) {
            addFromResource(schemes[i]);
        }
    }

    public void add(ColorScheme scheme) {
        list.add(scheme);
    }

    public String addFromResource(String resourceNamePrefix) {
        Resources r = context.getResources();
        String name = r.getString(r.getIdentifier(resourceNamePrefix + "_name", "string", context.getPackageName()));

        add(new ColorScheme(name,
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

    public ColorScheme find(String name) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getName().equals(name)) {
                return list.get(i);
            }
        }
        return null;
    }

    public int size() { return list.size(); }

    public ColorScheme get(int i) { return list.get(i); }

    public String[] getNames() {
        String[] result = new String[size()];

        for (int i = 0; i < size(); i++) {
            result[i] = get(i).getName();
        }

        return result;
    }
}
