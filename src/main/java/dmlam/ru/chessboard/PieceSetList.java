package dmlam.ru.chessboard;

import java.util.ArrayList;

/**
 * Created by Lam on 13.02.2016.
 */
public class PieceSetList {
    private ArrayList<PieceSet> list = new ArrayList<PieceSet>();

    public int size() { return list.size(); }

    public void add(String name, String prefix) {
        list.add(new PieceSet(name, prefix));
    }

    // конструктор, загружающий из ресурсов список PieceSet-ов
    public PieceSetList(String[] sets) {
        for (int i = 0; i < sets.length; i++) {
            int n = sets[i].indexOf('=');
            String prefix = sets[i].substring(0, n);
            String name = sets[i].substring(n + 1);

            add(name, prefix);
        }
    }

    public PieceSet get(int i) {
        return list.get(i);
    }

    public String[] getNames() {
        String[] result = new String[size()];

        for (int i = 0; i < size(); i++) {
            result[i] = get(i).getName();
        }

        return result;
    }

    public String[] getValues() {
        String[] result = new String[size()];

        for (int i = 0; i < size(); i++) {
            result[i] = get(i).getPrefix();
        }

        return result;
    }

    public PieceSet find(String name) {
        for (int i = 0; i < size(); i++) {
            PieceSet ps = get(i);
            if (ps.getName().equals(name))
                return ps;
        }

        return null;
    }

    public String[] getPrefixes() {
        String[] result = new String[size()];

        for (int i = 0; i < size(); i++) {
            result[i] = get(i).getPrefix();
        }

        return result;
    }
}
