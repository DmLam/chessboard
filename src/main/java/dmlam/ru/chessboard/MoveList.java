package dmlam.ru.chessboard;

import java.util.ArrayList;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Created by LamdanDY on 29.08.2016.
 */
public class MoveList extends ArrayList<Move> {
    private static final String LOGTAG = MoveList.class.getName();

    private String comment = null; // comment on the variant. May be placed before the first move of the variant

    public Move locateMove(int id) {
        for (int i = 0; i < size(); i++) {
            Move move = get(i);
            if (move.getMoveId() == id) {
                return move;
            }
        }

        return null;
    }

    public Move findMove(int id) {
        int i = 0;
        Move result = locateMove(id);

        while(result == null && i < size())

        {
            result = get(i).getVariants().findMove(id);
            i++;
        }

        return result;
    }

    public int locateMove(Move move) {
        for (int i = 0; i < size(); i++) {
            Move m = get(i);
            if (move.Equals(m)) {
                return i;
            }
        }

        return -1;
    }

    public int maxId() {
        int result = 0;

        for (int i = 0; i < size(); i++) {
            int curMax = get(i).getMoveId();

            if (get(i).getVariants() != null) {
                curMax = max(curMax, get(i).getVariants().maxId());
            }
            result = max(result, curMax);
        }

        return result;
    }

    public int minId() {
        int result = Integer.MAX_VALUE;

        for (int i = 0; i < size(); i++) {
            int curMin = get(i).getMoveId();

            if (get(i).getVariants() != null) {
                curMin = min(curMin, get(i).getVariants().minId());
            }
            result = min(result, curMin);
        }

        return result;
    }

    public void shiftIds(int difference) {
        for (int i = 0; i < size(); i++) {
            get(i).shiftMoveId(difference);
            get(i).getVariants().shiftIds(difference);
        }
    }

    public void removeLine(Move move) {
        removeLine(indexOf(move));
    }

    public Move removeLine(int index) {
        Move result = remove(index);
        result.prevVariants = null;

        return result;
    }

    public void addLine(Move line) {
        if (size() > 0) {
            line.prevVariants = get(0).prevVariants;
        }
        else {
            line.prevVariants = null;
        }
        for (int i = 0; i < line.getVariants().size(); i++) {
            line.getVariants(i).prevVariants = this;
        }
        add(line);
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getComment() {
        return comment;
    }

    @Override
    public void clear() {
        super.clear();

        comment = null;
    }
}
