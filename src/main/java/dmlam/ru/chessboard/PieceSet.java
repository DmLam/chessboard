package dmlam.ru.chessboard;

/**
 * Created by Lam on 13.02.2016.
 */
public class PieceSet {
    private String name = null;
    private String prefix = null;

    public PieceSet(String name, String prefix) {
        this.name = name;
        this.prefix = prefix;
    }

    public String getName() {
        return name;
    }

    public String getPrefix() {
        return prefix;
    }
}
