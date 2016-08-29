package dmlam.ru.chessboard;

import java.util.ArrayList;

/**
 * Created by LamdanDY on 29.08.2016.
 */
public class Game {
    private static final String LOGTAG = Game.class.getName();

    private class Tag {
        protected String name, value;

        public Tag(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    private ArrayList<Tag> tags = new ArrayList<Tag>();
    private MoveList moves = new MoveList();

    public void addTag(String name, String value) {
        if (tagByName(name) != null) {
            throw new RuntimeException(String.format("Tag %s already exists", name));
        }
        tags.add(new Tag(name, value));
    }

    public String tagByName(String name) {
        for (Tag tag: tags) {
            if (tag.name.equals(name)) {
                return tag.value;
            }
        }

        return null;
    }


}
